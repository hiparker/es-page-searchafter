package org.opsli.springbootespage.es.base.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.sniff.Sniffer;
import org.opsli.springbootespage.es.base.properties.ElasticsearchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * Elasticsearch 配置
 *
 * @author Parker
 * @date 2022年2月24日16:17:06
 */
@Slf4j
@Configuration
public class ElasticsearchConfigure {

    @Resource
    private ElasticsearchProperties elasticsearchProperties;

    private Sniffer sniffer;
    private RestHighLevelClient highClient;

    private RestClientBuilder getRestClientBuilder() {
        List<String> hosts = elasticsearchProperties.getHosts();
        HttpHost[] httpHosts = new HttpHost[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            String[] hostArray = hosts.get(i).split(":");
            if(hostArray.length == 1){
                httpHosts[i] = new HttpHost(hostArray[0], -1, "http");
            }else if(hostArray.length == 2){
                httpHosts[i] = new HttpHost(hostArray[0], Integer.parseInt(hostArray[1]), "http");
            }
        }
        RestClientBuilder builder = RestClient.builder(httpHosts);

        // 如果开启了 认证模式 则需要验证账号密码
        if(elasticsearchProperties.isAuthEnable()){
            //初始化ES操作客户端
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(
                            elasticsearchProperties.getUsername(), elasticsearchProperties.getPassword()));
            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                httpClientBuilder.disableAuthCaching();
                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            });
        }

        //region 在Builder中设置请求头
        //  1.设置请求头
        Header[] defaultHeaders = new Header[]{
                new BasicHeader("Content-Type", "application/json")
        };
        builder.setDefaultHeaders(defaultHeaders);
        return builder;
    }

    @Bean
    public RestHighLevelClient restHighLevelClient(){
        highClient = new RestHighLevelClient(getRestClientBuilder());

        // 判断是否启用嗅探器
        if(elasticsearchProperties.isSniffEnable()){
            //十秒刷新并更新一次节点
            sniffer = Sniffer.builder(highClient.getLowLevelClient())
                    .setSniffIntervalMillis(elasticsearchProperties.getSniffIntervalMillis())
                    .setSniffAfterFailureDelayMillis(elasticsearchProperties.getSniffAfterFailureDelayMillis())
                    .build();
        }

        return highClient;
    }

    @PreDestroy
    public void preDestroy(){
        // 关闭事件的挂钩
        log.info("ElasticClient ShutdownHook");
        if (null != highClient) {
            try {
                if(null != sniffer){
                    sniffer.close();    //需要在highClient close之前操作
                }
                highClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
