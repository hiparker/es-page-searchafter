package org.opsli.springbootespage.es.base.components;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.opsli.springbootespage.es.base.annotation.EsDataId;
import org.opsli.springbootespage.es.base.annotation.EsDocument;
import org.opsli.springbootespage.es.base.annotation.EsField;
import org.opsli.springbootespage.es.base.enums.FieldType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch ??????
 *
 * @author WULEI
 * @date 2022???2???18???17:23:14
 */
@Slf4j
@Component
public class ElasticsearchComponent {

    /** ??????????????? */
    private static final int INDEX_NUMBER_OF_SHARDS = 3;
    /** ???????????????  ????????? */
    private static final int INDEX_NUMBER_OF_REPLICAS = 1;
    private static final int SUCCESS_STATE = 200;

    @Resource
    private RestHighLevelClient restHighLevelClient;

    public RestHighLevelClient getInstance() {
        return restHighLevelClient;
    }

    /**
     * ????????????(??????????????????1???????????????0)
     * region ????????????(??????????????????3???????????????1)
     *
     * @param clazz ????????????????????????es??????
     */
    public boolean createIndex(Class<?> clazz) throws Exception {
        EsDocument declaredAnnotation = clazz.getDeclaredAnnotation(EsDocument.class);
        if (declaredAnnotation == null) {
            throw new Exception(
                    String.format("class name: %s can not find Annotation [EsDocument], please check",
                            clazz.getName()));
        }
        String indexName = declaredAnnotation.indexName();
        if(isIndexExists(indexName)){
           return false;
        }

        return createRootIndex(indexName, clazz);
    }

    /**
     * ????????????????????????
     *
     * @param indexName ????????????
     * @return boolean
     */
    public boolean isIndexExists(String indexName) {
        boolean exists = false;
        try {
            GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
            getIndexRequest.humanReadable(true);
            exists = restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exists;
    }

    /**
     * ????????????(??????????????????5???????????????1)
     *
     * @param clazz ????????????????????????es??????
     */
    public boolean createIndexIfNotExist(Class<?> clazz) throws Exception {
        EsDocument declaredAnnotation = clazz.getDeclaredAnnotation(EsDocument.class);
        if (declaredAnnotation == null) {
            throw new Exception(
                    String.format("class name: %s can not find Annotation [EsDocument], please check",
                            clazz.getName()));
        }
        String indexName = declaredAnnotation.indexName();

        boolean indexExists = isIndexExists(indexName);
        if (!indexExists) {
            return createRootIndex(indexName, clazz);
        }
        return false;
    }

    private boolean createRootIndex(String indexName, Class<?> clazz) throws IOException {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.settings(Settings.builder()
                // ?????????????????? ?????????
                .put("index.number_of_shards", INDEX_NUMBER_OF_SHARDS)
                .put("index.number_of_replicas", INDEX_NUMBER_OF_REPLICAS)
        );
        request.mapping(generateBuilder(clazz));
        CreateIndexResponse response = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
        // ??????????????????????????????????????????
        boolean acknowledged = response.isAcknowledged();
        // ???????????????????????????????????????????????????????????????????????????????????????
        boolean shardsAcknowledged = response.isShardsAcknowledged();
        return acknowledged || shardsAcknowledged;
    }

    /**
     * ????????????(??????????????????5???????????????1)???
     * ????????????????????????????????????????????????
     * ??????????????????????????????
     * region ????????????
     *
     * @param clazz ????????????????????????es??????
     */
    public boolean updateIndex(Class<?> clazz) throws Exception {
        EsDocument declaredAnnotation = clazz.getDeclaredAnnotation(EsDocument.class);
        if (declaredAnnotation == null) {
            throw new Exception(
                    String.format("class name: %s can not find Annotation [EsDocument], please check",
                            clazz.getName()));
        }
        String indexName = declaredAnnotation.indexName();
        PutMappingRequest request = new PutMappingRequest(indexName);

        request.source(generateBuilder(clazz));
        AcknowledgedResponse response = restHighLevelClient.indices().putMapping(request, RequestOptions.DEFAULT);
        // ??????????????????????????????????????????

        return response.isAcknowledged();
    }

    /**
     * ????????????
     *
     * @param indexName ????????????
     * @return boolean
     */
    public boolean delIndex(String indexName) {
        boolean acknowledged = false;
        try {
            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);
            deleteIndexRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
            AcknowledgedResponse delete = restHighLevelClient.indices()
                    .delete(deleteIndexRequest, RequestOptions.DEFAULT);
            acknowledged = delete.isAcknowledged();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return acknowledged;
    }

    /**
     * ??????????????????
     * ?????????????????????
     * 1. json
     * 2. map
     * Map<String, Object> jsonMap = new HashMap<>();
     * jsonMap.put("user", "kimchy");
     * jsonMap.put("postDate", new Date());
     * jsonMap.put("message", "trying out Elasticsearch");
     * IndexRequest indexRequest = new IndexRequest("posts")
     * .id("1").source(jsonMap);
     * 3. builder
     * XContentBuilder builder = XContentFactory.jsonBuilder();
     * builder.startObject();
     * {
     * builder.field("user", "kimchy");
     * builder.timeField("postDate", new Date());
     * builder.field("message", "trying out Elasticsearch");
     * }
     * builder.endObject();
     * IndexRequest indexRequest = new IndexRequest("posts")
     * .id("1").source(builder);
     * 4. source:
     * IndexRequest indexRequest = new IndexRequest("posts")
     * .id("1")
     * .source("user", "kimchy",
     * "postDate", new Date(),
     * "message", "trying out Elasticsearch");
     * <p>
     * ?????????  Validation Failed: 1: type is missing;
     * ????????????jar?????????
     * <p>
     * ??????????????????????????????
     *
     * @return IndexResponse
     */
    public IndexResponse index(Object o) throws Exception {
        EsDocument declaredAnnotation = o.getClass().getDeclaredAnnotation(EsDocument.class);
        if (declaredAnnotation == null) {
            throw new Exception(
                    String.format("class name: %s can not find Annotation [EsDocument], please check",
                            o.getClass().getName()));
        }
        String indexName = declaredAnnotation.indexName();

        IndexRequest request = new IndexRequest(indexName);
        Field fieldByAnnotation = getFieldByAnnotation(o, EsDataId.class);
        if (fieldByAnnotation != null) {
            fieldByAnnotation.setAccessible(true);
            try {
                Object id = fieldByAnnotation.get(o);
                request = request.id(id.toString());
            } catch (IllegalAccessException ignored) {
            }
        }

        request.source(JSONUtil.toJsonStr(o), XContentType.JSON);
        return restHighLevelClient.index(request, RequestOptions.DEFAULT);
    }

    /**
     * ??????id??????
     *
     * @param indexName ????????????
     * @param id ID
     * @return String
     */
    public String queryById(String indexName, String id) throws IOException {
        GetRequest getRequest = new GetRequest(indexName, id);
        // getRequest.fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE);

        GetResponse getResponse = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
        return getResponse.getSourceAsString();
    }


    /**
     * ??????????????????json?????????
     *
     * @param indexName ????????????
     * @param searchSourceBuilder searchSourceBuilder
     * @return String
     * @throws IOException
     */
    public String search(String indexName, SearchSourceBuilder searchSourceBuilder) throws IOException {
        SearchRequest searchRequest = new SearchRequest(indexName);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(1L));


        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        String scrollId = searchResponse.getScrollId();
        SearchHits hits = searchResponse.getHits();
        JSONArray jsonArray = JSONUtil.createArray();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            JSONObject jsonObject = JSONUtil.parseObj(sourceAsString);
            jsonArray.add(jsonObject);
        }
        return jsonArray.toString();
    }

    /**
     * ??????????????????json?????????
     * ????????????sort??????
     * @param indexName ????????????
     * @param searchSourceBuilder searchSourceBuilder
     * @return String
     * @throws IOException
     */
    public String searchAndSort(String indexName, SearchSourceBuilder searchSourceBuilder) throws IOException {
        SearchRequest searchRequest = new SearchRequest(indexName);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        JSONArray jsonArray = JSONUtil.createArray();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            JSONObject jsonObject = JSONUtil.parseObj(sourceAsString);
            jsonObject.putOnce("sort", hit.getSortValues());
            jsonArray.add(jsonObject);
        }
        return jsonArray.toString();
    }



    /**
     * ??????????????????json?????????
     *
     * @param indexName ????????????
     * @param searchSourceBuilder searchSourceBuilder
     * @return String
     * @throws IOException
     */
    public Aggregations searchAggs(String indexName, SearchSourceBuilder searchSourceBuilder) throws IOException {
        SearchRequest searchRequest = new SearchRequest(indexName);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        return searchResponse.getAggregations();
    }


    /**
     * ???????????????????????????
     *
     * @param searchSourceBuilder ???????????????
     * @param s clazz
     * @param <T> ??????
     * @return List<T>
     * @throws IOException
     */
    public <T> List<T> search(SearchSourceBuilder searchSourceBuilder, Class<T> s) throws Exception {
        EsDocument declaredAnnotation = s.getDeclaredAnnotation(EsDocument.class);
        if (declaredAnnotation == null) {
            throw new Exception(String.format("class name: %s can not find Annotation [EsDocument], please check", s.getName()));
        }
        String indexName = declaredAnnotation.indexName();
        SearchRequest searchRequest = new SearchRequest(indexName);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(1L));
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        // //????????????????????????
        // HighlightBuilder highlightBuilder = new HighlightBuilder(); //?????????????????????
        // highlightBuilder.field(title);      //??????????????????
        // highlightBuilder.field(content);    //??????????????????
        // highlightBuilder.requireFieldMatch(false);     //???????????????????????????,????????????false
        // highlightBuilder.preTags("<span style=\"color:red\">");   //????????????
        // highlightBuilder.postTags("</span>");
        //
        // //???????????????,?????????????????????????????????????????????????????????,????????????,???????????????????????????,?????????????????????
        // highlightBuilder.fragmentSize(800000); //?????????????????????
        // highlightBuilder.numOfFragments(0); //????????????????????????????????????

        String scrollId = searchResponse.getScrollId();
        SearchHits hits = searchResponse.getHits();

        JSONArray jsonArray = JSONUtil.createArray();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            JSONObject jsonObject = JSONUtil.parseObj(sourceAsString);
            jsonArray.add(jsonObject);
        }
        return jsonArray.toList(s);
    }

    /**
     * ??????????????????
     * ???????????? ?????????
     * ??????????????? ?????????
     *
     * @param list list??????
     * @param izAsync ????????????
     * @return boolean
     */
    public <T> boolean batchSaveOrUpdate(List<T> list, boolean izAsync) throws Exception {
        if(CollUtil.isEmpty(list)){
            return false;
        }
        Object o1 = list.get(0);
        EsDocument declaredAnnotation = o1.getClass().getDeclaredAnnotation(EsDocument.class);
        if (declaredAnnotation == null) {
            throw new Exception(
                    String.format("class name: %s can not find Annotation [@EsDocument], please check",
                            o1.getClass().getName()));
        }
        String indexName = declaredAnnotation.indexName();

        BulkRequest request = new BulkRequest(indexName);
        for (Object o : list) {

            JSONObject tempJson = JSONUtil.createObj();

            // ??????????????????
            Field[] fields = ReflectUtil.getFields(o.getClass());
            for (Field field : fields) {
                EsField fieldAnnotation =
                        field.getAnnotation(EsField.class);
                if(fieldAnnotation == null){
                    continue;
                }
                String fieldName = ObjectUtil.defaultIfBlank(fieldAnnotation.name(), field.getName());
                tempJson.putOnce(fieldName, ReflectUtil.getFieldValue(o, field));
            }

            String jsonStr = tempJson.toString();
            IndexRequest indexReq = new IndexRequest().source(jsonStr, XContentType.JSON);

            Field fieldByAnnotation = getFieldByAnnotation(o, EsDataId.class);
            if (fieldByAnnotation != null) {
                fieldByAnnotation.setAccessible(true);
                try {
                    Object id = fieldByAnnotation.get(o);
                    indexReq = indexReq.id(id.toString());
                } catch (IllegalAccessException ignored) {
                }
            }
            request.add(indexReq);
        }
        if (izAsync) {
            BulkResponse bulkResponse = restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
            return outResult(bulkResponse);
        } else {
            restHighLevelClient.bulkAsync(request, RequestOptions.DEFAULT, new ActionListener<BulkResponse>() {
                @Override
                public void onResponse(BulkResponse bulkResponse) {
                    outResult(bulkResponse);
                }

                @Override
                public void onFailure(Exception e) {
                }
            });
        }
        return true;
    }

    /**
     * ????????????
     *
     * @param indexName??? ????????????
     * @param docId???     ??????id
     */
    public boolean deleteDoc(String indexName, String docId) throws IOException {
        DeleteRequest request = new DeleteRequest(indexName, docId);
        DeleteResponse deleteResponse = restHighLevelClient.delete(request, RequestOptions.DEFAULT);
        // ??????response
        String index = deleteResponse.getIndex();
        String id = deleteResponse.getId();
        long version = deleteResponse.getVersion();
        ReplicationResponse.ShardInfo shardInfo = deleteResponse.getShardInfo();
        if (shardInfo.getFailed() > 0) {
            for (ReplicationResponse.ShardInfo.Failure failure :
                    shardInfo.getFailures()) {
                String reason = failure.reason();
            }
        }
        return true;
    }

    /**
     * ??????json??????????????????
     *
     * @param indexName ????????????
     * @param docId ??????ID
     * @param o ??????
     * @return boolean
     * @throws IOException
     */
    public boolean updateDoc(String indexName, String docId, Object o) throws IOException {
        UpdateRequest request = new UpdateRequest(indexName, docId);
        request.doc(JSONUtil.toJsonStr(o), XContentType.JSON);
        UpdateResponse updateResponse = restHighLevelClient.update(request, RequestOptions.DEFAULT);
        String index = updateResponse.getIndex();
        String id = updateResponse.getId();
        long version = updateResponse.getVersion();
        if (updateResponse.getResult() == DocWriteResponse.Result.CREATED) {
            return true;
        } else if (updateResponse.getResult() == DocWriteResponse.Result.UPDATED) {
            return true;
        } else if (updateResponse.getResult() == DocWriteResponse.Result.DELETED) {
        } else if (updateResponse.getResult() == DocWriteResponse.Result.NOOP) {
        }
        return false;
    }

    /**
     * ??????Map??????????????????
     *
     * @param indexName ????????????
     * @param docId ??????ID
     * @param map ????????????Map
     * @return boolean
     * @throws IOException
     */
    public boolean updateDoc(String indexName, String docId, Map<String, Object> map) throws IOException {
        UpdateRequest request = new UpdateRequest(indexName, docId);
        request.doc(map);
        UpdateResponse updateResponse = restHighLevelClient.update(request, RequestOptions.DEFAULT);
        String index = updateResponse.getIndex();
        String id = updateResponse.getId();
        long version = updateResponse.getVersion();
        if (updateResponse.getResult() == DocWriteResponse.Result.CREATED) {
            return true;
        } else if (updateResponse.getResult() == DocWriteResponse.Result.UPDATED) {
            return true;
        } else if (updateResponse.getResult() == DocWriteResponse.Result.DELETED) {
        } else if (updateResponse.getResult() == DocWriteResponse.Result.NOOP) {

        }
        return false;
    }

    public XContentBuilder generateBuilder(Class<?> clazz) throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        builder.startObject("properties");
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field f : declaredFields) {
            if (f.isAnnotationPresent(EsField.class)) {
                // ????????????
                EsField declaredAnnotation =
                        f.getDeclaredAnnotation(EsField.class);

                // ?????????????????????
                /**
                 * {
                 *   "mappings": {
                 *     "properties": {
                 *       "region": {
                 *         "type": "keyword"
                 *       },
                 *       "manager": {
                 *         "properties": {
                 *           "age":  { "type": "integer" },
                 *           "name": {
                 *             "properties": {
                 *               "first": { "type": "text" },
                 *               "last":  { "type": "text" }
                 *             }
                 *           }
                 *         }
                 *       }
                 *     }
                 *   }
                 * }
                 */
                if (declaredAnnotation.type() == FieldType.Object) {
                    // ????????????????????????-- Action
                    Class<?> type = f.getType();
                    Field[] df2 = type.getDeclaredFields();
                    builder.startObject(f.getName());
                    builder.startObject("properties");
                    // ?????????????????????????????????
                    for (Field f2 : df2) {
                        if (f2.isAnnotationPresent(EsField.class)) {
                            // ????????????com.think.bboss.es.base.annotation.EsField
                            EsField declaredAnnotation2 =
                                    f2.getDeclaredAnnotation(EsField.class);

                            String fieldName = ObjectUtil.defaultIfBlank(declaredAnnotation2.name(), f2.getName());

                            builder.startObject(fieldName);
                            builder.field("type", declaredAnnotation2.type().toString().toLowerCase());
                            // keyword???????????????
                            if (declaredAnnotation2.type() == FieldType.Text) {
                                // ????????????????????????????????? ??????????????????????????? ??????????????????
                                if(null != declaredAnnotation.analyzer()){
                                    builder.field("analyzer", declaredAnnotation.analyzer().getType());
                                }

                                /*"fields" : {
                                    "keyword" : {
                                        "type" : "keyword",
                                                "ignore_above" : 256
                                    }
                                }*/

                                builder.startObject("fields");
                                    builder.startObject("keyword");
                                        builder.field("type", "keyword");
                                        builder.field("ignore_above", 256);
                                    builder.endObject();
                                builder.endObject();
                            }
                            if (declaredAnnotation2.type() == FieldType.Date) {
                                String formatStr = ObjectUtil
                                        .defaultIfEmpty(declaredAnnotation.datePattern(), "yyyy-MM-dd HH:mm:ss");
                                builder.field("format", formatStr);
                            }
                            builder.endObject();
                        }
                    }
                    builder.endObject();
                    builder.endObject();

                } else {
                    String fieldName = ObjectUtil.defaultIfBlank(declaredAnnotation.name(), f.getName());
                    builder.startObject(fieldName);
                    builder.field("type", declaredAnnotation.type().toString().toLowerCase());
                    // keyword???????????????
                    if (declaredAnnotation.type() == FieldType.Text) {
                        // ????????????????????????????????? ??????????????????????????? ??????????????????
                        if(null != declaredAnnotation.analyzer()){
                            builder.field("analyzer", declaredAnnotation.analyzer().getType());
                        }

                        /*"fields" : {
                            "keyword" : {
                                "type" : "keyword",
                                        "ignore_above" : 256
                            }
                        }*/

                        builder.startObject("fields");
                            builder.startObject("keyword");
                                builder.field("type", "keyword");
                                builder.field("ignore_above", 256);
                            builder.endObject();
                        builder.endObject();
                    }
                    if (declaredAnnotation.type() == FieldType.Date) {
                        String formatStr = ObjectUtil
                                .defaultIfEmpty(declaredAnnotation.datePattern(), "yyyy-MM-dd HH:mm:ss");
                        builder.field("format", formatStr);
                    }
                    builder.endObject();
                }
            }
        }
        // ??????property
        builder.endObject();
        builder.endObject();
        return builder;
    }

    public static Field getFieldByAnnotation(Object o, Class annotationClass) {
        Field[] declaredFields = o.getClass().getDeclaredFields();
        if (declaredFields.length > 0) {
            for (Field f : declaredFields) {
                if (f.isAnnotationPresent(annotationClass)) {
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * getLowLevelClient
     *
     * @return
     */
    public RestClient getLowLevelClient() {
        return restHighLevelClient.getLowLevelClient();
    }

    /**
     * ??????????????? ????????????
     * map????????? JSONObject.parseObject(JSONObject.toJSONString(map), Content.class)
     *
     * @param searchResponse ????????????
     * @param highlightField ??????
     */
    public List<Map<String, Object>> setSearchResponse(SearchResponse searchResponse, String highlightField) {
        //????????????
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            Map<String, HighlightField> high = hit.getHighlightFields();
            HighlightField title = high.get(highlightField);

            hit.getSourceAsMap().put("id", hit.getId());

            //???????????????
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            //??????????????????,????????????????????????????????????
            if (title != null) {
                Text[] texts = title.fragments();
                StringBuilder nTitle = new StringBuilder();
                for (Text text : texts) {
                    nTitle.append(text);
                }
                //??????
                sourceAsMap.put(highlightField, nTitle.toString());
            }
            list.add(sourceAsMap);
        }
        return list;
    }

    /**
     * ???????????????
     *
     * @param index          ????????????
     * @param query          ????????????
     * @param size           ??????????????????
     * @param from           ??????????????????
     * @param fields         ???????????????????????????????????????????????????????????????
     * @param sortField      ????????????
     * @param highlightField ????????????
     * @return List<Map<String, Object>>
     */
    public List<Map<String, Object>> searchListData(String index,
                                                    SearchSourceBuilder query,
                                                    Integer size,
                                                    Integer from,
                                                    String fields,
                                                    String sortField,
                                                    String highlightField) throws IOException {
        SearchRequest request = new SearchRequest(index);
        if (StrUtil.isNotEmpty(fields)) {
            //???????????????????????????????????????????????????????????????????????????
            query.fetchSource(new FetchSourceContext(true, fields.split(","), Strings.EMPTY_ARRAY));
        }
        from = from <= 0 ? 0 : from * size;
        //???????????????????????????????????????????????????from??????????????????0
        query.from(from);
        query.size(size);
        if (StrUtil.isNotEmpty(sortField)) {
            //???????????????????????????proposal_no???text?????????????????????keyword?????????????????????.keyword
            query.sort(sortField + ".keyword", SortOrder.ASC);
        }
        //??????
        HighlightBuilder highlight = new HighlightBuilder();
        highlight.field(highlightField);
        //??????????????????
        highlight.requireFieldMatch(false);
        highlight.preTags("<span style='color:red'>");
        highlight.postTags("</span>");
        query.highlighter(highlight);
        //???????????????????????????????????????????????????
        //builder.fetchSource(false);
        request.source(query);
        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        if (response.status().getStatus() == SUCCESS_STATE) {
            // ????????????
            return setSearchResponse(response, highlightField);
        }
        return null;
    }

    private boolean outResult(BulkResponse bulkResponse) {
        for (BulkItemResponse bulkItemResponse : bulkResponse) {
            DocWriteResponse itemResponse = bulkItemResponse.getResponse();
            IndexResponse indexResponse = (IndexResponse) itemResponse;
            if (bulkItemResponse.isFailed()) {
                return false;
            }
        }
        return true;
    }
}
