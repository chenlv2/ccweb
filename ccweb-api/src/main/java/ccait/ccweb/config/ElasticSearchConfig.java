package ccait.ccweb.config;

import entity.tool.util.StringUtils;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
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
     * Bean name default
     * @return
     */
    @Bean(name = "transportClient")
    public TransportClient transportClient() {
        logger.info("Elasticsearch初始化开始。。。。。");
        TransportClient transportClient = null;
        try {
            // 配置信息
            Settings esSetting = Settings.builder()
                    .put("cluster.name", clusterName) //集群名字
                    .put("client.transport.sniff", true)//增加嗅探机制，找到ES集群
                    .put("thread_pool.search.size", poolSize)//增加线程池个数
                    .build();
            //配置信息Settings自定义
            transportClient = new PreBuiltTransportClient(esSetting);
            List<String> nodes = StringUtils.splitString2List(clusterNodes, ",");
            for(String nodeString : nodes) {
                String hostName = nodeString;
                Integer port = 9200;
                if(nodeString.indexOf(":") > 0) {
                    List<String> node = StringUtils.splitString2List(clusterNodes, ":");
                    hostName = node.get(0);
                    port = Integer.parseInt(node.get(1));
                }
                TransportAddress transportAddress = new InetSocketTransportAddress(InetAddress.getByName(hostName), port);
                transportClient.addTransportAddresses(transportAddress);
            }
        } catch (Exception e) {
            logger.error("elasticsearch TransportClient create error!!", e);
        }
        return transportClient;
    }
}
