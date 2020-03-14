/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.dynamic;

import ccait.ccweb.entites.*;
import ccait.ccweb.utils.OSUtils;
import entity.query.ColumnInfo;
import entity.query.core.ApplicationConfig;
import entity.tool.util.StringUtils;
import javapoet.JavaFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileFilter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ccait.ccweb.dynamic.MemoryJavaFileManager.getJavaFile;
import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;

public class DynamicClassBuilder {

    private static final Logger log = LogManager.getLogger( DynamicClassBuilder.class );

    private static final String DEFAULT_PACKAGE = "ccait.ccweb.entites";

    public static Object create(String tablename, List<ColumnInfo> columns) {
        return create(tablename, columns, true);
    }

    public static Object create(String tablename, List<ColumnInfo> columns, boolean isQueryable) {

        tablename = tablename.trim();
        String suffix = UUID.randomUUID().toString().replace("-", "");
        JavaFile javaFile = getJavaFile(columns, tablename, "id", "public", suffix, isQueryable);

        try {
            String className = String.format("%s%s", tablename.substring(0, 1).toUpperCase() + tablename.substring(1), suffix);
            String packagePath = ApplicationConfig.getInstance().get("entity.package", DEFAULT_PACKAGE);
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

            if(compiler == null) {
                log.error("compiler has been null!!!");
            }
            StandardJavaFileManager stdManager = compiler.getStandardFileManager(null, null, null);
            try (MemoryJavaFileManager manager = new MemoryJavaFileManager(stdManager)) {
                JavaFileObject javaFileObject = manager.makeStringSource(String.format("%s.java", className), javaFile.toString());

                List<String> options = null;
                options = null;
                String targetDir = Thread.currentThread().getContextClassLoader().getResource("").getPath()
                        .replaceAll("/[^/]+\\.jar!/BOOT-INF/classes!/", "")
                        .replace("file:", "");

                log.info("-----user.dir-----: " + targetDir);
                String classpath = getJarFiles(targetDir + "/libs");
                log.info("-----classpath-----: " + classpath);

                if(StringUtils.isNotEmpty(classpath)) {
                    options = Arrays.asList("-encoding", "UTF-8", "-classpath", classpath, "-d", targetDir, "-sourcepath", targetDir);
                }

                JavaCompiler.CompilationTask task = compiler.getTask(null, manager, null, options, null, Arrays.asList(javaFileObject));

                if (task.call()) {
                    Map<String, byte[]> results = manager.getClassBytes();
                    try (MemoryClassLoader classLoader = new MemoryClassLoader(results)) {
                        Class<?> clazz = classLoader.loadClass(String.format("%s.%s", packagePath, className));
                        Object bean = clazz.newInstance();

                        classLoader.close();
                        manager.close();

                        return bean;
                    }
                }
            }
            catch (Exception e) {
                log.error(LOG_PRE_SUFFIX + e.getMessage(), e);
            }
        }
        catch (Exception e){
            log.error(LOG_PRE_SUFFIX + "动态生成Entity错误！！！", e);
        }

        return null;
    }

    public static List<ColumnInfo> getColumnInfosBySelectList(List<SelectInfo> selectInfos) {
        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();

        if(selectInfos != null) {
            for (int i=0; i<selectInfos.size(); i++) {
                SelectInfo item = selectInfos.get(i);
                if ("id".equals(ensureColumnName(item.getField()).toLowerCase())) {
                    continue;
                }
                ColumnInfo col = new ColumnInfo();
                col.setColumnName(ensureColumnName(item.getField()));
                col.setAlias(item.getAlias());
                if(StringUtils.isEmpty(col.getAlias())) {
                    col.setAlias(ensureColumnName(item.getAlias()));
                }
                columns.add(col);
            }
        }

        //去重
        columns = columns.stream().collect(
                Collectors.collectingAndThen(Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(o -> o.getColumnName()))), ArrayList::new));

        return columns;
    }

    public static Object create(String tablename, Map<String, Object> data) {
        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
        if(data != null) {
            for(Map.Entry<String, Object> item : data.entrySet()) {

                ColumnInfo col = new ColumnInfo();
                col.setColumnName(item.getKey());
                if(item.getValue() != null) {
                    col.setDefaultValue(item.getValue().toString());
                    col.setDataType(item.getValue().getClass().getTypeName());
                }
                if("id".equals(item.getKey().toLowerCase())) {
                    col.setPrimaryKey(true);
                }

                columns.add(col);
            }
        }

        //去重
        columns = columns.stream().collect(
                Collectors.collectingAndThen(Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(o -> o.getColumnName()))), ArrayList::new));

        return create(tablename, columns);
    }

    public static String ensureColumnName(String name) {

        if(name == null) {
            return null;
        }

        Matcher matcher = Pattern.compile("\\w[\\w\\d]*\\.(\\w[\\w\\d]*)").matcher(name);
        if(matcher.matches()) {
            name = matcher.group(1);
            return name;
        }

        return name;
    }

    public static Object create(String tablename, QueryInfo queryInfo) {
        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
        if(queryInfo != null) {
            if(queryInfo.getConditionList() != null) {
                for (ConditionInfo item : queryInfo.getConditionList()) {
                    ColumnInfo col = new ColumnInfo();
                    col.setColumnName(ensureColumnName(item.getName()));
                    if ("id".equals(ensureColumnName(item.getName()).toLowerCase())) {
                        if(item.getValue() != null) {
                            col.setDefaultValue("0");
                            col.setDataType(item.getValue().getClass().getTypeName());
                        }
                    }

                    else {
                        if(item.getValue() != null) {
                            col.setDefaultValue(item.getValue().toString());
                            col.setDataType(item.getValue().getClass().getTypeName());
                        }
                    }

                    columns.add(col);
                }
            }

            if(queryInfo.getSortList() != null) {
                for (SortInfo item : queryInfo.getSortList()) {
                    ColumnInfo col = new ColumnInfo();
                    col.setColumnName(ensureColumnName(item.getName()));

                    if ("id".equals(ensureColumnName(item.getName().toLowerCase()))) {
                        col.setDataType("int");
                    }

                    else {
                        col.setDataType("object");
                    }

                    columns.add(col);
                }
            }

            if(queryInfo.getKeywords() != null) {
                for (FieldInfo item : queryInfo.getKeywords()) {
                    ColumnInfo col = new ColumnInfo();
                    col.setColumnName(ensureColumnName(item.getName()));

                    if ("id".equals(ensureColumnName(item.getName()).toLowerCase())) {
                        if(item.getValue() != null) {
                            col.setDefaultValue("0");
                        }
                        col.setDataType("int");
                    }
                    else {
                        if(item.getValue() != null) {
                            col.setDefaultValue(item.getValue().toString());
                        }
                        col.setDataType("string");
                    }

                    columns.add(col);
                }
            }

            if(queryInfo.getGroupList() != null) {
                for (String name : queryInfo.getGroupList()) {
                    ColumnInfo col = new ColumnInfo();
                    col.setColumnName(ensureColumnName(name));
                    if ("id".equals(ensureColumnName(name).toLowerCase())) {
                        col.setDataType("int");
                    }
                    else {
                        col.setDataType("object");
                    }

                    columns.add(col);
                }
            }
        }

        //去重
        columns = columns.stream().collect(
                Collectors.collectingAndThen(Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(o -> o.getColumnName()))), ArrayList::new));

        return create(tablename, columns);
    }

    /**
     * 查找该目录下的所有的jar文件
     *
     * @param jarPath
     * @throws Exception
     */
    public static String getJarFiles(String jarPath) throws Exception {
        File sourceFile = new File(jarPath);
        final String[] jars = {""};
        if (sourceFile.exists()) {// 文件或者目录必须存在
            if (sourceFile.isDirectory()) {// 若file对象为目录
                // 得到该目录下以.jar 结尾的文件或者目录
                File[] childrenFiles = sourceFile.listFiles(new FileFilter() {
                    public boolean accept(File file) {
                        if (file.isDirectory()) {
                            return true;
                        } else {
                            String name = file.getName();
                            if (name.endsWith(".jar")) {
                                file.setReadable(true);
                                log.info("-----jar: " + file.getPath());
                                String spliter = ":";
                                if(OSUtils.isWindows()) {
                                    spliter = ";";
                                }
                                jars[0] = jars[0] + file.getPath() + spliter;
                                return true;
                            }
                            return false;
                        }
                    }
                });
            }
        }
        return jars[0];
    }

    public static String smallHump(String columnName) {
        List<String> list = StringUtils.splitString2List(columnName, "_");
        if(list.size() < 1) {
            return columnName;
        }

        if(list.size() == 1) {
            return list.get(0);
        }

        list.set(0, list.get(0).substring(0,1).toLowerCase() + list.get(0).substring(1));
        for(int i=1; i<list.size(); i++) {
            if(StringUtils.isEmpty(list.get(i))) {
                continue;
            }
            list.set(i, list.get(i).substring(0,1).toUpperCase() + list.get(i).substring(1));
        }

        String result = StringUtils.join("", list);

        return result;
    }
}
