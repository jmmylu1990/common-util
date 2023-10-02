package org.common.util;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Van
 * @version 1.0
 */
public class DownloadHelper {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadHelper.class);

	/**
	 * TDCS的Etag資料來源網址
	 */
	private static final String TDCS_ETAG_RESOURCE_ROOT = "http://210.241.131.253/history/TDCS";
	/**
	 * TDCS的VD資料來源網址
	 */
	private static final String TDCS_VD_RESOURCE_FORMAT = "http://210.241.131.253/history/vd";

	private static final String LINK_ELEMENT = "#indexlist > tbody > tr > td.indexcolname > a";
	/**
	 * 預設串流下載的byte數
	 */
	private static int buffer = 8192;
	
	/**
	 * 變更預設串流下載的byte數
	 *
	 * @param bytes
	 *            串流的byte數, int型別
	 */
	public static void setBuffer(int bytes) {
		buffer = bytes;
	}
	
	private DownloadHelper() {
	}

	/**
	 * 下載指定來源網址的檔案, 並以串流輸出與指定路徑
	 *
	 * @see URL
	 * @param urlStr
	 *            來源網址, String型別
	 * @param outputFilePath
	 *            輸出檔案路徑, String型別
	 * @param outputFileName
	 *            輸出檔案名稱, String型別
	 * @return 下載的檔案, File型別
	 * @throws IOException
	 */
	public static File download(String urlStr, String outputFilePath, String outputFileName) throws IOException {
		File outputFile = null;
		try (CloseableHttpClient httpClient = (CloseableHttpClient) HttpUtils.createHttpClient();
			 CloseableHttpResponse response = httpClient.execute(HttpUtils.toHttpRequest(urlStr, HttpUtils.customHeaderForPTX()))) {
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				outputFile = new File(outputFilePath, outputFileName);
				File parentFile = outputFile.getParentFile();
				if (!parentFile.exists()) parentFile.mkdirs();
				try (BufferedInputStream in = new BufferedInputStream(response.getEntity().getContent());
						BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
					byte[] bytes = new byte[buffer];
					int size;
					while ((size = in.read(bytes)) != -1) {
						out.write(bytes, 0, size);
					}
					out.flush();
				}
			} else {
				throw new IOException("The URL is not available so that the connection failed.");
			}
		} catch (MalformedURLException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return outputFile;
	}

	/**
	 * [方法多載]<br>
	 * 支援傳入File型別的參數; 下載指定來源網址的檔案, 並以串流輸出與指定路徑
	 *
	 * @see URL
	 * @param urlStr
	 *            來源網址, String型別
	 * @param outpitFile
	 *            輸出檔案, File型別
	 * @return 下載的檔案, File型別
	 * @throws IOException
	 */
	public static File download(String urlStr, File outpitFile) throws IOException {
		return download(urlStr, outpitFile.getParent(), outpitFile.getName());
	}

	/**
	 * [方法多載]<br>
	 * 下載指定來源網址的檔案, 並以串流輸出與指定路徑<br>
	 * 倘若可以從網址中解析出檔案名稱, 則以此作為檔名; 否則拋出例外
	 *
	 * @param urlStr
	 *            來源網址, String型別
	 * @param destination
	 *            輸出檔案的位置, String型別
	 * @return 下載的檔案, File型別
	 * @throws IOException
	 *             無法解析檔名的例外
	 */
	public static File download(String urlStr, String destination) throws IOException {
		File destDir = new File(destination);
		if (!destDir.exists()) destDir.mkdirs();
		
		// 若可以從url header中解析出檔案名稱, 則自動產生檔案名稱; 反之則拋出例外
		String attachmentName = HttpUtils.getAttachmentName(urlStr);
		if (ClassUtils.isValid(attachmentName)) {
			return download(urlStr, destination, attachmentName);
		} else {
			throw new IOException("There is no file name! Please try the other method!\nEx: download(String urlStr, String destination, String fileName)");
		}
	}

	public static File downloadVDFile(String dataLink, String destination) {
		int vdType = 0;
		if (dataLink.contains("vd_value_")) {
			vdType = 1;
		} else if (dataLink.contains("vd_value5_")) {
			vdType = 5;
		}

		return downloadVDFile(vdType, destination);
	}

	public static File downloadVDFile(int vdType, String destination) {
		String dateStr = DateUtils.formatDateToStr(DateUtils.SIMPLE_DATE_FORMAT, new Date());
		String vdResourceUrl = String.format("%s/%s/", TDCS_VD_RESOURCE_FORMAT, dateStr);
		try (CloseableHttpClient httpClient = (CloseableHttpClient) HttpUtils.createHttpClient();
				CloseableHttpResponse response = httpClient.execute(HttpUtils.toHttpRequest(vdResourceUrl))) {
			// Get the valid path from TDCS website
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				vdResourceUrl = String.format("%s/%s/", TDCS_VD_RESOURCE_FORMAT, DateUtils.getOtherDateStr(dateStr, -1));
			}
			Document doc = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), vdResourceUrl);
			Elements elements = doc.select(LINK_ELEMENT);
			int size = elements.size();
			for (int i = 1; i < size; i++) {
				Element element = elements.get(i);
				String absUrl = element.absUrl("href");
				if (!ClassUtils.isValid(absUrl)) continue;
				switch (vdType) {
				case 0:
					if (absUrl.contains("vd_info_")) return download(absUrl, destination);
					break;
				case 1:
					if (absUrl.contains("vd_value_")) return download(absUrl, destination);
					break;
				case 5:
					if (absUrl.contains("vd_value5_")) return download(absUrl, destination);
					break;

				default:
					break;
				}
			}
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return null;
	}

	public static File downloadVDFile(String url, Date date, String destination) {
		String fileName = url.substring(url.lastIndexOf('/') + 1);
		String vdUrl = null;
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		String dateStr = DateUtils.formatDateToStr(DateUtils.SIMPLE_DATE_FORMAT, calendar.getTime());
		try {
			if (fileName.equals("vd_info_0000.xml.gz")) return download(url.replace("$date", dateStr), destination);

			int hour = 0;
			int minute = 0;

			if (fileName.contains("value5")) {
				dateStr = DateUtils.formatDateToStr("yyyyMMdd", calendar.getTime());
				hour = calendar.get(Calendar.HOUR_OF_DAY);
				minute = calendar.get(Calendar.MINUTE);
				int muti = minute / 5;
				minute = muti * 5;
			} else {
				calendar.add(Calendar.MINUTE, -5);
				dateStr = DateUtils.formatDateToStr(DateUtils.DASHED_DATE_FORMAT, calendar.getTime()).replaceAll("\\D", "");
				hour = calendar.get(Calendar.HOUR_OF_DAY);
				minute = calendar.get(Calendar.MINUTE);
			}
			vdUrl = url.replace("$date", dateStr).replace("$time", String.format("%02d%02d", hour, minute));

			return download(vdUrl, destination);
		} catch (Exception e) {
			LOGGER.error("{} does not exist", vdUrl, e);
			if (fileName.equals("vd_info_0000.xml.gz")) {
				calendar.add(Calendar.DAY_OF_MONTH, -1);
			} else {
				int offset = (fileName.contains("value5")) ? -5 : -1;
				calendar.add(Calendar.MINUTE, offset);
			}
			if (DateUtils.getTimeDiff(Calendar.MINUTE, calendar.getTime()) > 60) return null;

			return downloadVDFile(url, calendar.getTime(), destination);
		}
	}
	
	/**
	 * 將指定根目錄及日期的TDCS來源網址檔案全數下載至指定路徑<br>
	 * Ex:<br>
	 * rootDit: M03A, date: 20170706, dest: 指定下載目錄<br>
	 * 程式就會自行遍歷裏頭資料並下載至指定下載目錄
	 *
	 * @param rootDir
	 *            TDCS檔案根目錄, String型別
	 * @param dateStr
	 *            TDCS檔案日期目錄, String型別
	 * @param dest
	 *            輸出檔案路徑, String型別
	 * @return 下載的檔案資料目錄, File型別
	 * @throws IOException
	 */
	public static File downloadEtagFilesByDate(String rootDir, String dateStr, String dest) {
		File outputDir = new File(dest);
		try {
			String etagResourceUrl = String.format("%s/%s/%2$s_%s.tar.gz", TDCS_ETAG_RESOURCE_ROOT, rootDir, dateStr.replaceAll("\\D", ""));
			HttpURLConnection conn = (HttpURLConnection) new URL(etagResourceUrl).openConnection();
			if (conn.getResponseCode() == HttpStatus.SC_OK) {
				conn.disconnect();
				return GzUtils.decompressTarGz(download(etagResourceUrl, dest), true);
			} else {
				for (int i = 0; i < 24; i++) {
					downloadEtagFilesByHour(rootDir, DateUtils.parseStrToDate(dateStr), i, dest);
				}
			}
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return FileOperationUtils.isEmpty(outputDir) ? null : outputDir;
	}

	/**
	 * [方法多載]<br>
	 * 將指定根目錄及日期的TDCS來源網址檔案全數下載至指定路徑<br>
	 * Ex:<br>
	 * rootDit: M03A, date: 20170706, dest: 指定下載目錄<br>
	 * 程式就會自行遍歷裏頭資料並下載至指定下載目錄
	 *
	 * @param rootDir
	 *            TDCS檔案根目錄, String型別
	 * @param date
	 *            TDCS檔案日期目錄, Date型別
	 * @param dest
	 *            輸出檔案路徑, String型別
	 * @return 下載的檔案資料目錄, File型別
	 * @throws IOException
	 */
	public static File downloadEtagFilesByDate(String rootDir, Date date, String dest) {
		return downloadEtagFilesByDate(rootDir, DateUtils.formatDateToStr(DateUtils.SIMPLE_DATE_FORMAT, date), dest);
	}

	/**
	 * 將指定根目錄及時間的TDCS來源網址檔案下載至指定路徑<br>
	 * Ex:<br>
	 * rootDit: M04A, date: 2017-04-06 12:21:33, dest: 指定下載目錄<br>
	 * 程式就會自行下載最接近此時間的檔案至指定下載目錄
	 *
	 * @param rootDir
	 *            TDCS檔案根目錄, String型別
	 * @param dateTime
	 *            欲下載的TDCS檔案時間, Date型別
	 * @param dest
	 *            輸出檔案路徑, String型別
	 * @return 下載的檔案, File型別
	 */
	public static File downloadEtagFileByTime(String rootDir, Date dateTime, String dest) {
		return downloadEtagFileByTime(rootDir, DateUtils.formatDateToStr(dateTime), dest);
	}

	/**
	 * [方法多載]<br>
	 * 將指定根目錄及時間的TDCS來源網址檔案下載至指定路徑<br>
	 * Ex:<br>
	 * rootDit: M04A, date: 2017-04-06 12:21:33, dest: 指定下載目錄<br>
	 * 程式就會自行下載最接近此時間的檔案至指定下載目錄
	 *
	 * @param rootDir
	 *            TDCS檔案根目錄, String型別
	 * @param dateTime
	 *            欲下載的TDCS檔案時間, String型別
	 * @param dest
	 *            輸出檔案路徑, String型別
	 * @return 下載的檔案, File型別
	 */
	public static File downloadEtagFileByTime(String rootDir, String dateTime, String dest) {
		String[] dateTimeSegment = dateTime.split("[\\s\\p{Alpha}]");
		String datePart = dateTimeSegment[0].replaceAll("\\D", "");
		String timePart = dateTimeSegment[1];
		String[] timeSegment = timePart.split("\\D");
		int hour = Integer.parseInt(timeSegment[0]);
		rootDir = rootDir.toUpperCase();
		String etagResourceUrl = String.format("%s/%s/%s/%02d/", TDCS_ETAG_RESOURCE_ROOT, rootDir, datePart, hour);
		try (CloseableHttpClient httpClient = (CloseableHttpClient) HttpUtils.createHttpClient();) {
			CloseableHttpResponse response = httpClient.execute(HttpUtils.toHttpRequest(etagResourceUrl));
			// Get the valid path from TDCS website
			while (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK &&
					DateUtils.getTimeDiff(Calendar.HOUR, datePart) < 24) {
				response.close();
				if (hour == -1) {
					datePart = DateUtils.getOtherDateStr(datePart, -1);
					hour += 24;
				}
				etagResourceUrl = String.format("%s/%s/%s/%02d/", TDCS_ETAG_RESOURCE_ROOT, rootDir, datePart, hour--);
				response = httpClient.execute(HttpUtils.toHttpRequest(etagResourceUrl));
			}
			Document doc = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), etagResourceUrl);
			Elements elements = doc.select(LINK_ELEMENT);
			int size = elements.size();
			if (size <= 1) {
				datePart = DateUtils.getOtherDateStr(datePart, -1);
				etagResourceUrl = String.format("%s/%s/%s/%02d/", TDCS_ETAG_RESOURCE_ROOT, rootDir, datePart, hour--);
				response = httpClient.execute(HttpUtils.toHttpRequest(etagResourceUrl));
				doc = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), etagResourceUrl);
				elements = doc.select(LINK_ELEMENT);
				size = elements.size();
			}
			for (int i = 1; i < size; i++) {
				Element element = elements.get(i);
				String absUrl = element.absUrl("href");
				if (ClassUtils.isValid(absUrl)) return download(absUrl, dest);
			}
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return null;
	}

	/**
	 * 將指定根目錄及時間的TDCS來源網址檔案下載至指定路徑<br>
	 * Ex:<br>
	 * rootDit: M04A, date: new Date(),hour: 5, dest: 指定下載目錄<br>
	 * 程式就會自行下載5點到6點間的所有Etag檔案至指定下載目錄<br>
	 *
	 * @param rootDir
	 *            TDCS檔案根目錄, String型別
	 * @param hour
	 *            欲下載的TDCS檔案時間(24小時制/小時為單位), int型別
	 * @param dest
	 *            輸出檔案路徑, String型別
	 * @return 下載的檔案資料目錄, File型別
	 */
	public static File downloadEtagFilesByHour(String rootDir, Date date, int hour, String dest) {
		File outputDir = new File(dest);
		String dateStr = DateUtils.formatDateToStr(DateUtils.SIMPLE_DATE_FORMAT, date);
		rootDir = rootDir.toUpperCase();
		String etagResourceUrl = String.format("%s/%s/%s/%02d/", TDCS_ETAG_RESOURCE_ROOT, rootDir, dateStr, hour);
		try (CloseableHttpClient httpClient = (CloseableHttpClient) HttpUtils.createHttpClient();
				CloseableHttpResponse response = httpClient.execute(HttpUtils.toHttpRequest(etagResourceUrl))) {
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				Document doc = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), etagResourceUrl);
				Elements elements = doc.select(LINK_ELEMENT);
				int size = elements.size();
				if (size > 1) {
					for (int i = 1; i < size; i++) {
						Element element = elements.get(i);
						String absUrl = element.absUrl("href");
						if (ClassUtils.isValid(absUrl)) download(absUrl, dest);
					}
				} else {
					throw new IOException(String.format("[%s] No File Found", etagResourceUrl));
				}
			}
		} catch (IOException e) {
			LOGGER.error("Erorr connect the resource: {}", etagResourceUrl);
			LOGGER.error(e.getMessage(), e);
		}

		return FileOperationUtils.isEmpty(outputDir) ? null : outputDir;
	}
}
