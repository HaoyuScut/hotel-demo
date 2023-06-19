package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

import static cn.itcast.hotel.constants.HotelConstants.MAPPING_TEMPLATE;

/**
 * @author 王浩宇
 * @version 1.0
 * @create 2023/06/18 20:23
 * 文档操作的基本步骤:
 * 初始化RestHighLevelClient
 * 创建XxxRequest。XXX是Index、Get、Update、Delete·
 * 准备参数(Index和Update时需要)
 * 发送请求。调用RestHighLevelClient#.xxx()方法，
 * xXx是index、get、update、delete
 * 解析结果（Get时需要)
 */

@SpringBootTest
public class HotelDocumentTest {

    @Autowired
    private IHotelService hotelService;

    private RestHighLevelClient client;

    @Test
    void testInit() {
        System.out.println(client);
    }

   @Test
   void testAddDocument() throws IOException {
        //根据id查询酒店数据
       Hotel hotel = hotelService.getById(61083L);
       //转换为文档类型
       HotelDoc hotelDoc = new HotelDoc(hotel);

       //1.准备Request对象
       IndexRequest request = new IndexRequest("hotel").id(hotelDoc.getId().toString());
       //2.准备Json文档
       request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
       //3.发送请求
       client.index(request, RequestOptions.DEFAULT);
   }

   @Test
   void testGetDocumentById() throws IOException {
        //1.准备准备Request对象
       GetRequest request = new GetRequest("hotel", "61083");
       //2.发送请求，得到相应
       GetResponse response = client.get(request, RequestOptions.DEFAULT);

       //3.解析响应结果
       String json = response.getSourceAsString();

       HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
       System.out.println(hotelDoc);
   }

    @Test
    void testUpdateDocumentById() throws IOException {
        //1.准备准备Request对象
        UpdateRequest request = new UpdateRequest("hotel", "61083");

        //2.准备请求参数
        request.doc(
                "price", "952",
                "starName", "四钻"
        );

        //3.发送请求，得到相应
        client.update(request, RequestOptions.DEFAULT);
    }

    @Test
    void testDeleteDocumentById() throws IOException {
        //1.准备准备Request对象
        DeleteRequest request = new DeleteRequest("hotel", "618032");

        //2.发送请求
        client.delete(request, RequestOptions.DEFAULT);
    }

    @Test
    void testBulkRequest() throws IOException {
        //批量查询酒店数据
        List<Hotel> hotels = hotelService.list();
        //转换为文档类型HotelDoc



        //1.准备准备Request对象
        BulkRequest request = new BulkRequest();

        //2.准备请求参数
        for(Hotel hotel : hotels) {
            HotelDoc hotelDoc = new HotelDoc(hotel);
            //创建新增文档的
            request.add(new IndexRequest("hotel")
                    .id(hotelDoc.getId().toString())
                    .source(JSON.toJSONString(hotelDoc), XContentType.JSON));
        }


        //3.发送请求，得到相应
        client.bulk(request, RequestOptions.DEFAULT);
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
