package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    @Autowired
    private RestHighLevelClient client;

    @Override
    public PageResult search(RequestParams params) {
        try {
            //1.准备Request
            SearchRequest request = new SearchRequest("hotel");
            //2.准备DSL
            //2.1 关键字搜索query
            // 构建BooleanQuery
            buildBasicQuery(params, request);
            //2.2分页
            int page = params.getPage();
            int size = params.getSize();

            //2.3 排序
            String location = params.getLocation();
            if (location != null && !location.equals("")) {
                request.source().sort(SortBuilders
                        .geoDistanceSort("location", new GeoPoint(location))
                        .order(SortOrder.ASC)
                        .unit(DistanceUnit.KILOMETERS));
            }

            request.source().from((page - 1) * size).size(size);
            //3.发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //4.解析结果
            PageResult pageResult = handleResponse(response);
            return pageResult;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, List<String>> filter(RequestParams params) {
        try {
            // 1. 准备Request
            SearchRequest request = new SearchRequest("hotel");
            // 2. 准备DSL
            //2.1.query
            buildBasicQuery(params, request);
            //2.2 设置size
            request.source().size(0);
            //2.3 聚合
            buildAggregation(request);

            // 3. 发出请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

            // 4.解析结果
            Map<String, List<String>> result = new HashMap<>();
            //4.1 解析聚合结果
            Aggregations aggregations = response.getAggregations();
            //4.2 根据品牌名称，获取品牌结果
            List<String> brandList = getAggByName(aggregations, "brandAgg");
            //4.3 放入map
            result.put("brand", brandList);
            //4.2 根据品牌名称，获取品牌结果
            List<String> cityList = getAggByName(aggregations, "cityAgg");
            //4.3 放入map
            result.put("city", cityList);
            //4.2 根据品牌名称，获取品牌结果
            List<String> starList = getAggByName(aggregations, "starNameAgg");
            //4.3 放入map
            result.put("starName", starList);

            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 实现自动补全
     * @param prefix
     * @return
     */
    @Override
    public List<String> getSuggestions(String prefix) {
        try {
            //1.准备Request
            SearchRequest request = new SearchRequest("hotel");

            //2.准备DSL
            request.source().suggest(new SuggestBuilder().addSuggestion(
                    "title_suggest",
                    SuggestBuilders.completionSuggestion("suggestion")
                            .prefix(prefix)
                            .skipDuplicates(true)
                            .size(10)
            ));

            //3.发起请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

            //4.解析结果
            Suggest suggest = response.getSuggest();
            //4.1 根据补全查询名称获取补全结果
            CompletionSuggestion suggestion = suggest.getSuggestion("title_suggest");
            //4.2 获取Options,进行遍历
            List<CompletionSuggestion.Entry.Option> options = suggestion.getOptions();
            List<String> list = new ArrayList<>(options.size());
            for (CompletionSuggestion.Entry.Option option : options) {
                String text = option.getText().toString();
                list.add(text);
            }
            return list;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> getAggByName(Aggregations aggregations, String aggName) {
        //4.2 根据名称获取聚合结果
        Terms brandTerms = aggregations.get(aggName);
        //4.3 获取桶
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        //4.4 遍历
        List<String> aggList = new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
            //4.5 获取key
            String key = bucket.getKeyAsString();
            aggList.add(key);
        }
        return aggList;
    }

    private static void buildAggregation(SearchRequest request) {
        request.source().aggregation(AggregationBuilders
                .terms("brandAgg")
                .field("brand")
                .size(100)
        );
        request.source().aggregation(AggregationBuilders
                .terms("cityAgg")
                .field("city")
                .size(100)
        );
        request.source().aggregation(AggregationBuilders
                .terms("starNameAgg")
                .field("starName")
                .size(100)
        );
    }

    private static void buildBasicQuery(RequestParams params, SearchRequest request) {
        //1.构建BooleanQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //关键字搜索
        String key = params.getKey();
        if (key == null || key.equals("")) {
            boolQuery.must(QueryBuilders.matchAllQuery());
        } else {
            boolQuery.must(QueryBuilders.matchQuery("all", key));
        }
        // 条件过滤
        //城市条件
        if (params.getCity() != null && !params.getCity().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("city", params.getCity()));
        }
        //品牌条件
        if (params.getBrand() != null && !params.getBrand().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("brand", params.getBrand()));
        }
        //星级条件
        if (params.getStartName() != null && !params.getStartName().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("startName", params.getStartName()));
        }
        //价格
        if (params.getMinPrice() != null && params.getMaxPrice() != null) {
            boolQuery.filter(QueryBuilders
                    .rangeQuery("price").gte(params.getMinPrice()).lte(params.getMaxPrice()));
        }

        //2.算分控制
        FunctionScoreQueryBuilder functionScoreQuery =
                QueryBuilders.functionScoreQuery(
                        //原始查询，相关性算分的查询
                        boolQuery,
                        //function score的数组
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                ////其中的一个function score元素
                                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                        //过滤条件
                                        QueryBuilders.termQuery("isAD", "true"),
                                        //算分函数
                                        ScoreFunctionBuilders.weightFactorFunction(10)
                                )
                        });

        request.source().query(functionScoreQuery);
    }


    private static PageResult handleResponse(SearchResponse response) {
        //4.解析结果
        SearchHits searchHits = response.getHits();
        //4.1 获取总条数
        long total = searchHits.getTotalHits().value;
//        System.out.println("共搜索到" + total + "条数据");
        //4.2 文档数组
        SearchHit[] hits = searchHits.getHits();
        //4.3 遍历
        List<HotelDoc> hotels = new ArrayList<>();
        for (SearchHit hit : hits) {
            // 获取文档source
            String json = hit.getSourceAsString();
            //反序列化
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);

            //获取排序值
            Object[] sortValues = hit.getSortValues();
            if (sortValues.length > 0) {
                Object sortValue = sortValues[0];
                hotelDoc.setDistance(sortValue);
            }
            hotels.add(hotelDoc);
//            System.out.println(hotelDoc);
        }
        //4.4 封装返回
        return new PageResult(total, hotels);
    }
}
