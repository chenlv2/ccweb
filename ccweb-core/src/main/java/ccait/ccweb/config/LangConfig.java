package ccait.ccweb.config;

import entity.query.core.ApplicationConfig;
import entity.tool.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LangConfig {

    private static final Logger log = LogManager.getLogger(LangConfig.class);

    private final static Map<String, String> configMap = new HashMap<String, String>();

    private static Map<String, Object> map;

    private String lang;

    private static LangConfig instance;

    public static LangConfig getInstance() {

        if(instance != null) {
            return instance;
        }
        synchronized (configMap) {
            return new LangConfig();
        }
    }

    private LangConfig() {
        try {
            lang = ApplicationConfig.getInstance().get("${entity.lang}");
            init();
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void init() throws FileNotFoundException {

        if(configMap.size() > 0) {
            return;
        }

        synchronized (configMap) {
            Yaml yml = new Yaml();

            InputStream configStream = getConfigStream();

            map = yml.loadAs(configStream, HashMap.class);

            fillKeyValue("", map);
        }
    }

    private InputStream getConfigStream() throws FileNotFoundException {
        String property = System.getProperty("catalina.home");
        String path = property+ File.separator + "conf" + File.separator+"lang.yml";
        File file = new File(path);
        if(file.exists()) {
            return new FileInputStream(file);
        }

        file = new File(property+ File.separator + "conf" + File.separator+"lang.yaml");
        if(file.exists()) {
            return new FileInputStream(file);
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + "/lang.yaml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + "/config/lang.yml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + "/config/lang.yaml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + "/conf/lang.yml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + "/conf/lang.yaml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + "/resources/lang.yml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + "/resources/lang.yaml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + "/src/main/resources/lang.yml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }
        else {
            file = new File(System.getProperty("user.dir") + "/src/main/resources/lang.yaml");
        }

        if(file.exists()) {
            return new FileInputStream(file);
        }

        try {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream("lang.yml");
        }
        catch (Exception e) {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream("lang.yaml");
        }
    }


    private synchronized void fillKeyValue(String key, Map<String, Object> map) {

        for(Map.Entry<String, Object> entry : map.entrySet()) {

            String currentKey = String.format("%s%s", StringUtils.isEmpty(key) ? key : key + ".", entry.getKey());
            if(entry.getValue() instanceof Map) {
                fillKeyValue(currentKey, (Map<String, Object>) entry.getValue());
            }

            else {
                configMap.put(String.format("%s", currentKey.toLowerCase()), entry.getValue().toString());
            }
        }
    }

    public String get(String key) {
        return get(key, "");
    }

    public String get(String key, String defaultValue) {

        if(StringUtils.isNotEmpty(lang)) {
            key = String.format("%s.%s", lang, key);
        }

        key = key.toLowerCase();

        if(configMap == null || configMap.size() < 1) {
            return defaultValue;
        }

        if(configMap.containsKey(key) && StringUtils.isNotEmpty(configMap.get(key))) {
            return configMap.get(key);
        }

        return defaultValue;
    }
}
