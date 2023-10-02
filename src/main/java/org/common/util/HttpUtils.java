package org.common.util;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);
	
	public static final String HTTPS = "https";
	public static final String HTTP = "http";
	public static final String SSL_PROTOCOL = "SSL";
	public static final String TLS_PROTOCOL = "TLS";
	public static final int HTTPS_DEFAULT_PORT = 443;
	public static final String GZIP_ENCODING = "gzip";
	public static final String IP_REGEX = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
	public static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";
	private static final List<Integer> AVAILABLE_RETRY_RESPONSE_CODE_LIST = Arrays.asList(
			// 2xx
			HttpStatus.SC_NO_CONTENT, 			// 204
			// 4xx
			HttpStatus.SC_BAD_GATEWAY, 			// 401
			HttpStatus.SC_REQUEST_TIMEOUT, 		// 408
			// 5xx
			HttpStatus.SC_BAD_GATEWAY,			// 502
			HttpStatus.SC_SERVICE_UNAVAILABLE,	// 503
			HttpStatus.SC_GATEWAY_TIMEOUT		// 504
	);
	
	private static final RequestConfig DEFAULT_REQUEST_CONFIG = RequestConfig.custom()
			.setSocketTimeout(15 * 1000)
			.setConnectTimeout(15 * 1000)
			.setConnectionRequestTimeout(30 * 1000)
			.build();
	
	private static final ServiceUnavailableRetryStrategy DEFAULT_SERVICE_RETRY_STRATEGY = new ServiceUnavailableRetryStrategy() {
        /**
         * retry邏輯
         */
		@Override
		public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
			return executionCount < 3 && AVAILABLE_RETRY_RESPONSE_CODE_LIST.contains(response.getStatusLine().getStatusCode());
		}
        /**
         * retry間隔時間
         */
        @Override
        public long getRetryInterval() {
            return 1 * 1000L;
        }
    };
	
	private HttpUtils() {
	}
	
	/**
	 * 創建一個忽略SSL憑證驗證的ConnectionManager
	 * 
	 * @return PoolingHttpClientConnectionManager
	 */
	public static PoolingHttpClientConnectionManager createIgnoreSSLConnectionManager() {
		try {
			SSLConnectionSocketFactory ignoreSslSocketFactory = new SSLConnectionSocketFactory(SSLContexts.custom().loadTrustMaterial((chain, authType) -> true).build(), NoopHostnameVerifier.INSTANCE);
			return new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
					.register(HTTP, PlainConnectionSocketFactory.getSocketFactory())
					.register(HTTPS, ignoreSslSocketFactory).build());
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return new PoolingHttpClientConnectionManager();
	}
	
	/**
	 * 檢是來源URL是否為自訂Domain名稱
	 * 
	 * @param url 來源URL
	 * @return 否為自訂Domain名稱, boolean型別
	 */
	public static boolean isDomainUri(String url) {
		return StringTools.findFirstMatchSequence(url, IP_REGEX).isEmpty();
	}
	
	/**
	 * 取得使用者真實IP位置
	 * 
	 * @param request 使用者請求
	 * @return 使用者真實IP位置, String型別
	 */
	public static String getIpAddr(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		final String unknown = "unknown";
		if (!ClassUtils.isValid(ip) || unknown.equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (!ClassUtils.isValid(ip) || unknown.equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (!ClassUtils.isValid(ip) || unknown.equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}
	
	/**
	 * 	取得請求的網址根目錄
	 * 
	 * @param request 使用者請求
	 * @return 網站根目錄
	 */
	public static String getBaseUrl(HttpServletRequest request) {
		int serverPort = request.getServerPort();
		String schema = serverPort == HTTPS_DEFAULT_PORT ? HTTPS : "http";
		String portPart = schema.equals(HTTPS) ? "" : ":" + serverPort;
		return String.format("%s://%s%s%s", 
				schema,
				request.getServerName(),
				portPart,
				request.getContextPath());
	}
	
	/**
	 * 創建一個HttpClient實例
	 * 
	 * @return HttpClient實例
	 */
	public static HttpClient createHttpClient() {
		return HttpClients.custom()
				.setDefaultRequestConfig(DEFAULT_REQUEST_CONFIG)
				.setServiceUnavailableRetryStrategy(DEFAULT_SERVICE_RETRY_STRATEGY)
				.setRetryHandler((e, executionCount, context) -> executionCount < 3)
				.setConnectionManager(HttpUtils.createIgnoreSSLConnectionManager())
				.build();
	}
	
	public static HttpClient createHttpClient(int socketTime,int connectTime,int connectionRequestTime) {
		return HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom()
						.setSocketTimeout(socketTime * 1000)
						.setConnectTimeout(connectTime * 1000)
						.setConnectionRequestTimeout(connectionRequestTime * 1000)
						.build())
				.setServiceUnavailableRetryStrategy(DEFAULT_SERVICE_RETRY_STRATEGY)
				.setRetryHandler((e, executionCount, context) -> executionCount < 3)
				.setConnectionManager(HttpUtils.createIgnoreSSLConnectionManager())
				.build();
	}
	
	/**
	 * 獲取指定URI的Http狀態碼
	 * 
	 * @see org.apache.http.HttpStatus
	 * @see org.apache.http.impl.client.CloseableHttpClient
	 * @see org.apache.http.client.methods.CloseableHttpResponse
	 * 
	 * @param uri 指定URI, String型別
	 * @param headers 指定Headers, Map型別
	 * 
	 * @return Http狀態碼, int型別
	 */
	public static int getHttpStatusCode(String uri, Map<String, String> headers) {
		try (CloseableHttpClient httpClient = (CloseableHttpClient) HttpUtils.createHttpClient();
				CloseableHttpResponse response = httpClient.execute(HttpUtils.toHttpRequest(uri, headers))) {
			return response.getStatusLine().getStatusCode();
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return HttpStatus.SC_BAD_GATEWAY;
	}
	
	/**
	 * [方法多載]<br>
	 * 支援無須傳入額外headers; 獲取指定URI的Http狀態碼
	 * 
	 * @see org.apache.http.HttpStatus
	 * @see org.apache.http.impl.client.CloseableHttpClient
	 * @see org.apache.http.client.methods.CloseableHttpResponse
	 * 
	 * @param uri 指定URI, String型別
	 * 
	 * @return Http狀態碼, int型別
	 */
	public static int getHttpStatusCode(String uri) {
		return getHttpStatusCode(uri, null);
	}
	
	/**
	 * 取得指定uri於header中記錄的附件名稱; 若無定義Content-Disposition屬性, 則嘗試從uri中萃取檔名
	 * 
	 * @param uri 指定uri, String型別
	 * @param headers 自訂標頭, Map型別
	 * @return 附件名稱, String型別
	 */
	public static String getAttachmentName(String uri, Map<String, String> headers) {
		String attachmentName = null;
		try (CloseableHttpClient httpClient = (CloseableHttpClient) HttpUtils.createHttpClient();
				CloseableHttpResponse response = httpClient.execute(HttpUtils.toHttpRequest(uri, headers))) {
			
			Header header = response.getFirstHeader(CONTENT_DISPOSITION_HEADER);
			String fileNameInUri = uri.substring(uri.lastIndexOf('/') + 1);
			attachmentName = Objects.isNull(header) && fileNameInUri.matches("[\\w\\-]+(\\.\\w{2,5}){1,2}") ? fileNameInUri : null;
			if (Objects.nonNull(header)) {
				attachmentName = Stream.of(header.getElements())
						.filter(e -> e.getName().equalsIgnoreCase("attachment"))
						.map(e -> e.getParameterByName("filename"))
						.filter(Objects::nonNull)
						.map(NameValuePair::getValue)
						.findFirst().orElse(null);
			}
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return attachmentName;
	}
	
	public static String getAttachmentName(String uri) {
		return getAttachmentName(uri, new HashMap<>());
	}
	
	public static HttpRequestBase toHttpRequest(String targetUrl, String methodType, Map<String, String> headers, List<NameValuePair> params) {
		HttpRequestBase httpRequest = null;
		switch (methodType.toUpperCase()) {
		case "GET":
			httpRequest = new HttpGet(targetUrl);
			break;
		case "POST":
			HttpPost httpPost = new HttpPost(targetUrl);
			if (!params.isEmpty()) httpPost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
			httpRequest = httpPost;
			break;
		case "PUT":
			httpRequest = new HttpPut(targetUrl);
			break;
		case "DELETE":
			httpRequest = new HttpDelete(targetUrl);
			break;
		case "HEAD":
			httpRequest = new HttpHead(targetUrl);
			break;
		case "PATCH":
			httpRequest = new HttpPatch(targetUrl);
			break;
		default:
			throw new IllegalArgumentException("Invalid HTTP method: " + methodType);
		}
		// Check extra header exists or not, if exists then add in header, else skip
		if (Objects.nonNull(headers)) headers.forEach(httpRequest::addHeader);
		
		return httpRequest;
	}
	
	public static HttpRequestBase toHttpRequest(String targetUrl, String methodType, List<NameValuePair> params) {
		return toHttpRequest(targetUrl, methodType, null, params);
	}
	
	public static HttpRequestBase toHttpRequest(String targetUrl, String methodType, Map<String, String> headers, NameValuePair... params) {
		return toHttpRequest(targetUrl, methodType, headers, Arrays.asList(params));
	}
	
	public static HttpRequestBase toHttpRequest(String targetUrl, String methodType, NameValuePair... params) {
		return toHttpRequest(targetUrl, methodType, null, Arrays.asList(params));
	}
	
	public static HttpRequestBase toHttpRequest(String targetUrl, Map<String, String> headers) {
		return toHttpRequest(targetUrl, "GET", headers);
	}

	public static HttpRequestBase toHttpRequest(String targetUrl) {
		return toHttpRequest(targetUrl, null);
	}

	public static boolean isGzEncoding(HttpResponse response) {
		HttpEntity entity = response.getEntity();
		Header contentEncodingHeader = entity.getContentEncoding();
		Header contentDispositionHeader = response.getFirstHeader("Content-Disposition");
		return Objects.nonNull(contentEncodingHeader) && contentEncodingHeader.getValue().equals(HttpUtils.GZIP_ENCODING) ||
			Objects.nonNull(contentDispositionHeader) && contentDispositionHeader.getValue().endsWith(".gz");
	}

	public static String passRequestParam(HttpServletRequest request, String baseUrl, boolean econde) {
		try {
			String queryStr = request.getQueryString().replaceAll("[\r\n]", "");
			String encodeQueryParam = econde ? URLEncoder.encode(queryStr, StandardCharsets.UTF_8.name()) : queryStr;

			return String.format("%s?%s", baseUrl, encodeQueryParam);
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return baseUrl;
	}
	
	public static String passRequestParam(HttpServletRequest request, String baseUrl) {
		return passRequestParam(request, baseUrl, true);
	}

	public static String setRequestParam(String baseUrl, Map<String, Object> params) {
		baseUrl += params.isEmpty() ? "" : "?";
		return params.entrySet().stream().map(entry -> {
			String paramPair = "";
			try {
				paramPair = String.format("%s=%s",
						URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()),
						URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8.name()));
			} catch (UnsupportedEncodingException e) {
				LOGGER.error(e.getMessage(), e);
			}
			return paramPair;
		}).filter(ClassUtils::isValid)
		.collect(Collectors.joining("&", baseUrl, ""));
	}
	
	public static String setRequestParam(String baseUrl, List<NameValuePair> params) {
		baseUrl += params.isEmpty() ? "" : "?";
		return params.stream().map(param -> {
			String paramPair = "";
			try {
				paramPair = String.format("%s=%s",
						URLEncoder.encode(param.getName(), StandardCharsets.UTF_8.name()),
						URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8.name()));
			} catch (UnsupportedEncodingException e) {
				LOGGER.error(e.getMessage(), e);
			}
			return paramPair;
		}).filter(ClassUtils::isValid)
		.collect(Collectors.joining("&", baseUrl + "?", ""));
	}
	
	public static Map<String, String> customHeaderForPTX(String appId, String appKey) {
		String xdate = DateUtils.getServerTime();
		String signDate = "x-date: " + xdate;
		String signature = CryptUtils.encryptWithHMAC(signDate, appKey);
		String sAuth = String.format("hmac username=\"%s\", algorithm=\"hmac-sha1\", headers=\"x-date\", signature=\"%s\"", appId, signature);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.AUTHORIZATION, sAuth);
		headers.put(HttpHeaders.ACCEPT_ENCODING, GZIP_ENCODING);
		headers.put("x-date", xdate);
		
		return headers;
	}
	
	public static Map<String, String> customHeaderForPTX() {
		String appId = "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF";
		String appKey = "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFFI";
		
		return customHeaderForPTX(appId, appKey);
	}

	public static Map<String, String> customHeaderFoTDX(String token) {
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token));
		return headers;
	}
}
