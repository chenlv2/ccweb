package ccait.ccweb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.reactive.config.EnableWebFlux;


@EnableHystrixDashboard
@EnableDiscoveryClient
@EnableFeignClients
@EnableEurekaClient
@EnableHystrix
@EnableZuulProxy
@EnableWebFlux
@SpringBootApplication( exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,} )
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
        log.info( "ccweb success start!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" );
        log.info( "ccweb success start!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" );
    }
}
