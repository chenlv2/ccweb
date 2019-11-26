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

import ccait.ccweb.context.ApplicationContext;
import com.alibaba.excel.annotation.ExcelProperty;
import entity.query.ColumnInfo;
import entity.query.Queryable;
import entity.query.annotation.*;
import entity.query.core.ApplicationConfig;
import entity.tool.util.StringUtils;
import javapoet.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;
import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.CharBuffer;
import java.sql.Blob;
import java.util.*;
import java.util.stream.Collectors;

import static ccait.ccweb.utils.StaticVars.CURRENT_DATASOURCE;

/**
 * In-memory java file manager.
 *
 * @author michael
 */
public class MemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    // compiled classes in bytes:
    final Map<String, byte[]> classBytes = new HashMap<String, byte[]>();

    private static final String DEFAULT_PACKAGE = "ccait.ccweb.entites";

    MemoryJavaFileManager(JavaFileManager fileManager) {
        super(fileManager);
    }

    public Map<String, byte[]> getClassBytes() {
        return new HashMap<String, byte[]>(this.classBytes);
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
        classBytes.clear();
    }

    @Override
    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, Kind kind,
                                               FileObject sibling) throws IOException {
        if (kind == Kind.CLASS) {
            return new MemoryOutputJavaFileObject(className);
        } else {
            return super.getJavaFileForOutput(location, className, kind, sibling);
        }
    }

    JavaFileObject makeStringSource(String name, String code) {
        return new MemoryInputJavaFileObject(name, code);
    }

    static class MemoryInputJavaFileObject extends SimpleJavaFileObject {

        final String code;

        MemoryInputJavaFileObject(String name, String code) {
            super(URI.create("string:///" + name), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharBuffer getCharContent(boolean ignoreEncodingErrors) {
            return CharBuffer.wrap(code);
        }
    }

    class MemoryOutputJavaFileObject extends SimpleJavaFileObject {
        final String name;

        MemoryOutputJavaFileObject(String name) {
            super(URI.create("string:///" + name), Kind.CLASS);
            this.name = name;
        }

        @Override
        public OutputStream openOutputStream() {
            return new FilterOutputStream(new ByteArrayOutputStream()) {
                @Override
                public void close() throws IOException {
                    out.close();
                    ByteArrayOutputStream bos = (ByteArrayOutputStream) out;
                    classBytes.put(name, bos.toByteArray());
                }
            };
        }
    }

    public static JavaFile getJavaFile(List<ColumnInfo> columns, String tablename, String primaryKey, String scope, String suffix) {

        String packagePath = ApplicationConfig.getInstance().get("entity.package", DEFAULT_PACKAGE);
        String className = String.format("%s%s", tablename.substring(0, 1).toUpperCase() + tablename.substring(1), suffix);
        TypeSpec.Builder builder = TypeSpec.classBuilder(className).addModifiers(getModifier(scope));
        builder.addAnnotation(Component.class);

        String datasource = (String) ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE);
        if(StringUtils.isNotEmpty(datasource)) {
            AnnotationSpec annDataSource = AnnotationSpec.builder(DataSource.class).addMember("value", "$S", datasource).build();
            builder.addAnnotation(annDataSource);
        }

        AnnotationSpec annScopeSpec = AnnotationSpec.builder(Scope.class).addMember("value", "$S", "prototype").build();
        builder.addAnnotation(annScopeSpec);

        AnnotationSpec annClassSpec = AnnotationSpec.builder(Tablename.class).addMember("value", "$S", tablename).build();
        builder.addAnnotation(annClassSpec);

        builder.superclass(ParameterizedTypeName.get(
                ClassName.get(Queryable.class), ClassName.get(packagePath, className))
        );

        JavaFile javaFile = null;

        try {
            for(int i=0; i<columns.size(); i++) {
                ColumnInfo col = columns.get(i);
                if(col.getColumnName().indexOf(".") > 0) {
                    List<String> data = StringUtils.splitString2List(col.getColumnName(), ".");
                    if(data.get(0).toLowerCase()
                            .equals(tablename.toLowerCase())) {
                        col.setColumnName(data.get(1));
                    }
                    else {
                        continue;
                    }
                }

                FieldSpec.Builder fldSpec = FieldSpec.builder(getFieldType(col.getDataType()), col.getColumnName(), Modifier.PRIVATE);

                if(col.getColumnComment() != null) {
                    fldSpec.addJavadoc(col.getColumnComment());
                }

                if(col.getPrimaryKey() || primaryKey.toLowerCase().equals(col.getColumnName().toLowerCase())) {
                    AnnotationSpec annPrimaryKey = AnnotationSpec.builder(PrimaryKey.class).build();
                    fldSpec.addAnnotation(annPrimaryKey);

                    ClassName typename = getFieldType(col.getDataType());
                    if(Integer.class.getTypeName().equals(typename.canonicalName()) ||
                            Long.class.getTypeName().equals(typename.canonicalName())) {
                        col.setIsAutoIncrement(true);
                    }
                }

                if(col.getIsAutoIncrement() != null && col.getIsAutoIncrement()) {
                    AnnotationSpec annAutoIncrement = AnnotationSpec.builder(AutoIncrement.class).build();
                    fldSpec.addAnnotation(annAutoIncrement);
                }

                AnnotationSpec annFieldSpec = AnnotationSpec.builder(Fieldname.class).addMember("value", "$S", col.getColumnName()).build();
                fldSpec.addAnnotation(annFieldSpec);

                if(StringUtils.isNotEmpty(col.getAlias())) {
                    AnnotationSpec annExcelProperty = AnnotationSpec.builder(ExcelProperty.class)
                            .addMember("value", "$S", col.getAlias())
                            .addMember("index", "$S", i).build();
                    fldSpec.addAnnotation(annExcelProperty);
                }

                builder.addField(fldSpec.build());

                MethodSpec getter = genGetter(col);
                builder.addMethod(getter);

                MethodSpec setter = genSetter(col);
                builder.addMethod(setter);
            }

            javaFile = JavaFile.builder(packagePath, builder.build()).build();

            javaFile.writeTo(System.out);
        }

        catch (Exception e) {
            e.printStackTrace();
        }

        return javaFile;
    }

    private static MethodSpec.Builder getMethodName(String prefix, String columnName) {

        List<String> names = StringUtils.splitString2List(columnName, "_").stream()
                .map(a->a.substring(0, 1).toUpperCase() + a.substring(1)).collect(Collectors.toList());
        columnName = StringUtils.join("", names);
        String methodName = prefix.toLowerCase() + columnName.substring(0, 1).toUpperCase() + columnName.substring(1);

        return MethodSpec.methodBuilder(methodName);
    }

    private static MethodSpec genGetter(ColumnInfo col) {
        MethodSpec.Builder method = getMethodName("get", col.getColumnName());
        if(col.getColumnComment() == null) {
            method.addModifiers(Modifier.PUBLIC).returns(getFieldType(col.getDataType())).addStatement(String.format("return this.%s", col.getColumnName()));
        }

        else {
            method.addJavadoc(col.getColumnComment()).addModifiers(Modifier.PUBLIC).returns(getFieldType(col.getDataType())).addStatement(String.format("return this.%s", col.getColumnName()));
        }

        return method.build();
    }

    private static MethodSpec genSetter(ColumnInfo col) {
        MethodSpec.Builder method = getMethodName("set", col.getColumnName());

        if(col.getColumnComment() == null) {
            method.addParameter(getFieldType(col.getDataType()), col.getColumnName())
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement(String.format("this.%s = %s", col.getColumnName(), col.getColumnName()));
        }

        else {
            method.addParameter(getFieldType(col.getDataType()), col.getColumnName())
                    .addJavadoc(col.getColumnComment())
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement(String.format("this.%s = %s", col.getColumnName(), col.getColumnName()));
        }

        return method.build();
    }

    private static Modifier getModifier(String classScope) {
        switch (classScope.toUpperCase()) {
            case "PUBLIC":
                return Modifier.PUBLIC;
            case "ABSTRACT":
                return Modifier.ABSTRACT;
            case "PROTECTED":
                return Modifier.PROTECTED;
            case "PRIVATE":
                return Modifier.PRIVATE;
            case "FINAL":
                return Modifier.FINAL;
            default:
                return Modifier.DEFAULT;
        }
    }

    private static ClassName getFieldType(String dataType) {

        if(StringUtils.isEmpty(dataType)) {
            return ClassName.get(String.class);
        }

        switch (dataType.toLowerCase()) {
            case "tinyint":
            case "smallint":
            case "mediumint":
            case "int":
            case "integer":
            case "java.lang.integer":
            case "bigserial":
            case "serial":
                return ClassName.get(Integer.class);
            case "bigint":
            case "timestamp":
            case "long":
            case "java.lang.long":
                return ClassName.get(Long.class);
            case "char":
            case "nchar":
                return ClassName.get(Character.class);
            case "varchar":
            case "text":
            case "string":
            case "java.lang.string":
            case "uniqueidentifier":
            case "guid":
            case "nvarchar":
            case "ntext":
            case "tinytext":
            case "mediumtext":
            case "longtext":
            case "character":
            case "clob":
            case "nclob":
            case "character varying":
                return ClassName.get(String.class);
            case "blob":
            case "binary":
            case "varbinary":
            case "image":
            case "graphic":
            case "vargraphic":
            case "bytes":
            case "bytea":
            case "java.sql.blob":
                return ClassName.get(Blob.class);
            case "decimal":
            case "numeric":
            case "smallmoney":
            case "money":
            case "bigdecimal":
            case "java.math.bigdecimal":
                return ClassName.get(BigDecimal.class);
            case "float":
            case "real":
            case "java.lang.float":
                return ClassName.get(Float.class);
            case "set":
                return ClassName.get(Set.class);
            case "date":
            case "time":
            case "datetime":
            case "year":
            case "smalldatetime":
            case "java.util.date":
                return ClassName.get(Date.class);
            case "boolean":
            case "bit":
            case "bool":
            case "java.lang.boolean":
                return ClassName.get(Boolean.class);
            case "byte":
            case "varying":
            case "varbit":
            case "java.lang.byte":
                return ClassName.get(Byte.class);
            case "precision":
            case "double":
            case "java.lang.double":
                return ClassName.get(Double.class);
            default:
                return ClassName.get(Object.class);
        }
    }
}
