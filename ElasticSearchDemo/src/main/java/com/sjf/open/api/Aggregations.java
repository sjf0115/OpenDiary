package com.sjf.open.api;

import com.sjf.open.common.Common;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.MetricsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.min.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by xiaosi on 16-7-4.
 *
 * 聚合功能
 */
public class Aggregations {
    private static final Logger logger = LoggerFactory.getLogger(Aggregations.class);

    private static String INDEX = "qunar-index";
    private static String TYPE = "employee";

    /**
     * 聚合统计
     * 
     * @param index
     * @param type
     */
    public static void aggs(Client client, String index, String type) {

        // 聚合条件
        AggregationBuilder ageAggregationBuilder = AggregationBuilders.terms("by_age").field("age");
        // 子聚合条件
        AggregationBuilder firstNameAggregationBuilder = AggregationBuilders.terms("by_firstName").field("first_name");
        // 聚合条件下添加子聚合条件
        ageAggregationBuilder.subAggregation(firstNameAggregationBuilder);

        // 聚合
        SearchRequestBuilder requestBuilder = client.prepareSearch();
        requestBuilder.setIndices(index);
        requestBuilder.setTypes(type);
        requestBuilder.addAggregation(ageAggregationBuilder);

        // 执行
        SearchResponse searchResponse = requestBuilder.execute().actionGet();

        // 结果
        Map<String, Aggregation> aggMap = searchResponse.getAggregations().asMap();

        LongTerms gradeTerms = (LongTerms) aggMap.get("by_age");

        Iterator<Terms.Bucket> ageBucketIte = gradeTerms.getBuckets().iterator();

        while (ageBucketIte.hasNext()) {
            Terms.Bucket ageBucket = ageBucketIte.next();
            logger.info("{} 年龄 有 {} 个员工 {}", ageBucket.getKey(), ageBucket.getDocCount());
            StringTerms firstNameTerms = (StringTerms) ageBucket.getAggregations().asMap().get("by_firstName");
            Iterator<Terms.Bucket> firstNameBucketIte = firstNameTerms.getBuckets().iterator();
            while (firstNameBucketIte.hasNext()) {
                Terms.Bucket firstNameBucket = firstNameBucketIte.next();
                logger.info("--- {} 年龄 姓 {} 有 {} 个员工", ageBucket.getKey(), firstNameBucket.getKey(),
                        firstNameBucket.getDocCount());
            } // while
        } // while
    }

    /**
     * https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/_metrics_aggregations.html 最小值
     *  最小值
     * @param client
     * @param index
     * @param type
     */
    public static void minAggregation(Client client, String index, String type) {

        // 聚合条件
        MetricsAggregationBuilder minAggregation = AggregationBuilders.min("min_age").field("age");

        // 聚合
        SearchRequestBuilder requestBuilder = client.prepareSearch();
        requestBuilder.setIndices(index);
        requestBuilder.setTypes(type);
        requestBuilder.addAggregation(minAggregation);

        // 执行
        SearchResponse searchResponse = requestBuilder.execute().actionGet();

        // 结果
        Min agg = searchResponse.getAggregations().get("min_age");
        double value = agg.getValue();

        logger.info("员工中最小年龄 {} 聚合名称 {}",value,agg.getName());
    }

    public static void main(String[] args) {
        Client client = Common.createClient();
        minAggregation(client,INDEX,TYPE);
    }
}
