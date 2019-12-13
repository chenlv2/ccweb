/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.generator;


import entity.query.ColumnInfo;
import entity.query.Queryable;
import entity.query.core.ApplicationConfig;
import entity.query.core.DataSource;
import entity.tool.util.StringUtils;
import javapoet.*;
import java.io.File;
import java.util.*;
import entity.query.core.DataSourceFactory;

import static ccait.ccweb.dynamic.MemoryJavaFileManager.getJavaFile;

public class EntitesGenerator {

    private static final String SOURCE_DIR = "/src/main/java/";
    private static final String DEFAULT_PACKAGE = "ccait.ccweb.entites";
    private static final Map<String, Map<String, List<ColumnInfo>>> tableColumnsMap = new HashMap<String, Map<String, List<ColumnInfo>>>();

    static {
        try {
            String configPath = System.getProperty("user.dir") + "/src/main/resources/"
                    + ApplicationConfig.getInstance().get("entity.queryable.configFile", "db-config.xml");

            Collection<DataSource> dsList = DataSourceFactory.getInstance().getAllDataSource(configPath);
            if(dsList == null || dsList.size() < 1) {
                //tomcat路径
                String property = System.getProperty("catalina.home");
                System.out.println("catalina home: " + property);
                String path =property+ File.separator + "conf" + File.separator + "db-config.xml";
                dsList = DataSourceFactory.getInstance().getAllDataSource(path);
            }

            for(DataSource ds : dsList) {
                List<String> tablenames = Queryable.getTables(ds.getId());
                for(String tb : tablenames) {

                    if(StringUtils.isEmpty(tb)) {
                        continue;
                    }

                    List<ColumnInfo> columns = Queryable.getColumns(ds.getId(), tb);
                    if(!tableColumnsMap.containsKey(ds.getId())) {
                        tableColumnsMap.put(ds.getId(), new HashMap<String, List<ColumnInfo>>());
                    }
                    tableColumnsMap.get(ds.getId()).put(tb, columns);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        genCodes();
    }

    public static void genCodes() {
        try {

            String configPath = System.getProperty("user.dir") + "/src/main/resources/"
                    + ApplicationConfig.getInstance().get("entity.queryable.configFile", "db-config.xml");
            String packagePath = ApplicationConfig.getInstance().get("entity.package", DEFAULT_PACKAGE);

            Collection<DataSource> dsList = DataSourceFactory.getInstance().getAllDataSource(configPath);
            for(DataSource ds : dsList) {
                String currentPackage = packagePath + String.format(".%s", ds.getId());
                if(!tableColumnsMap.containsKey(ds.getId())) {
                    continue;
                }

                if(tableColumnsMap.get(ds.getId()) == null) {
                    continue;
                }

                for(Map.Entry<String, List<ColumnInfo>> tb : tableColumnsMap.get(ds.getId()).entrySet()) {

                    if(StringUtils.isEmpty(tb.getKey())) {
                        continue;
                    }

                    if(tb.getValue() == null || tb.getValue().size() < 1) {
                        continue;
                    }

                    String primaryKey = null;

                    try {
                        Object tmp = Queryable.getPrimaryKey(ds.getId(), tb.getKey());
                        if(tmp != null) {
                            primaryKey = Queryable.getPrimaryKey(ds.getId(), tb.getKey()).toString();
                        }
                    }
                    catch (Exception e) {
                        primaryKey = "";
                    }

                    String suffix = ApplicationConfig.getInstance().get("entity.suffix", "Entity");
                    JavaFile javaFile = getJavaFile(tb.getValue(), tb.getKey().toString(), primaryKey, ds.getClassScope(), suffix, true);

                    if(javaFile != null) {

                        File file = new File(System.getProperty("user.dir") + SOURCE_DIR);

                        System.out.println(file.getPath());

                        javaFile.writeTo(file);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean hasColumn(String datasource, String tablename, String columnName) {

        if(StringUtils.isEmpty(tablename)) {
            return false;
        }

        if(StringUtils.isEmpty(columnName)) {
            return false;
        }

        if(tableColumnsMap == null) {
            return false;
        }

        if(!tableColumnsMap.containsKey(datasource)) {
            return false;
        }

        if(tableColumnsMap.get(datasource) == null ||
                !tableColumnsMap.get(datasource).containsKey(tablename) ||
                tableColumnsMap.get(datasource).get(tablename) == null ||
                tableColumnsMap.get(datasource).get(tablename).size() < 1) {
            return false;
        }

        Optional<ColumnInfo> opt = tableColumnsMap.get(datasource).get(tablename).stream().filter(a -> columnName.equals(a.getColumnName())).findAny();
        if(opt == null) {
            return false;
        }

        return opt.isPresent();
    }
}
