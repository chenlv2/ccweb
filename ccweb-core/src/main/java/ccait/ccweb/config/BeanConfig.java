/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.config;

import ccait.ccweb.annotation.Trigger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Map;

import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;


@Lazy(true)
@Configuration
public class BeanConfig  implements BeanDefinitionRegistryPostProcessor {

    private static final Logger log = LogManager.getLogger( BeanConfig.class );

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {

    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory postProcessBeanFactory) throws BeansException {

        Map<String, Object> map = postProcessBeanFactory.getBeansWithAnnotation(Trigger.class);
        if(map == null) {
            return;
        }

        for (String key : map.keySet()) {

            log.info(LOG_PRE_SUFFIX + "beanName= "+ key);
        }

    }
}
