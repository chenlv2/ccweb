/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 *  @author linlurui
 *  @Date Date: 2019-02-10
 */


package ccait.ccweb.config;

import ccait.ccweb.context.EntityContext;
import ccait.ccweb.context.IndexingContext;
import ccait.ccweb.context.TriggerContext;
import ccait.ccweb.filter.InitializationFilter;
import ccait.ccweb.interceptor.SecurityInterceptor;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import entity.query.core.DataSourceFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.session.web.http.CookieHttpSessionStrategy;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;
import org.springframework.web.servlet.config.annotation.*;


@Configuration
@EnableAutoConfiguration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(final AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(300000);
        configurer.registerCallableInterceptors(timeoutInterceptor());
    }

    @Bean
    public TimeoutCallableProcessingInterceptor timeoutInterceptor() {
        return new TimeoutCallableProcessingInterceptor();
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        AntPathMatcher matcher = new AntPathMatcher();
        matcher.setCaseSensitive(false);
        configurer.setPathMatcher(matcher);
        configurer.setUseRegisteredSuffixPatternMatch(false);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("POST", "GET", "PUT", "OPTIONS", "DELETE")
                .maxAge(5555)
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityInterceptor())
                .excludePathPatterns("/static/*")
                .excludePathPatterns("/error")
                .excludePathPatterns("/template")
                .addPathPatterns("/**");
    }

    @Bean
    public EntityContext entityContext()
    {
        return new EntityContext();
    }

    @Bean
    public IndexingContext indexingContext() { return new IndexingContext(); }

    @Bean
    DataSourceFactory dataSourceFactory() { return new DataSourceFactory(); }

    @Bean
    public CookieHttpSessionStrategy httpSessionStrategy() {
        return new CookieHttpSessionStrategy();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() { //application.yml->@Value
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public FilterRegistrationBean requestFilter()
    {
        FilterRegistrationBean bean = new FilterRegistrationBean(new InitializationFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return bean;
    }

    @Bean
    public SecurityInterceptor securityInterceptor() {
        return new SecurityInterceptor();
    }

    @Bean
    public HttpMessageConverters fastJsonConfigure(){
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setSerializerFeatures(SerializerFeature.PrettyFormat);
        //日期格式化
        fastJsonConfig.setDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        converter.setFastJsonConfig(fastJsonConfig);
        return new HttpMessageConverters(converter);
    }

    @Bean
    public TriggerContext triggerContext()
    {
        return new TriggerContext();
    }
}
