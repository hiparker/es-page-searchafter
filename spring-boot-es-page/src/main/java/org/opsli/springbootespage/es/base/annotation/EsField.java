package org.opsli.springbootespage.es.base.annotation;


import org.opsli.springbootespage.es.base.enums.AnalyzerType;
import org.opsli.springbootespage.es.base.enums.FieldType;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Elasticsearch 字段
 *
 * @author WULEI
 * @date 2022年2月24日16:05:06
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
public @interface EsField {

    @AliasFor("name")
    String value() default "";

    @AliasFor("value")
    String name() default "";

    FieldType type() default FieldType.Auto;

    String datePattern() default "";

    /** 指定分词器 */
    AnalyzerType analyzer() default AnalyzerType.STANDARD;

}