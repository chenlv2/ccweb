package ccait.ccweb.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

@Resource
@Component
public class CommonEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment configurableEnvironment, SpringApplication springApplication) {
        //tomcat路径
        String property = System.getProperty("catalina.home");
        System.out.println("catalina home: " + property);
        String path =property+ File.separator + "conf" + File.separator+"application.properties";
        File file = new File(path);
        if (file.exists()) {
            setPropertys(configurableEnvironment, file);
            return;
        }
        else {
            file = new File(System.getProperty("user.dir") + "/application.properties");
        }

        if(file.exists()) {
            setPropertys(configurableEnvironment, file);
            return;
        }
        else {
            file = new File(System.getProperty("user.dir") + "/resources/application.properties");
        }

        if(file.exists()) {
            setPropertys(configurableEnvironment, file);
            return;
        }
        else {
            file = new File(System.getProperty("user.dir") + "/src/main/resources/application.properties");
        }

        Yaml yml = new Yaml();

        if(file.exists()) {
            setPropertys(configurableEnvironment, file);
            return;
        }
    }

    private void setPropertys(ConfigurableEnvironment configurableEnvironment, File file) {
        MutablePropertySources propertySources = configurableEnvironment.getPropertySources();
        Properties properties = loadProperties(file);

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
}
