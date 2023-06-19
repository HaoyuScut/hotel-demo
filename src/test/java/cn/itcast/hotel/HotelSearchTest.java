package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Map;

/**
 * @author 王浩宇
 * @version 1.0
 * @create 2023/06/18 20:23
 * 查询的基本步骤是:
 * 1．创建SearchRequest对象
 * 2．准备Request.source()，也就是DSL。QueryBuilders来构建查询条件
 * 传入Request.source()的query()方法3．发送请求，得到结果
 * 4．解析结果（参考JSON结果，从外到内，逐层解析)
 */

@SpringBootTest
public class HotelSearchTest {

    private RestHighLevelClient client;

    /**
     * 查询所有
     *
     * @throws IOException
     */
    @Test
    void testMathchAll() throws IOException {
        //1.准备Request
        SearchRequest request = new SearchRequest("hotel");
        //2.准备DSL
        request.source().query(QueryBuilders.matchAllQuery());
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.解析结果
        handleResponse(response);
//        System.out.println(response);
    }


    @Test
    void testMath() throws IOException {
        //1.准备Request
        SearchRequest request = new SearchRequest("hotel");
        //2.准备DSL
        request.source()
                .query(QueryBuilders.matchQuery("all", "如家"));
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.解析结果
        handleResponse(response);
    }

    /**
     * Boolean查询
     *
     * @throws IOException
     */
    @Test
    void testBool() throws IOException {
        //1.准备Request
        SearchRequest request = new SearchRequest("hotel");
        //2.准备DSL
        //2.1 准备BooleanQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //2.2 添加term
        boolQuery.must(QueryBuilders.termQuery("city", "上海"));
        //2.3 添加filter
        boolQuery.filter(QueryBuilders.rangeQuery("price").lte(252));

        request.source().query(boolQuery);
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.解析结果
        handleResponse(response);
    }

    /**
     * 排序和分页
     *
     * @throws IOException
     */
    @Test
    void testPageAndSort() throws IOException {
        // 页码，每页大小
        int page = 2, size = 5;
        //1.准备Request
        SearchRequest request = new SearchRequest("hotel");
        //2.准备DSL
        //2.1 query
        request.source().query(QueryBuilders.matchAllQuery());
        //2.2 排序sort
        request.source().sort("price", SortOrder.ASC);
        //2.3 分页 from size
        request.source().from((page - 1) * size).size(size);
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.解析结果
        handleResponse(response);
    }

    /**
     * 高亮
     *
     * @throws IOException
     */
    @Test
    void testHighLight() throws IOException {
        //1.准备Request
        SearchRequest request = new SearchRequest("hotel");
        //2.准备DSL
        request.source().query(QueryBuilders.matchQuery("all", "如家"));
        //2.2 高亮
        request.source().highlighter(
                new HighlightBuilder()
                        .field("name").requireFieldMatch(false));

        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.解析结果
        handleResponse(response);
    }

    private static void handleResponse(SearchResponse response) {
        //4.解析结果
        SearchHits searchHits = response.getHits();
        //4.1 获取总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("共搜索到" + total + "条数据");
        //4.2 文档数组
        SearchHit[] hits = searchHits.getHits();
        //4.3 遍历
        for (SearchHit hit : hits) {
            // 获取文档source
            String json = hit.getSourceAsString();
            //反序列化
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            //获取高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
//            if(highlightFields == null || highlightFields.size() == 0) {
//
//            }
            if(!CollectionUtils.isEmpty(highlightFields)) {
                // 根据字段名获取高亮结果
                HighlightField highlightField = highlightFields.get("name");
                if(highlightField != null) {
                    //获取高亮值
                    String name = highlightField.getFragments()[0].string();
                    hotelDoc.setName(name);
                }
            }

            System.out.println(hotelDoc);
        }
    }


    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.200.135:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }
}
