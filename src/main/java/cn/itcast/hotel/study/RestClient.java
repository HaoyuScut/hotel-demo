package cn.itcast.hotel.study;

/**
 * @author 王浩宇
 * @version 1.0
 * @create 2023/06/18 20:12
 * 初始化JavaRestClient
 * 1.引入es的RestHighLevelclient依赖:
 * 2．因为SpringBoot默认的ES版本是7.6.2，所以我们需要覆盖默认的ES版本:
 * 3．初始化RestHighLevelClient:
 */
public class RestClient {
    /**
     * ES中支持两种地理坐标数据类型:
     * geo_point:由纬度（latitude）和经度( longitude）确定的一个点。
     * 例如:"32.8752345,120.2981576"
     *
     * geo_shape:有多个geo_point组成的复杂几何图形。例如一条直线,
     * "LINESTRING(-77.03653 38.897676,-77.009051 38.889939)"
     *
     * 字段拷贝可以使用copy_to属性将当前字段拷贝到指定字段。示例:
     */

    /**
     * ES是分布式的，所以会面临深度分页问题。例如按price排序后，获取from = 990，size =10的数据:
     * 1．首先在每个数据分片上都排序并查询前1000条文档。
     * 2．然后将所有节点的结果聚合，在内存中重新排序选出前1000条文档
     * 3．最后从这1000条中，选取从990开始的10条文档
     * 如果搜索页数过深，或者结果集（ from + size)
     * 越大，对内存和CPU的消耗也越高。因此ES设定结果集查询的上限是10000
     */
}
