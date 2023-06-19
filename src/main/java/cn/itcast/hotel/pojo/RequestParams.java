package cn.itcast.hotel.pojo;

import lombok.Data;

/**
 * @author 王浩宇
 * @version 1.0
 * @create 2023/06/19 17:18
 */
@Data
public class RequestParams {
    private String key;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String city;
    private String brand;
    private String startName;
    private Integer minPrice;
    private Integer maxPrice;
    private String location;
}
