package ccait.ccweb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.web.reactive.config.EnableWebFlux;

@EnableWebFlux
@SpringBootApplication( exclude = {DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,} )
public class CcwebAppliction {

    private static final Logger log = LogManager.getLogger( CcwebAppliction.class );

    public static void main(String[] args) {
        run(null, args);
    }

    public static void run(Class clazz, String[] args) {
        SpringApplication app = new SpringApplication(CcwebAppliction.class);
        if(clazz == null) {
            app.run(args);
        }

        else {
            app.run(clazz, args);
        }

        log.info( "ccweb success start!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" );
    }
}
