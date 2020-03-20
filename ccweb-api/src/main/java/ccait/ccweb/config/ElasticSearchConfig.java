package ccait.ccweb.config;

import ccait.ccweb.context.JestContext;
import com.google.gson.GsonBuilder;
import entity.tool.util.StringUtils;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "elasticSearch", name = "enable", havingValue = "true")
public class ElasticSearchConfig {

    private static final Logger logger = LogManager.getLogger(ElasticSearchConfig.class);

    /**
     * elk集群节点(地址:端口)
     */
    @Value("${elasticsearch.cluster-nodes}")
    private String clusterNodes;

    /**
     * 集群名称
     */
    @Value("${elasticsearch.cluster-name}")
    private String clusterName;

    /**
     * 连接池
     */
    @Value("${elasticsearch.pool:5}")
    private int poolSize;

    /**
     * 超时时间
     */
    @Value("${elasticsearch.timeout:18000}")
    private int timeout;

    /**
     * Bean name jest
     * @return
     */
    @Bean(name = "jestClient")
    public JestClient jestClient() {
        return JestContext.createClient(timeout, clusterNodes);
    }
}
