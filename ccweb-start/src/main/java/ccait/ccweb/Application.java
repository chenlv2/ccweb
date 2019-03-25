package ccait.ccweb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    private static final Logger log = LogManager.getLogger( Application.class );
    private static boolean uat = false;
    public static void main(String[] args) {

        String flag = System.getProperty("isuat");
        if("true".equals( flag )) {
            setUat( true );
        }

        else {
            setUat( false );
        }

        CcwebAppliction.run(Application.class, args);
    }

    public static boolean isUat()
    {
        return uat;
    }

    private static void setUat( boolean isUat )
    {
        Application.uat = isUat;
    }
}
