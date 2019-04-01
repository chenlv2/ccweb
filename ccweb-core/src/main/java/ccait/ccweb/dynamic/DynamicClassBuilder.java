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

import ccait.ccweb.model.ConditionInfo;
import ccait.ccweb.model.FieldInfo;
import ccait.ccweb.model.QueryInfo;
import ccait.ccweb.model.SortInfo;
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
import java.util.stream.Collectors;

import static ccait.ccweb.dynamic.MemoryJavaFileManager.getJavaFile;
import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;

public class DynamicClassBuilder {

    private static final Logger log = LogManager.getLogger( DynamicClassBuilder.class );

    private static final String DEFAULT_PACKAGE = "ccait.ccweb.entites";

    public static Object create(String tablename, List<ColumnInfo> columns) {

        String suffix = UUID.randomUUID().toString().replace("-", "");
        JavaFile javaFile = getJavaFile(columns, tablename, "id", "public", suffix);

        try {
            String className = String.format("%s%s", tablename.substring(0, 1).toUpperCase() + tablename.substring(1), suffix);

            String packagePath = ApplicationConfig.getInstance().get("entity.package", DEFAULT_PACKAGE);
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

            StandardJavaFileManager stdManager = compiler.getStandardFileManager(null, null, null);
            try (MemoryJavaFileManager manager = new MemoryJavaFileManager(stdManager)) {
                JavaFileObject javaFileObject = manager.makeStringSource(String.format("%s.java", className), javaFile.toString());

                List<String> options = null;

                /**
                 * 编译选项，在编译java文件时，编译程序会自动的去寻找java文件引用的其他的java源文件或者class。 -sourcepath选项就是定义java源文件的查找目录， -classpath选项就是定义class文件的查找目录。
                 */

                options = null;
                String targetDir = Thread.currentThread().getContextClassLoader().getResource("").getPath().replaceAll("/[^/]+\\.jar!/BOOT-INF/classes!/", "").replace("file:", "");
                log.info("-----user.dir-----: " + targetDir);
                String jarPath = getJarFiles(targetDir);
                log.info("-----jarPath-----: " + jarPath);

                if(StringUtils.isNotEmpty(jarPath)) {
                    options = Arrays.asList("-encoding", "UTF-8", "-classpath", jarPath, "-d", targetDir, "-sourcepath", targetDir);
                    log.info("-classpath --> " + jarPath);
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
        }
        catch (Exception e){
            log.error(LOG_PRE_SUFFIX + "动态生成Entity错误！！！", e);
        }

        return null;
    }

    public static Object create(String tablename, Map<String, Object> data) {
        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
        if(data != null) {
            for(Map.Entry<String, Object> item : data.entrySet()) {

                ColumnInfo col = new ColumnInfo();
                col.setColumnName(item.getKey());
                col.setDefaultValue(item.getValue().toString());
                col.setDataType(item.getValue().getClass().getTypeName());
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

    public static Object create(String tablename, QueryInfo queryInfo) {
        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
        if(queryInfo != null) {
            if(queryInfo.getConditionList() != null) {
                for (ConditionInfo item : queryInfo.getConditionList()) {
                    if ("id".equals(item.getName().toLowerCase())) {
                        continue;
                    }
                    ColumnInfo col = new ColumnInfo();
                    col.setColumnName(item.getName());
                    col.setDefaultValue(item.getValue().toString());
                    col.setDataType(item.getValue().getClass().getTypeName());

                    columns.add(col);
                }
            }

            if(queryInfo.getSortList() != null) {
                for (SortInfo item : queryInfo.getSortList()) {
                    if ("id".equals(item.getName().toLowerCase())) {
                        continue;
                    }
                    ColumnInfo col = new ColumnInfo();
                    col.setColumnName(item.getName());
                    col.setDataType("object");

                    columns.add(col);
                }
            }

            if(queryInfo.getKeywords() != null) {
                for (FieldInfo item : queryInfo.getKeywords()) {
                    if ("id".equals(item.getName().toLowerCase())) {
                        continue;
                    }
                    ColumnInfo col = new ColumnInfo();
                    col.setColumnName(item.getName());
                    col.setDefaultValue(item.getValue().toString());
                    col.setDataType("string");

                    columns.add(col);
                }
            }

            if(queryInfo.getGroupList() != null) {
                for (String name : queryInfo.getGroupList()) {
                    if ("id".equals(name.toLowerCase())) {
                        continue;
                    }
                    ColumnInfo col = new ColumnInfo();
                    col.setColumnName(name);
                    col.setDataType("object");

                    columns.add(col);
                }
            }
        }

        columns.add(new ColumnInfo("id", "text", true));

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
                    public boolean accept(File pathname) {
                        if (pathname.isDirectory()) {
                            return true;
                        } else {
                            String name = pathname.getName();
                            if (name.endsWith(".jar")) {
                                log.info("-----jar: " + pathname.getPath());
                                jars[0] = jars[0] + pathname.getPath() + ";";
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
}
