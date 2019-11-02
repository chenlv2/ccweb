/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */

package ccait.ccweb;

import entity.query.core.ApplicationConfig;
import entity.tool.util.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;


//@EnableHystrixDashboard
//@EnableDiscoveryClient
//@EnableFeignClients
//@EnableHystrix
//@EnableZuulProxy
@EnableEurekaClient
@EnableWebFlux
@SpringBootApplication( exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class} )
public class CcwebAppliction {

    private static final Logger log = LogManager.getLogger( CcwebAppliction.class );

    public static void main(String[] args) throws FileNotFoundException, MalformedURLException {
        run(null, args);
    }

    public static void run(Class clazz, String[] args) throws FileNotFoundException, MalformedURLException {
        SpringApplication app = new SpringApplication(CcwebAppliction.class);
        if(clazz == null) {
            app.run(args);
        }

        else {
            app.run(clazz, args);
        }

        String path = null;
        File file = null;

        if(file==null || !file.exists()) {
            if(StringUtils.isNotEmpty(ApplicationConfig.getInstance().get("${log4j.config.path}"))) {
                String logConfigPath = System.getProperty("user.dir") + "/" +
                        ApplicationConfig.getInstance().get("${log4j.config.path}");

                file = new File(logConfigPath);
            }
        }

        if(!file.exists()) {
            try {
                path = Thread.currentThread().getContextClassLoader()
                        .getResource(ApplicationConfig.getInstance()
                                .get("${log4j.config.path}")).toURI().getPath();

                file = new File(path);
            } catch (URISyntaxException e) {
                System.out.println("URISyntaxException message=======>" + e.getMessage());
            }
        }

        if(!file.exists()) {
            String property = System.getProperty("catalina.home");
            System.out.println("catalina home: " + property);
            path =property+ File.separator + "conf" + File.separator + "log4j2.xml";
            file = new File(path);
        }

        if(file.exists() && StringUtils.isNotEmpty(path)) {

            PropertyConfigurator.configure(path);

            if(file.exists()) {

                ConfigurationSource source = new ConfigurationSource(new FileInputStream(path), file);

                if (source != null) {
                    Configurator.initialize(null, source);
                }
            }

            System.out.println("Current log4j path: " + path);
        }

        log.info( "---------------------------------------------------------------------------------------" );
        log.info( "ccweb started!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" );
    }
}
