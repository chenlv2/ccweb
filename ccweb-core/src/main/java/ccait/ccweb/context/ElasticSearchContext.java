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

import ccait.ccweb.model.PageInfo;
import ccait.ccweb.model.SearchData;
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
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class ElasticSearchContext {

    private JestClient client;

    @Value(value = "${entity.elasticSearch.enable:false}")
    private String enableEasticSearch;

    @Value(value = "${entity.elasticSearch.url:}")
    private String url;

    @Value(value = "${entity.elasticSearch.timeout:10000}")
    private int timeout;

    @PostConstruct
    private void postConstruct() {

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

        DeleteIndex deleteIndex = new DeleteIndex.Builder(indexname).build();
        JestResult result = client.execute(deleteIndex);
    }



    /**
     * 清缓存
     * @throws Exception
     */
    public void clearCache() throws Exception {

        ClearCache closeIndex = new ClearCache.Builder().build();
        JestResult result = client.execute(closeIndex);
    }



    /**
     * 关闭索引
     * @throws Exception
     */
    public void closeIndex(String indexname) throws Exception {

        CloseIndex closeIndex = new CloseIndex.Builder(indexname).build();
        JestResult result = client.execute(closeIndex);
    }

    /**
     * 优化索引
     * @throws Exception
     */
    public void optimize() throws Exception {

        Optimize optimize = new Optimize.Builder().build();
        JestResult result = client.execute(optimize);
    }

    /**
     * 刷新索引
     * @throws Exception
     */
    public void flush() throws Exception {

        Flush flush = new Flush.Builder().build();
        JestResult result = client.execute(flush);
    }

    /**
     * 判断索引是否存在
     * @throws Exception
     */
    public void indicesExists(String indexname) throws Exception {

        IndicesExists indicesExists = new IndicesExists.Builder(indexname).build();
        JestResult result = client.execute(indicesExists);
    }

    /**
     * 查看节点信息
     * @throws Exception
     */
    public void nodesInfo() throws Exception {

        NodesInfo nodesInfo = new NodesInfo.Builder().build();
        JestResult result = client.execute(nodesInfo);
    }


    /**
     * 查看集群健康信息
     * @throws Exception
     */
    public void health() throws Exception {

        Health health = new Health.Builder().build();
        JestResult result = client.execute(health);
    }

    /**
     * 节点状态
     * @throws Exception
     */
    public void nodesStats() throws Exception {
        NodesStats nodesStats = new NodesStats.Builder().build();
        JestResult result = client.execute(nodesStats);
    }

    /**
     * 更新Document
     * @throws Exception
     */
    private void updateDocument(String indexname, String id, String content) throws Exception {

        Update update = new Update.Builder(content).index(indexname).id(id).build();

        JestResult result = client.execute(update);
    }


    /**
     * 删除Document
     * @param id
     * @throws Exception
     */
    private void deleteDocument(String indexname, String id) throws Exception {

        Delete delete = new Delete.Builder(id).index(indexname).build();

        JestResult result = client.execute(delete);
    }

    /**
     * 获取Document
     * @param id
     * @throws Exception
     */
    private <T> T getDocument(String indexname, String id, Class<T> clazz) throws Exception {

        Get get = new Get.Builder(indexname, id).build();
        JestResult jestResult = client.execute(get);
        T result = jestResult.getSourceAsObject(clazz);

        return result;
    }

    /**
     * Suggestion
     * @throws Exception
     */
    private void suggest() throws Exception{
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
     * 查询全部
     * @throws Exception
     */
    private <T> SearchData<List<T>> search(String indexname, String queryString, boolean heightlight, Class<T> clazz) throws Exception {

        SearchData<List<T>> result = new SearchData<List<T>>();
        List<T> list = new ArrayList<T>();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        if(StringUtils.isEmpty(queryString)) {
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        }
        else {
            searchSourceBuilder.query(QueryBuilders.queryStringQuery(queryString));
        }

        if(heightlight) {
            searchSourceBuilder.highlight().field("title");//高亮title
            searchSourceBuilder.highlight().field("content");//高亮content
            searchSourceBuilder.highlight().preTags("<em>").postTags("</em>");//高亮标签
            searchSourceBuilder.highlight().fragmentSize(200);//高亮内容长度
        }

        Search search = new Search.Builder(searchSourceBuilder.toString())
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

    public <T> void batchIndex(String indexname, List<T> datas) throws Exception {

        if(datas == null || datas.size() < 1)  {
            return;
        }

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

        Index index = new Index.Builder(data).index(indexname).build();
        JestResult jestResult = client.execute(index);

        return jestResult;
    }
}
