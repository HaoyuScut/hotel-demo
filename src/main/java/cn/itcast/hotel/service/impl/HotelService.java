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
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
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
            String key = params.getKey();
            if(key == null || key.equals("")) {
                request.source().query(QueryBuilders.matchAllQuery());
            } else {
                request.source().query(QueryBuilders.matchQuery("all", key));
            }
            //2.2分页
            int page = params.getPage();
            int size = params.getSize();
            ;
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
            hotels.add(hotelDoc);
//            System.out.println(hotelDoc);
        }
        //4.4 封装返回
        return new PageResult(total, hotels);
    }
}
