package org.opsli.springbootespage;

import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.opsli.springbootespage.es.base.components.ElasticsearchComponent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;

/**
 * Es 分页
 * @author Parker
 * @date 2022/3/22 18:26
 */
@CrossOrigin
@AllArgsConstructor
@RestController
public class PageController {

	private ElasticsearchComponent elasticsearchComponent;

	/**
	 * 查询 注意 前端传入的 参数是 searchAfter ☆☆☆ 且不是必填
	 * @param searchAfterStr 参数字符串
	 * @return ResponseEntity<?>
	 */
	@GetMapping("/es/page")
	public ResponseEntity<?> page(
			@RequestParam(name="searchAfter", required = false) String searchAfterStr)
			throws IOException {
		// 查询语句
		// TODO 自己写的话 不要把DSL直接写在 Controller里哈, 自己可以写一个解析器
		String query = "{\n" +
						"  \"size\": 25,\n" +
						"  \"from\": 0, \n" +
						"  \"query\": {\n" +
						"    \"match_all\": {}\n" +
						"  },\n" +
						"  \"sort\": [\n" +
						"    {\n" +
						"      \"time\": {\n" +
						"        \"order\": \"desc\"\n" +
						"      }\n" +
						"    },\n" +
						"    {\n" +
						"      \"id\": {\n" +
						"        \"order\": \"asc\"\n" +
						"      }\n" +
						"    }\n" +
						"  ]\n";
		if(StringUtils.isNotBlank(searchAfterStr)){
				String[] searchAfter = searchAfterStr.split(",");
				query +="  ,\"search_after\": " + JSONUtil.toJsonStr(searchAfter);
		}
		query += "}";

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		SearchModule searchModule = new SearchModule(Settings.EMPTY, false, Collections.emptyList());
		try (XContentParser parser = XContentFactory.xContent(XContentType.JSON).createParser(new NamedXContentRegistry(searchModule
				.getNamedXContents()), DeprecationHandler.THROW_UNSUPPORTED_OPERATION, query)) {
			searchSourceBuilder.parseXContent(parser);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// 返回Json
		String jsonStr = elasticsearchComponent.searchAndSort("earthquakes", searchSourceBuilder);
		return ResponseEntity.ok(JSONUtil.parseArray(jsonStr));
	}

}
