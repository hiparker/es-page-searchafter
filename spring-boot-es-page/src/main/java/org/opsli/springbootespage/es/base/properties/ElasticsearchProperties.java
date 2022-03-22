package org.opsli.springbootespage.es.base.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ES 配置文件
 *
 * @author Parker
 * @date 2022年2月18日17:15:40
 */
@Component
@ConfigurationProperties(prefix = "elastic", ignoreInvalidFields=true)
@Data
public class ElasticsearchProperties {

    /** 地址 */
    private List<String> hosts;

    /** 认证校验 */
    private boolean authEnable;
    private String username;
    private String password;

    /** 是否启动嗅探器 */
    private boolean sniffEnable = true;
    /** 嗅探器执行时间 */
    private Integer sniffIntervalMillis = 5000;
    /** 嗅探器错误重试执行时间 */
    private Integer sniffAfterFailureDelayMillis = 15000;

}