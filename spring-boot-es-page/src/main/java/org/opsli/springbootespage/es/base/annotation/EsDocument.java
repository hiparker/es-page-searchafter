package org.opsli.springbootespage.es.base.annotation;

import java.lang.annotation.*;


/**
 * 标识为 Elasticsearch的 索引文件
 *
 * @author WULEI
 * @date 2022年2月18日17:16:12
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
public @interface EsDocument {


    /**
     * index : 索引名称
     * @return String
     */
    String indexName();

}