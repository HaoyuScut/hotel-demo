package cn.itcast.hotel.pojo;

import lombok.Data;

import java.util.List;

/**
 * @author 王浩宇
 * @version 1.0
 * @create 2023/06/19 17:19
 */

@Data
public class PageResult {
    private Long total;
    private List<HotelDoc> hotels;

    public PageResult(Long total, List<HotelDoc> hotels) {
        this.total = total;
        this.hotels = hotels;
    }

    public PageResult() {
    }
}
