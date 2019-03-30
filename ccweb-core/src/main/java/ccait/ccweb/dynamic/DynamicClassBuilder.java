package ccait.ccweb.dynamic;


import ccait.ccweb.model.ConditionInfo;
import ccait.ccweb.model.FieldInfo;
import ccait.ccweb.model.QueryInfo;
import ccait.ccweb.model.SortInfo;
import entity.query.ColumnInfo;
import entity.query.core.ApplicationConfig;
import javapoet.JavaFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.tools.*;
import java.nio.charset.StandardCharsets;
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
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
            StandardJavaFileManager stdManager = compiler.getStandardFileManager(diagnostics, Locale.CHINESE, StandardCharsets.UTF_8);
            try (MemoryJavaFileManager manager = new MemoryJavaFileManager(stdManager)) {
                JavaFileObject javaFileObject = manager.makeStringSource(String.format("%s.java", className), javaFile.toString());
                JavaCompiler.CompilationTask task = compiler.getTask(null, manager, null, null, null, Arrays.asList(javaFileObject));
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
}
