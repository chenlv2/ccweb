package ccait.ccweb.processor;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Properties;

@Resource
@Component
public class ApplicationConfigProcessor implements EnvironmentPostProcessor {

    private static final String ENCODING = "UTF-8";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment configurableEnvironment, SpringApplication springApplication) {
        File file = getApplicationConfig(configurableEnvironment, ".yml");
        if(file != null  && file.exists()) {
            setPropertys(configurableEnvironment, loadPropertiesByYaml(file));
            return;
        }

        file = getApplicationConfig(configurableEnvironment, ".yaml");
        if(file != null  && file.exists()) {
            setPropertys(configurableEnvironment, loadPropertiesByYaml(file));
            return;
        }

        file = getApplicationConfig(configurableEnvironment, ".properties");
        if(file != null  && file.exists()) {
            setPropertys(configurableEnvironment, loadProperties(file));
            return;
        }
    }

    public File getApplicationConfig(ConfigurableEnvironment configurableEnvironment, String suffix) {
        //tomcat路径
        String property = System.getProperty("catalina.home");
        String path =property+ File.separator + "conf" + File.separator+"application" + suffix;
        File file = new File(path);
        if (file.exists()) {
            return file;
        }
        else {
            file = new File(System.getProperty("user.dir") + "/application" + suffix);
        }

        if(file.exists()) {
            return file;
        }
        else {
            file = new File(System.getProperty("user.dir") + "/config/application" + suffix);
        }

        if(file.exists()) {
            return file;
        }
        else {
            file = new File(System.getProperty("user.dir") + "/conf/application" + suffix);
        }

        if(file.exists()) {
            return file;
        }
        else {
            file = new File(System.getProperty("user.dir") + "/resources/application" + suffix);
        }

        if(file.exists()) {
            return file;
        }
        else {
            file = new File(System.getProperty("user.dir") + "/src/main/resources/application" + suffix);
        }
        return file;
    }

    private void setPropertys(ConfigurableEnvironment configurableEnvironment, Properties properties) {
        MutablePropertySources propertySources = configurableEnvironment.getPropertySources();

        //以外部配置文件为准
        propertySources.addFirst(new PropertiesPropertySource("Config", properties));
        //以application.properties文件为准
        //propertySources.addLast(new PropertiesPropertySource("Config", properties));
    }

    private Properties loadProperties(File f) {
        FileSystemResource resource = new FileSystemResource(f);
        try {
            return PropertiesLoaderUtils.loadProperties(resource);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to load local settings from " + f.getAbsolutePath(), ex);
        }
    }

    public Properties loadPropertiesByYaml(File file) {
        final String DOT = ".";
        Properties result = new Properties();
        try {
            YAMLFactory yamlFactory = new YAMLFactory();
            YAMLParser parser = yamlFactory.createParser(
                    new InputStreamReader(new FileInputStream(file), Charset.forName(ENCODING)));

            String key = "";
            String value = null;
            JsonToken token = parser.nextToken();
            while (token != null) {
                if (JsonToken.START_OBJECT.equals(token)) {
                    // do nothing
                } else if (JsonToken.FIELD_NAME.equals(token)) {
                    if (key.length() > 0) {
                        key = key + DOT;
                    }
                    key = key + parser.getCurrentName();

                    token = parser.nextToken();
                    if (JsonToken.START_OBJECT.equals(token)) {
                        continue;
                    }
                    value = parser.getText();
                    result.setProperty(key, value);

                    int dotOffset = key.lastIndexOf(DOT);
                    if (dotOffset > 0) {
                        key = key.substring(0, dotOffset);
                    }
                    value = null;
                } else if (JsonToken.END_OBJECT.equals(token)) {
                    int dotOffset = key.lastIndexOf(DOT);
                    if (dotOffset > 0) {
                        key = key.substring(0, dotOffset);
                    } else {
                        key = "";
                    }
                }
                token = parser.nextToken();
            }
            parser.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }
}
