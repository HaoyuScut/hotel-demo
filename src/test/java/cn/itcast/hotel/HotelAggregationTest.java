package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author 王浩宇
 * @version 1.0
 * @create 2023/06/18 20:23
 */

@SpringBootTest
public class HotelAggregationTest {

    private RestHighLevelClient client;

    @Test
    void testAggregation() throws IOException {
        // 1. 准备Request
        SearchRequest request = new SearchRequest("hotel");
        // 2. 准备DSL
        //2.1 设置size
        request.source().size(0);
        //2.2 聚合
        request.source().aggregation(AggregationBuilders
                .terms("brandAgg")
                .field("brand")
                .size(10)
        );


        // 3. 发出请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        // 4.解析结果
        //4.1 解析聚合结果
        Aggregations aggregations = response.getAggregations();
        //4.2 根据名称获取聚合结果
        Terms brandTerms = aggregations.get("brandAgg");
        //4.3 获取桶
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        //4.4 遍历
        for (Terms.Bucket bucket : buckets) {
            String key = bucket.getKeyAsString();
            long docCount = bucket.getDocCount();
            System.out.println(key + ":" + docCount);
        }
//        System.out.println(response);
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
            if (!CollectionUtils.isEmpty(highlightFields)) {
                // 根据字段名获取高亮结果
                HighlightField highlightField = highlightFields.get("name");
                if (highlightField != null) {
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
