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

import ccait.ccweb.model.*;
import ccait.ccweb.utils.FastJsonUtils;
import com.google.gson.GsonBuilder;
import entity.tool.util.StringUtils;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.cluster.Health;
import io.searchbox.cluster.NodesInfo;
import io.searchbox.cluster.NodesStats;
import io.searchbox.core.*;
import io.searchbox.indices.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class IndexingContext {

    private JestClient client;

    @Value(value = "${entity.elasticSearch.enable:false}")
    private boolean enableEasticSearch;

    @Value(value = "${entity.elasticSearch.url:}")
    private String url;

    @Value(value = "${entity.elasticSearch.timeout:10000}")
    private int timeout;

    @Value(value = "${entity.elasticSearch.defaultMatch:match_node}")
    private String defaultMatch;

    @PostConstruct
    private void postConstruct() {

        if(!enableEasticSearch) {
            return;
        }

        List<String> urlList = StringUtils.splitString2List(url, ";", "http://%s");
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder(urlList)
                .gson(new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create())
                .multiThreaded(true)
                .readTimeout(timeout)
                .build());
        client = factory.getObject();
    }

    /**
     * 将删除所有的索引
     * @throws Exception
     */
    public void deleteIndex(String indexname) throws Exception {

        if(client == null) {
            return;
        }
        indexname = indexname.toLowerCase();
        DeleteIndex deleteIndex = new DeleteIndex.Builder(indexname).build();
        JestResult result = client.execute(deleteIndex);
    }



    /**
     * 清缓存
     * @throws Exception
     */
    public void clearCache() throws Exception {

        if(client == null) {
            return;
        }

        ClearCache closeIndex = new ClearCache.Builder().build();
        JestResult result = client.execute(closeIndex);
    }



    /**
     * 关闭索引
     * @throws Exception
     */
    public void closeIndex(String indexname) throws Exception {

        if(client == null) {
            return;
        }
        indexname = indexname.toLowerCase();
        CloseIndex closeIndex = new CloseIndex.Builder(indexname).build();
        JestResult result = client.execute(closeIndex);
    }

    /**
     * 优化索引
     * @throws Exception
     */
    public void optimize() throws Exception {

        if(client == null) {
            return;
        }

        Optimize optimize = new Optimize.Builder().build();
        JestResult result = client.execute(optimize);
    }

    /**
     * 刷新索引
     * @throws Exception
     */
    public JestResult flush() throws Exception {

        Flush flush = new Flush.Builder().build();
        JestResult result = client.execute(flush);

        return result;
    }

    /**
     * 判断索引是否存在
     * @throws Exception
     */
    public boolean indicesExists(String indexname) throws Exception {
        indexname = indexname.toLowerCase();
        IndicesExists indicesExists = new IndicesExists.Builder(indexname).build();
        JestResult result = client.execute(indicesExists);

        return result.isSucceeded();
    }

    /**
     * 查看节点信息
     * @throws Exception
     */
    public JestResult nodesInfo() throws Exception {

        NodesInfo nodesInfo = new NodesInfo.Builder().build();
        JestResult result = client.execute(nodesInfo);

        return result;
    }


    /**
     * 查看集群健康信息
     * @throws Exception
     */
    public JestResult health() throws Exception {

        Health health = new Health.Builder().build();
        JestResult result = client.execute(health);

        return result;
    }

    /**
     * 节点状态
     * @throws Exception
     */
    public JestResult nodesStats() throws Exception {
        NodesStats nodesStats = new NodesStats.Builder().build();
        JestResult result = client.execute(nodesStats);

        return result;
    }

    /**
     * 更新Document
     * @throws Exception
     */
    public boolean updateDocument(String indexname, String id, String content) throws Exception {

        indexname = indexname.toLowerCase();
        Update update = new Update.Builder(content).index(indexname).id(id).build();

        JestResult result = client.execute(update);

        return result.isSucceeded();
    }


    /**
     * 删除Document
     * @param id
     * @throws Exception
     */
    public boolean deleteDocument(String indexname, String id) throws Exception {

        indexname = indexname.toLowerCase();
        Delete delete = new Delete.Builder(id).index(indexname).build();

        JestResult result = client.execute(delete);

        return result.isSucceeded();
    }

    /**
     * 获取Document
     * @param id
     * @throws Exception
     */
    public <T> T getDocument(String indexname, String id, Class<T> clazz) throws Exception {

        indexname = indexname.toLowerCase();
        Get get = new Get.Builder(indexname, id).build();
        JestResult jestResult = client.execute(get);
        T result = jestResult.getSourceAsObject(clazz);

        return result;
    }

    /**
     * Suggestion
     * @throws Exception
     */
    public void suggest() throws Exception{
        String suggestionName = "my-suggestion";

        Suggest suggest = new Suggest.Builder("{" +
                "  \"" + suggestionName + "\" : {" +
                "    \"text\" : \"the amsterdma meetpu\"," +
                "    \"term\" : {" +
                "      \"field\" : \"body\"" +
                "    }" +
                "  }" +
                "}").build();
        SuggestResult suggestResult = client.execute(suggest);
        System.out.println(suggestResult.isSucceeded());
        List<SuggestResult.Suggestion> suggestionList = suggestResult.getSuggestions(suggestionName);
        System.out.println(suggestionList.size());
        for(SuggestResult.Suggestion suggestion:suggestionList){
            System.out.println(suggestion.text);
        }
    }

    /**
     * 查询
     * @throws Exception
     */
    public <T> SearchData<List<T>> search(String indexname, QueryInfo queryInfo, Class<T> clazz) throws Exception {
        String queryString = FastJsonUtils.convertObjectToJSON(getQueryMap(queryInfo));
        return search(indexname, queryString, false, clazz);
    }
    public <T> SearchData<List<T>> search(String indexname, QueryInfo queryInfo, boolean heightlight, Class<T> clazz) throws Exception {
        String queryString = FastJsonUtils.convertObjectToJSON(getQueryMap(queryInfo));
        return search(indexname, queryString, false, clazz);

    }
    public <T> SearchData<List<T>> search(String indexname, String queryString, boolean heightlight, Class<T> clazz) throws Exception {

        indexname = indexname.toLowerCase();
        SearchData<List<T>> result = new SearchData<List<T>>();
        List<T> list = new ArrayList<T>();

        Search search = new Search.Builder(queryString)
                .addIndex(indexname)
                .build();
        SearchResult searchResult = client.execute(search);
        List<SearchResult.Hit<T,Void>> hits = searchResult.getHits(clazz);
        for (SearchResult.Hit<T, Void> hit : hits) {

            if(heightlight) {
                //获取高亮后的内容
                result.getHeightlightList().add( hit.highlight );
            }

            list.add(hit.source);
        }

        result.setPageInfo(new PageInfo());
        result.getPageInfo().setTotalRecords(searchResult.getTotal());

        result.setData(list);

        return result;
    }

    public String getQueryMap(QueryInfo queryInfo) {

        if(queryInfo == null) {
            return defaultMatch;
        }

        Map<String, Object> queryMap = new HashMap<String, Object>();

        if(queryInfo.getSortList() != null && queryInfo.getSortList().size() > 0) {

            Map<String, Object> map = new HashMap<String, Object>();
            for(SortInfo item : queryInfo.getSortList()) {
                map.put(item.getName(), new HashMap<String, String>() { { put("order", item.isDesc() ? "desc" : "asc"); } });
            }

            queryMap.put("sort", map);
        }

        if(queryInfo.getSelectList() != null && queryInfo.getSelectList().size() > 0) {

            Map<String, Object> map = new HashMap<String, Object>();
            queryMap.put("stored_fields", queryInfo.getSelectList().stream().map(a->a.getField()).collect(Collectors.toList()));
        }

        if(queryInfo.getGroupList() != null && queryInfo.getGroupList().size() > 0) {

            Map<String, Object> map = new HashMap<String, Object>();
            for(SortInfo item : queryInfo.getSortList()) {
                map.put("field", item.getName());
            }

            queryMap.put("collapse", map);
        }

        if(queryInfo.getPageInfo().getPageSize() > 0) {
            queryMap.put("size", queryInfo.getPageInfo().getPageSize());
        }

        if(queryInfo.getSkip() > 0) {
            queryMap.put("search_after", queryInfo.getSkip());
        }

        Map<String, Object> matchMap = new HashMap<String, Object>();
        if(queryInfo.getConditionList() != null && queryInfo.getConditionList().size() > 0) {
            Map<String, Object> map = new HashMap<String, Object>();
            for(ConditionInfo item : queryInfo.getConditionList()) {
                map.put(item.getName(), new HashMap<String, String>() { {
                    put("query", item.getValue().toString());
                    put("operator", item.getAlgorithm().getValue().toLowerCase().trim());
                } });
            }

            matchMap.put("match", map);
        }

        if(queryInfo.getKeywords() != null && queryInfo.getKeywords().size() > 0) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("must", queryInfo.getKeywords().stream().map(a->new HashMap<String, String>() { {
                put(a.getName(), a.getValue().toString());
            } }).collect(Collectors.toList()));

            matchMap.put("bool", map);
        }

        if(matchMap.size() > 0) {
            queryMap.put("query", matchMap);
        }

        return FastJsonUtils.convertObjectToJSON(queryMap);
    }

    public <T> void batchIndex(String indexname, List<T> datas) throws Exception {

        if(datas == null || datas.size() < 1)  {
            return;
        }

        indexname = indexname.toLowerCase();
        List<Index> builderList = new ArrayList<>();

        for(T data : datas) {
            builderList.add(new Index.Builder(data).build());
        }

        Bulk bulk = new Bulk.Builder()
                .defaultIndex(indexname)
                .addAction(builderList).build();

        client.execute(bulk);
    }


    public <T> JestResult createIndex(String indexname, T data) throws Exception {

        if(data == null)  {
            return null;
        }

        indexname = indexname.toLowerCase();
        Index index = new Index.Builder(data).index(indexname).build();
        JestResult jestResult = client.execute(index);

        return jestResult;
    }
}
