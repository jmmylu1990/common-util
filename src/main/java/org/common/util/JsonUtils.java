package org.common.util;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.common.util.jackson.StringTrimmerModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * @author Van
 * @version 1.0
 */
public class JsonUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtils.class);
	
	private static ObjectMapper objectMapper;
	
	private JsonUtils() {
	}

	/**
	 * 初始化Json序列化/反序列化轉化器
	 * 
	 * @return Json物件轉換器, ObjectMapper型別
	 */
	private static ObjectMapper initialMapper() {
		objectMapper = new ObjectMapper();
		// Set the field name case insensitive
		// Set the key or value can use single quote or no quote
		// Set the array value can only single value
		objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
				.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.enable(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME)
				.enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
				.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
				.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
				.registerModule(new StringTrimmerModule());

		return objectMapper;
	}

	/**
	 * 取得唯一Json序列化/反序列化轉化器
	 * 
	 * @return Json物件轉換器, ObjectMapper型別
	 */
	public static ObjectMapper getMapper() {
		return (objectMapper == null) ? initialMapper() : objectMapper;
	}

	/**
	 * 將json字串反序列化為對應的bean物件
	 * 
	 * @param jsonContent
	 *            json字串, String型別
	 * @param beanClass
	 *            反序列化後的java bean類別, Class型別
	 * @return 反序列化後的Java bean物件, 使用者指定型別
	 */
	public static <E> E toBean(String jsonContent, Class<E> beanClass) {
		try {
			return getMapper().readValue(jsonContent, beanClass);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return null;
	}

	/**
	 * 將json字串反序列化為對應的bean集合物件
	 *
	 * @see List
	 * @see ObjectMapper
	 * @see CollectionType
	 * @see TypeFactory
	 * @see DeserializationFeature
	 * @param jsonContent
	 *            json字串, String型別
	 * @param beanClass
	 *            java bean類別, Class型別
	 * @return bean list物件, List型別
	 */
	public static <E> List<E> toBeanList(String jsonContent, Class<E> beanClass) {
		try {
			CollectionType typeReference = TypeFactory.defaultInstance().constructCollectionType(List.class, beanClass);

			return getMapper().readValue(jsonContent, typeReference);
		} catch (Exception e) {
			LOGGER.error("Error content: {}", jsonContent);
			LOGGER.error(e.getMessage(), e);
		}
		return new ArrayList<>();
	}

	/**
	 * 將json字串反序列化為對應的bean集合物件
	 *
	 * @see Set
	 * @see ObjectMapper
	 * @see CollectionType
	 * @see TypeFactory
	 * @see DeserializationFeature
	 * @param jsonContent
	 *            json字串, String型別
	 * @param beanClass
	 *            java bean類別, Class型別
	 * @return bean set物件, Set型別
	 */
	public static <E> Set<E> toBeanSet(String jsonContent, Class<E> beanClass) {
		try {
			CollectionType typeReference = TypeFactory.defaultInstance().constructCollectionType(Set.class, beanClass);

			return getMapper().readValue(jsonContent, typeReference);
		} catch (Exception e) {
			LOGGER.error("Error content: {}", jsonContent);
			LOGGER.error(e.getMessage(), e);
		}

		return new HashSet<>();
	}

	/**
	 * 採用不同的http方法讀取來源網址中Json物件, 並轉換成字串回傳
	 *
	 * @see java.net.URL
	 * @see java.net.HttpURLConnection
	 * @see BufferedReader
	 * @param url 來源網址字串, String型別
	 * @param methodType http存取方法形式, String型別
	 * @param headers 指定Headers, Map型別
	 * 
	 * @return Json字串, String型別
	 * @throws GeneralSecurityException 
	 * @throws Exception
	 */
	public static String toJsonString(String url, String methodType, Map<String, String> headers) throws Exception {
		headers = Optional.ofNullable(headers).orElseGet(HashMap::new);
		if (!headers.containsKey(HttpHeaders.USER_AGENT)) {
			headers.put(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Linux; Android 4.2.1; Nexus 7 Build/JOP40D) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166  Safari/535.19");
		}
		try (CloseableHttpClient httpClient = (CloseableHttpClient) HttpUtils.createHttpClient();
				CloseableHttpResponse response = httpClient.execute(HttpUtils.toHttpRequest(url, methodType, headers));
				InputStream content = response.getEntity().getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(
					HttpUtils.isGzEncoding(response) ? new GZIPInputStream(content) : content, StandardCharsets.UTF_8), 8192
				)
		) {
			return reader.lines().collect(Collectors.joining());
		}
	}

	/**
	 * [方法多載]<br>
	 * 採用預設的GET方法去讀取來源網址中Json物件, 並轉換成字串回傳
	 *
	 * @see java.net.URL
	 * @see java.net.HttpURLConnection
	 * @see BufferedReader
	 * @param url 來源網址字串, String型別
	 * @param headers 指定Headers, Map型別
	 * 
	 * @return Json字串, String型別
	 * @throws Exception
	 */
	public static String toJsonString(String url, Map<String, String> headers) throws Exception {
		return toJsonString(url, "GET", headers);
	}
	
	/**
	 * [方法多載]<br>
	 * 採用預設的GET方法去讀取來源網址中Json物件, 並轉換成字串回傳
	 *
	 * @see java.net.URL
	 * @see java.net.HttpURLConnection
	 * @see BufferedReader
	 * @param url
	 *            來源網址字串, String型別
	 * @return Json字串, String型別
	 * @throws Exception
	 */
	public static String toJsonString(String url) throws Exception {
		return toJsonString(url, "GET", new HashMap<>());
	}

	/**
	 * [方法多載]<br>
	 * 讀取來指定路徑中檔案中所含的Json字符, 並轉換成字串回傳
	 *
	 * @see BufferedReader
	 * @param file
	 *            來源檔案, File型別
	 * @return Json字串, String型別
	 */
	public static String toJsonString(File file) {
		return FileOperationUtils.extractContent(file);
	}
	
	/**
	 * 採用不同的http方法讀取來源網址中Json物件, 並轉換成字串回傳
	 *
	 * @see java.net.URL
	 * @see java.net.HttpURLConnection
	 * @see BufferedReader
	 * @param url 來源網址字串, String型別
	 * 
	 * @return Json字串, String型別
	 * @throws GeneralSecurityException 
	 * @throws Exception
	 */
	public static String postJsonString(String url, Object body) throws Exception {
		HttpPost httpPost = (HttpPost) HttpUtils.toHttpRequest(url, HttpMethod.POST.name());
		httpPost.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		httpPost.setEntity(new StringEntity(getMapper().writeValueAsString(body), StandardCharsets.UTF_8));
		try (CloseableHttpClient httpClient = (CloseableHttpClient) HttpUtils.createHttpClient();
				CloseableHttpResponse response = httpClient.execute(httpPost);
				InputStream content = response.getEntity().getContent();
		) {
			return EntityUtils.toString(response.getEntity());
		}
	}

	public static Map<String, Object> toHashMap(String jsonContent) {
		try {
			return objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>(){});
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return null;
	}

	/**
	 * 從傳入的Json字串中取的對應的欄位值
	 *
	 * @see ObjectMapper
	 * @see com.fasterxml.jackson.databind.JsonNode
	 * @param jsonContent
	 *            json字串, String型別
	 * @param fieldName
	 *            欲從Json字串中取得的欄位名稱, String型別
	 * @return 取得的欄位值, String型別
	 */
	public static Object getField(String jsonContent, String fieldName) {
		Object fieldValue = null;
		try {
			ObjectNode node = getMapper().readValue(jsonContent, ObjectNode.class);
			if (node.has(fieldName)) {
				String fieldValueStr = node.get(fieldName).toString().replaceAll("\"", "");
				if (fieldValueStr.matches("\\d+")) {
					fieldValue = Integer.parseInt(fieldValueStr);
				} else if (fieldValueStr.matches("[\\d\\.]+")) {
					fieldValue = Double.parseDouble(fieldValueStr);
				} else if (fieldValueStr.matches(DateUtils.DATE_SIMILAR_REGEX)) {
					fieldValue = DateUtils.parseStrToDate(fieldValueStr);
				} else if (fieldValueStr.startsWith("[") && fieldValueStr.endsWith("]")) {
					fieldValue = node.get(fieldName);
				} else {
					fieldValue = fieldValueStr;
				}
			}
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return fieldValue;
	}
}