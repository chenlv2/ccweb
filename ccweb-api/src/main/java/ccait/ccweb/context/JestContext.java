/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */

package ccait.ccweb.context;

import ccait.ccweb.entites.*;
import ccait.ccweb.enums.Algorithm;
import ccait.ccweb.model.PageInfo;
import ccait.ccweb.model.SearchData;
import ccait.ccweb.utils.FastJsonUtils;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import entity.tool.util.ThreadUtils;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.cluster.Health;
import io.searchbox.cluster.NodesInfo;
import io.searchbox.cluster.NodesStats;
import io.searchbox.core.*;
import io.searchbox.indices.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class JestContext {

    @Autowired
    private JestClient jestClient;

    @Value(value = "${elasticSearch.enable:false}")
    private boolean enableEasticSearch;

    @Value(value = "${elasticSearch.defaultMatch:match_node}")
    private String defaultMatch;

    private static final Logger log = LogManager.getLogger( JestContext.class );

    @PostConstruct
    private void postConstruct() {
    }

    /**
     * 将删除所有的索引
     * @throws Exception
     */
    public void deleteIndex(String indexname) throws Exception {

        if(jestClient == null) {
            return;
        }

        final String index = indexname.toLowerCase().replaceAll("[\\[`]([^\\[\\]]+)[\\]`]", "$1");

        if(enableEasticSearch) {
            ThreadUtils.async(new Runnable() {
                @Override
                public void run() {
                    try {
                        DeleteIndex deleteIndex = new DeleteIndex.Builder(index).build();
                        JestResult result = jestClient.execute(deleteIndex);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });
        }
    }



    /**
     * 清缓存
     * @throws Exception
     */
    public void clearCache() throws Exception {

        if(jestClient == null) {
            return;
        }

        ClearCache closeIndex = new ClearCache.Builder().build();
        JestResult result = jestClient.execute(closeIndex);
    }



    /**
     * 关闭索引
     * @throws Exception
     */
    public void closeIndex(String indexname) throws Exception {

        if(jestClient == null) {
            return;
        }
        final String index = ensureIndexName(indexname);

        if(enableEasticSearch) {
            ThreadUtils.async(new Runnable() {
                @Override
                public void run() {
                    try {
                        CloseIndex closeIndex = new CloseIndex.Builder(index).build();
                        JestResult result = jestClient.execute(closeIndex);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });
        }
    }

    /**
     * 优化索引
     * @throws Exception
     */
    public void optimize() throws Exception {

        if(jestClient == null) {
            return;
        }

        Optimize optimize = new Optimize.Builder().build();
        JestResult result = jestClient.execute(optimize);
    }

    /**
     * 刷新索引
     * @throws Exception
     */
    public JestResult flush() throws Exception {

        Flush flush = new Flush.Builder().build();
        JestResult result = jestClient.execute(flush);

        return result;
    }

    /**
     * 判断索引是否存在
     * @throws Exception
     */
    public boolean indicesExists(String indexname) throws Exception {
        String index = ensureIndexName(indexname);
        IndicesExists indicesExists = new IndicesExists.Builder(index).build();
        JestResult result = jestClient.execute(indicesExists);

        return result.isSucceeded();
    }

    /**
     * 查看节点信息
     * @throws Exception
     */
    public JestResult nodesInfo() throws Exception {

        NodesInfo nodesInfo = new NodesInfo.Builder().build();
        JestResult result = jestClient.execute(nodesInfo);

        return result;
    }


    /**
     * 查看集群健康信息
     * @throws Exception
     */
    public JestResult health() throws Exception {

        Health health = new Health.Builder().build();
        JestResult result = jestClient.execute(health);

        return result;
    }

    /**
     * 节点状态
     * @throws Exception
     */
    public JestResult nodesStats() throws Exception {
        NodesStats nodesStats = new NodesStats.Builder().build();
        JestResult result = jestClient.execute(nodesStats);

        return result;
    }

    /**
     * 更新Document
     * @throws Exception
     */
    public void updateDocument(String indexname, String id, String content) throws Exception {

        final String index = ensureIndexName(indexname);

        if(enableEasticSearch) {
            ThreadUtils.async(new Runnable() {
                @Override
                public void run() {
                    try {
                        Update update = new Update.Builder(content).index(index).id(id).build();
                        jestClient.execute(update);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });
        }
    }


    /**
     * 删除Document
     * @param id
     * @throws Exception
     */
    public void deleteDocument(String indexname, String id) throws Exception {

        final String index = ensureIndexName(indexname);

        if(enableEasticSearch) {
            ThreadUtils.async(new Runnable() {
                @Override
                public void run() {
                    try {
                        Delete delete = new Delete.Builder(id).index(index).build();
                        jestClient.execute(delete);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });
        }
    }

    /**
     * 获取Document
     * @param id
     * @throws Exception
     */
    public <T> T getDocument(String indexname, String id, Class<T> clazz) throws Exception {

        indexname = indexname.toLowerCase();
        Get get = new Get.Builder(indexname, id).build();
        JestResult jestResult = jestClient.execute(get);
        T result = jestResult.getSourceAsObject(clazz);

        return result;
    }

    /**
     * 查询
     * @throws Exception
     */
    public SearchData<List<Map>> search(String indexname, SearchInfo queryInfo) throws Exception {
        String queryString = FastJsonUtils.convertObjectToJSON(getQueryMap(queryInfo));
        SearchData<List<Map>> result = search(indexname, queryString, false);
        if(queryInfo.getPageInfo() != null) {
            result.getPageInfo().setPageIndex(queryInfo.getPageInfo().getPageIndex());
            result.getPageInfo().setPageSize(queryInfo.getPageInfo().getPageSize());
        }

        return result;
    }
    public SearchData<List<Map>> search(String indexname, String queryString, boolean heightlight) throws Exception {

        indexname = ensureIndexName(indexname);
        SearchData<List<Map>> result = new SearchData<List<Map>>();
        List<Map> list = new ArrayList<Map>();

        Search search = new Search.Builder(queryString)
                .addIndex(indexname)
                .build();
        SearchResult searchResult = jestClient.execute(search);
        if(searchResult.getResponseCode() != 200) {
            throw new Exception(searchResult.getErrorMessage());
        }
        List<SearchResult.Hit<Map,Void>> hits = searchResult.getHits(Map.class);
        for (SearchResult.Hit<Map, Void> hit : hits) {

            if(heightlight) {
                //获取高亮后的内容
                result.getHeightlightList().add( hit.highlight );
            }

            list.add(hit.source);
        }

        result.setPageInfo(new PageInfo());
        JsonElement total = searchResult.getJsonObject().get("hits").getAsJsonObject().get("total").getAsJsonObject().get("value");
        if(total != null) {
            result.getPageInfo().setTotalRecords(total.getAsInt());
        }

        result.setData(list);

        return result;
    }

    public String getQueryMap(SearchInfo queryInfo) {

        if(queryInfo == null) {
            return defaultMatch;
        }

        Map<String, Object> esMap = new HashMap<String, Object>();
        Map<String, Object> queryMap = new HashMap<String, Object>();
        Map<String, Object> boolMap = new HashMap<String, Object>();

        if(queryInfo.getSortList() != null && queryInfo.getSortList().size() > 0) {

            Map<String, Object> map = new HashMap<String, Object>();
            for(SortInfo item : queryInfo.getSortList()) {
                map.put(item.getName(), new HashMap<String, String>() { { put("order", item.isDesc() ? "desc" : "asc"); } });
            }

            esMap.put("sort", map);
        }

        if(queryInfo.getGroupList() != null && queryInfo.getGroupList().size() > 0) {

            Map<String, Object> map = new HashMap<String, Object>();
            for(String group : queryInfo.getGroupList()) {
                Matcher m = Pattern.compile("\\s*((\\w+)\\()?(\\w+)\\)?\\s+[aA][sS]\\s+(\\w+)\\s*").matcher(group);
                if(!m.matches()) {
                    continue;
                }

                map.put(m.group(4), new HashMap<String, Object>() {{
                    put(m.group(2), new HashMap<String, Object>() {{
                        put("field", m.group(3));
                    }});
                }});
            }

            esMap.put("aggs", map);
        }

        if(queryInfo.getPageInfo().getPageSize() > 0) {
            esMap.put("size", queryInfo.getPageInfo().getPageSize());
        }

        if(queryInfo.getSkip() > 0) {
            esMap.put("from", queryInfo.getSkip());
        }

        if(queryInfo.getConditionList() != null && queryInfo.getConditionList().size() > 0) {
            List<Map<String, Object>> mustList = new ArrayList<Map<String, Object>>();
            queryInfo.getConditionList().stream().filter(a-> Algorithm.EQ.equals(a.getAlgorithm())).forEach(item->{
                mustList.add(new HashMap<String, Object>() {{
                    put("term", new HashMap<String, Object>(){{ put(item.getName(), item.getValue()); }});
                }});
            });

            queryInfo.getConditionList().stream().filter(a-> Algorithm.LIKE.equals(a.getAlgorithm())).forEach(item->{
                mustList.add(new HashMap<String, Object>() {{
                    put("wildcard", new HashMap<String, Object>(){{ put(item.getName(), item.getValue()); }});
                }});
            });

            queryInfo.getConditionList().stream().filter(a-> Algorithm.IN.equals(a.getAlgorithm())).forEach(item->{
                mustList.add(new HashMap<String, Object>() {{
                    put("terms", new HashMap<String, Object>(){{ put(String.format("%s.keyword", item.getName()), item.getValue()); }});
                }});
            });

            queryInfo.getConditionList().stream().filter(a-> Algorithm.GT.equals(a.getAlgorithm()) ||
                    Algorithm.GTEQ.equals(a.getAlgorithm()) || Algorithm.LT.equals(a.getAlgorithm()) ||
                    Algorithm.LTEQ.equals(a.getAlgorithm())).forEach(item->{

                mustList.add(new HashMap<String, Object>() {{

                    put("range", new HashMap<String, Object>(){{
                        put(item.getName(), new HashMap<String, Object>(){{
                            String alg = "eq";
                            switch (item.getAlgorithm()) {
                                case GT:
                                    alg = "gt";
                                    break;
                                case LT:
                                    alg = "lt";
                                case LTEQ:
                                    alg = "lte";
                                    break;
                                case GTEQ:
                                    alg = "gte";
                                    break;
                            }
                            put(alg, item.getValue());
                        }});
                    }});
                }});
            });

            if(mustList.size() > 0) {
                boolMap.put("must", mustList);
            }

            List<Map<String, Object>> mustNotList = new ArrayList<Map<String, Object>>();
            queryInfo.getConditionList().stream().filter(a-> Algorithm.NOT.equals(a.getAlgorithm())).forEach(item->{
                mustNotList.add(new HashMap<String, Object>() {{
                    put("term", new HashMap<String, Object>(){{ put(item.getName(), item.getValue()); }});
                }});
            });

            queryInfo.getConditionList().stream().filter(a-> Algorithm.NOTIN.equals(a.getAlgorithm())).forEach(item->{
                mustNotList.add(new HashMap<String, Object>() {{
                    put("terms", new HashMap<String, Object>(){{ put(String.format("%s.keyword", item.getName()), item.getValue()); }});
                }});
            });

            if(mustNotList.size() > 0) {
                boolMap.put("must_not", mustNotList);
            }
        }

        if(queryInfo.getKeywords() != null && queryInfo.getKeywords().size() > 0) {
            boolMap.put("should", queryInfo.getKeywords().stream().map(item->new HashMap<String, Object>() { {
                put("wildcard", new HashMap<String, Object>(){{ put(item.getName(), item.getValue()); }});
            } }).collect(Collectors.toList()));
        }

        if(boolMap.size() == 0) {
            queryMap.put("match_all", new HashMap());
        }

        else {
            queryMap.put("bool", boolMap);
        }

        esMap.put("query", queryMap);

        return FastJsonUtils.convertObjectToJSON(esMap);
    }

    public <T> void batchIndex(String indexname, List<T> datas) throws Exception {

        if(datas == null || datas.size() < 1)  {
            return;
        }

        final String index = ensureIndexName(indexname);

        if(enableEasticSearch) {
            ThreadUtils.async(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<Index> builderList = new ArrayList<>();

                        for(T data : datas) {
                            builderList.add(new Index.Builder(JsonUtils.toJson(data)).build());
                        }

                        Bulk bulk = new Bulk.Builder()
                                .defaultIndex(index)
                                .addAction(builderList).build();

                        jestClient.execute(bulk);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });
        }
    }

    public String ensureIndexName(String indexname) {
        return indexname.toLowerCase().replaceAll("[\\[`]([^\\[\\]]+)[\\]`]", "$1");
    }
    /**
     * 插入单条数据
     * 若该条数据已经存在,则覆盖。
     * @return
     */
    public <T> boolean insertOrUpdateDoc(T data, String uniqueId, String index) {
        Index.Builder builder = new Index.Builder(JsonUtils.toJson(data));
        builder.id(uniqueId);
        builder.refresh(true);
        Index indexDoc = builder.index(index).type(index).build();
        JestResult result;
        try {
            result = jestClient.execute(indexDoc);
            if (result != null && result.isSucceeded()) {
                return true;
            }

            else {
                log.error("ESJestClient insertDoc exception===>" + result.getErrorMessage());
            }
        } catch (IOException e) {
            log.error("ESJestClient insertDoc exception", e);
        }
        return false;
    }

    public <T> void createIndex(String indexname, String uniqueId, T data) throws Exception {

        if(data == null)  {
            return;
        }

        final String index = indexname.toLowerCase().replaceAll("[\\[`]([^\\[\\]]+)[\\]`]", "$1");

        if(enableEasticSearch) {
            ThreadUtils.async(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(!indicesExists(index)) {
                            jestClient.execute(new CreateIndex.Builder(index).build());
                        }
                        insertOrUpdateDoc(data, uniqueId, index);

                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });
        }
    }

    public static JestClient createClient(int timeout, String clusterNodes) {
        List<String> urlList = StringUtils.splitString2List(clusterNodes, ",", "http://%s");
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder(urlList)
                //.defaultCredentials("elastic","changeme") //如果使用了x-pack，就要添加用户名和密码
                .gson(new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create())
                .multiThreaded(true)
                .connTimeout(timeout)
                .readTimeout(timeout)
                .build());
        JestClient jestClient = factory.getObject();

        return jestClient;
    }
}
