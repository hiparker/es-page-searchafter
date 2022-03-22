package org.opsli.springbootespage.es.base.annotation;

import java.lang.annotation.*;

/**
 * 注解标识 标识为ES的ID
 *
 * @author WULEI
 * @date 2022年2月18日17:15:40
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
public @interface EsDataId {

}
