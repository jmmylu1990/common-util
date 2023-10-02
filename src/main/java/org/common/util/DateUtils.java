package org.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Van
 * @version 2.5
 */
public class DateUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(DateUtils.class);
	
	public enum DateUnitShortNameEnum {
		D, W, M, Y
	}
	
	/**
	 * [正規表示式Regex]<br>
	 * 表示日期呼叫toString方法後轉換的另類字串格式<br>
	 */
	public static final String DATE_OTHER_TOSTRING_FORMAT_REGEX = "\\p{Alpha}{2,4},\\s\\d{1,2}\\s\\p{Alpha}{3,}\\s\\d{4}\\s\\d{2}:\\d{2}:\\d{2}\\s(\\p{Upper}{3,}(\\+\\d)?)";

	/**
	 * [正規表示式Regex]<br>
	 * 表示日期呼叫toString方法後轉換的字串格式<br>
	 */
	public static final String DATE_TOSTRING_FORMAT_REGEX = "\\p{Alpha}{2,7}\\s\\p{Alpha}{3,}\\s\\d{1,2}\\s\\d{2}:\\d{2}:\\d{2}\\s(\\p{Upper}{3,}(\\+\\d)?)\\s\\d{4}";
	/**
	 * [正規表示式Regex]<br>
	 * 表示日期為以下格式的字串<br>
	 * 1. yyyy-MM-dd<br>
	 * 2. yyyyMMdd<br>
	 * 3. yyyy.MM.dd<br>
	 * 4. yyyy/MM/dd<br>
	 */
	public static final String DATE_FORMAT_REGEX = "(\\d{4})[\\.\\-/]?(\\d{2})[\\.\\-/]?(\\d{2})";
	/**
	 * [正規表示式Regex]<br>
	 * 表示時間為以下格式的字串<br>
	 * 1. HH:mm:ss<br>
	 * 2. HH:mm<br>
	 * 3. HHmmss<br>
	 * 4. HHmm<br>
	 */
	public static final String TIME_FORMAT_REGEX = "(\\d{2}):?(\\d{2})(:?(\\d{2}))?";

	/**
	 * [正規表示式Regex]<br>
	 * 表示日期為以下格式的字串<br>
	 * 1. yyyy-MM-dd HH:mm:ss<br>
	 * 2. yyyyMMdd HH:mm:ss<br>
	 * 3. yyyy.MM.dd HH:mm:ss<br>
	 * 4. yyyy/MM/dd HH:mm:ss<br>
	 * 5. yyyy-MM-dd HH:mm<br>
	 * 6. yyyyMMdd HH:mm<br>
	 * 7. yyyy.MM.dd HH:mm<br>
	 * 8. yyyy/MM/dd HH:mm<br>
	 */
	public static final String DATETIME_FORMAT_REGEX = "(\\d{4})[\\.\\-/]?(\\d{2})[\\.\\-/]?(\\d{2})(\\s|T)(\\d{2})(:\\d{2})(:\\d{2})?";

	/**
	 * [正規表示式Regex]<br>
	 * 表示日期為以下格式的字串<br>
	 * 1. yyyy-MM-dd<br>
	 * 2. yyyyMMdd<br>
	 * 3. yyyy.MM.dd<br>
	 * 4. yyyy/MM/dd<br>
	 * 5. yyyyMMdd<br>
	 * 6. yyyy-MM-dd HH:mm:ss<br>
	 * 7. yyyyMMdd HH:mm:ss<br>
	 * 8. yyyy.MM.dd HH:mm:ss<br>
	 * 9. yyyy/MM/dd HH:mm:ss<br>
	 * 10. yyyy-MM-dd HH:mm<br>
	 * 11. yyyyMMdd HH:mm<br>
	 * 12. yyyy.MM.dd HH:mm<br>
	 * 13. yyyy/MM/dd HH:mm<br>
	 */
	public static final String DATE_SIMILAR_REGEX = "(\\d{4})[\\.\\-/年](\\d{2})[\\.\\-/月](\\d{2})(\\s|\\p{Alpha})?((\\d{2})(:\\d{2})(:\\d{2})?(\\.\\d{3})?((\\+|\\-)\\d{2}:0{2})?)?";
	
	/**
	 * 日期格式: EEE MMM dd HH:mm:ss z yyyy
	 */
	public static final String STRING_DATE_FORMAT = "EEE MMM dd HH:mm:ss z yyyy";

	/**
	 * 日期格式: EEE, dd MMM yyyy HH:mm:ss Z
	 */
	public static final String OTHER_STRING_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";

	/**
	 * 年的格式: yyyy
	 */
	public static final String YEAR_FORMAT = "yyyy";
	
	/**
	 * 年月的格式: yyyyMM
	 */
	public static final String SIMPLE_YEAR_MONTH_FORMAT = "yyyyMM";
	
	/**
	 * 年月的格式: yyyy-MM
	 */
	public static final String DASHED_YEAR_MONTH_FORMAT = "yyyy-MM";
	
	/**
	 * 年月的格式: yyyy.MM
	 */
	public static final String DOT_YEAR_MONTH_FORMAT = "yyyy.MM";

	/**
	 * 年月的格式: yyyy/MM
	 */
	public static final String SLASH_YEAR_MONTH_FORMAT = "yyyy/MM";
	
	/**
	 * 日期格式: yyyyMMdd
	 */
	public static final String SIMPLE_DATE_FORMAT = "yyyyMMdd";
	
	/**
	 * 日期格式: yyyy-MM-dd
	 */
	public static final String DASHED_DATE_FORMAT = "yyyy-MM-dd";

	/**
	 * 日期格式: yyyy.MM.dd
	 */
	public static final String DOT_DATE_FORMAT = "yyyy.MM.dd";

	/**
	 * 日期格式: yyyy/MM/dd
	 */
	public static final String SLASH_DATE_FORMAT = "yyyy/MM/dd";

	/**
	 * 日期格式: yyyy-MM-dd HH:mm:ss
	 */
	public static final String DASHED_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

	/**
	 * 日期格式: yyyyMMddHHmmss
	 */
	public static final String SIMPLE_DATETIME_FORMAT = "yyyyMMddHHmmss";

	/**
	 * 日期格式: yyyy.MM.dd HH:mm:ss
	 */
	public static final String DOT_DATETIME_FORMAT = "yyyy.MM.dd HH:mm:ss";

	/**
	 * 日期格式: yyyy/MM/dd HH:mm:ss
	 */
	public static final String SLASH_DATETIME_FORMAT = "yyyy/MM/dd HH:mm:ss";

	/**
	 * 預設日期格式: yyyy-MM-dd
	 */
	private static String defaultDateFormat = DASHED_DATE_FORMAT;

	/**
	 * 預設日期時間格式: yyyy-MM-dd HH:mm:ss
	 */
	private static String defaultDatetimeFormat = DASHED_DATETIME_FORMAT;
	
	/*
	 * Since Taiwan time zone is GMT+8
	 */
	public static final Date INITIAL_DATE = new Date(-8 * 3600 * 1000L); 
	
	/**
	 * No constructor for utils class
	 */
	private DateUtils() {
	}

	/**
	 * 變更預設日期格式
	 *
	 * @param dateFormat
	 *            日期格式, String型別
	 */
	public static void setDateFormat(String dateFormat) {
		defaultDateFormat = dateFormat;
	}

	/**
	 * 變更預設日期時間格式
	 *
	 * @param dateTimeFormat
	 *            日期時間格式, String型別
	 */
	public static void setDateTimeFormat(String dateTimeFormat) {
		defaultDatetimeFormat = dateTimeFormat;
	}
	
	/**
	 * 採用預日期時間格式, 取得上週日期字串<br>
	 * Ex: 2017-11-29
	 *
	 * @return 昨天日期, String型別
	 */
	public static String lastWeek() {
		return getOtherDateStr(new Date(), -7);
	}

	/**
	 * 採用預設日期時間格式, 取得昨天日期字串<br>
	 * Ex: 2017-11-29
	 *
	 * @return 昨天日期, String型別
	 */
	public static String yesterday() {
		return yesterday(defaultDateFormat);
	}
	
	/**
	 * [方法多載]
	 * 採用指定日期格式, 取得昨天日期字串<br>
	 * Ex: yyyyMMdd -> 20171129
	 *
	 * @param format 日期字串格式
	 * 
	 * @return 昨天日期, String型別
	 */
	public static String yesterday(String format) {
		return formatDateToStr(format, addDays(new Date(), -1));
	}

	/**
	 * 採用預設日期時間格式, 取得當下時間字串<br>
	 * Ex: 2017-11-30 12:10:30
	 *
	 * @return 當下日期時間, String型別
	 */
	public static String now() {
		return now(defaultDatetimeFormat);
	}
	
	/**
	 * [方法多載]
	 * 採用指定日期格式, 取得當下時間字串<br>
	 * Ex: yyyyMMddHHmmss -> 20171129123000
	 *
	 * @param format 日期字串格式
	 * 
	 * @return 當下日期時間, String型別
	 */
	public static String now(String format) {
		return formatDateToStr(format, new Date());
	}

	/**
	 * 採用預設日期時間格式, 取得當下日期字串<br>
	 * Ex: 2017-11-30
	 *
	 * @return 當下日期, String型別
	 */
	public static String today() {
		return today(defaultDateFormat);
	}
	
	/**
	 * [方法多載]
	 * 採用指定日期格式, 取得當下日期字串<br>
	 * Ex: yyyyMMdd -> 20171130
	 *
	 * @param format 日期字串格式
	 * 
	 * @return 當下日期, String型別
	 */
	public static String today(String format) {
		return formatDateToStr(format, new Date());
	}

	/**
	 * 採用預設日期時間格式, 取得明天日期字串<br>
	 * Ex: 2017-12-01
	 *
	 * @return 明天日期, String型別
	 */
	public static String tomorrow() {
		return tomorrow(defaultDateFormat);
	}
	
	/**
	 * [方法多載]
	 * 採用指定日期格式, 取得明天日期字串<br>
	 * Ex: yyyyMMdd -> 20171201
	 *
	 * @param format 日期字串格式
	 * 
	 * @return 明天日期, String型別
	 */
	public static String tomorrow(String format) {
		return formatDateToStr(format, addDays(new Date(), 1));
	}

	/**
	 * 採用預設日期時間格式, 取得隔週日期字串<br>
	 * Ex: 2017-12-01
	 *
	 * @return 明天日期, String型別
	 */
	public static String nextWeek() {
		return getOtherDateStr(new Date(), 7);
	}
	
	/**
	 *    取得當周的起始日期
	 * 
	 * @return 當周的起始日期, Date型別
	 */
	public static Date currectWeekStartDate(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		while (calendar.get(Calendar.DAY_OF_WEEK) > 2) {
		    calendar.add(Calendar.DATE, -1); // Substract 1 day until first day of week.
		}
		
		return new java.sql.Date(calendar.getTimeInMillis());
	}
	
	/**
	 *  [方法多載]<br>
	 *   支援無須參數，以當天日期作為基準日; 取得當周的起始日期
	 * 
	 * @return 當周的起始日期, Date型別
	 */
	public static Date currectWeekStartDate() {
		return currectWeekStartDate(new Date());
	}

	/**
	 * 	當周的結束日期
	 * 
	 * @return 當周的結束日期, Date型別
	 */
	public static Date currectWeekEndDate(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		while (calendar.get(Calendar.DAY_OF_WEEK) > 1) {
		    calendar.add(Calendar.DATE, 1); // Substract 1 day until last day of week.
		}
		
		return new java.sql.Date(calendar.getTimeInMillis());
	}
	
	/**
	 * 	[方法多載]<br>
	 *   支援無須參數，以當天日期作為基準日; 當周的結束日期
	 * 
	 * @return 當周的結束日期, Date型別
	 */
	public static Date currectWeekEndDate() {
		return currectWeekEndDate(new Date());
	}

	/**
	 * 取得當下UTC時間
	 *
	 * @return 當下UTC時間, String型別
	 */
	public static String getServerTime() {
		Calendar calendar = Calendar.getInstance();
		DateFormat df = new SimpleDateFormat(OTHER_STRING_DATE_FORMAT, Locale.US);
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		return df.format(calendar.getTime());
	}
	
	/**
	 * 傳入指定字符轉換為Calendar欄位<br>
	 * <p>Ex:<p>
	 * <code>D --> Calendar.DAY_OF_MONTH</code><br>
	 * <code>W --> Calendar.WEEK_OF_MONTH</code><br>
	 * <code>M --> Calendar.MONTH</code><br>
	 * <code>Y --> Calendar.YEAR</code><br>
	 * 
	 * @see Calendar
	 * @param s 指定字符
	 * @return 對應的Calendar field
	 */
	public static int toCalendarField(String s) {
		switch (DateUnitShortNameEnum.valueOf(s.toUpperCase())) {
		default:
		case D:
			return Calendar.DAY_OF_MONTH;
		case W:
			return Calendar.WEEK_OF_MONTH;
		case M:
			return Calendar.MONTH;
		case Y:
			return Calendar.YEAR;
		}
	}
	
	/**
	 * 傳入指定字符轉換為ChronoUnit欄位<br>
	 * <p>Ex:<p>
	 * <code>D --> ChronoUnit.DAYS</code><br>
	 * <code>W --> ChronoUnit.WEEKS</code><br>
	 * <code>M --> ChronoUnit.MONTHS</code><br>
	 * <code>Y --> ChronoUnit.YEARS</code><br>
	 * 
	 * @see ChronoUnit
	 * @param s 指定字符
	 * @return 對應的ChronoUnit
	 */
	public static ChronoUnit toTemporalUnit(String s) {
		switch (DateUnitShortNameEnum.valueOf(s.toUpperCase())) {
		default:
		case D:
			return ChronoUnit.DAYS;
		case W:
			return ChronoUnit.WEEKS;
		case M:
			return ChronoUnit.MONTHS;
		case Y:
			return ChronoUnit.YEARS;
		}
	}
	
	/**
	 * 傳入Date型別的參數; 回傳傳入日期時間前後位移量的日期或時間<br>
	 * Ex:<br>
	 * date: 2017-04-02 12:33:00, type: Calendar.HOUR, offset: 12 => 2017-04-03 00:33:00<br>
	 * date: 2017-04-02 12:33:00, type: Calendar.HOUR, offset: -12 => 2017-04-02 00:33:00
	 *
	 * @see Calendar
	 * @see DateFormat
	 * @param date
	 *            基準日期, Date型別
	 * @param type
	 *            變動的時間型別參數, int型別
	 * @param offset
	 *            日期位移量, int型別
	 * @return 計算位移量後的日期, Date型別
	 */
	public static Date add(Date date, int type, int offset) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(type, offset);

		return calendar.getTime();
	}
	
	/**
	 * 傳入Date型別的參數; 回傳傳入日期時間前後[年]位移量的日期或時間<br>
	 * Ex:<br>
	 * date: 2017-04-02, offset: 12 => 2029-04-02<br>
	 * date: 2017-04-02, offset: -12 => 2005-04-02
	 *
	 * @see Calendar#YEAR
	 * @param date
	 *            基準日期, Date型別
	 * @param offset
	 *            年位移量, int型別
	 * @return 計算[年]位移量後的日期, Date型別
	 */
	public static Date addYears(Date date, int offset) {
		return add(date, Calendar.YEAR, offset);
	}
	
	/**
	 * 傳入Date型別的參數; 回傳傳入日期時間前後[月]位移量的日期或時間<br>
	 * Ex:<br>
	 * date: 2017-04-02, offset: 12 => 2029-04-02<br>
	 * date: 2017-04-02, offset: -12 => 2005-04-02
	 *
	 * @see Calendar
	 * @see DateFormat
	 * @param date
	 *            基準日期, Date型別
	 * @param offset
	 *            月位移量, int型別
	 * @return 計算[月]位移量後的日期, Date型別
	 */
	public static Date addMonths(Date date, int offset) {
		return add(date, Calendar.MONTH, offset);
	}
	
	/**
	 * 傳入Date型別的參數; 回傳傳入日期時間前後[日]位移量的日期或時間<br>
	 * Ex:<br>
	 * date: 2017-04-02, offset: 12 => 2017-04-14<br>
	 * date: 2017-04-02, offset: -12 => 2017-03-21
	 *
	 * @see Calendar#DAY_OF_MONTH
	 * @param date
	 *            基準日期, Date型別
	 * @param offset
	 *            日期位移量, int型別
	 * @return 計算[日]位移量後的日期, Date型別
	 */
	public static Date addDays(Date date, int offset) {
		return add(date, Calendar.DAY_OF_MONTH, offset);
	}
	
	/**
	 * 傳入Date型別的參數; 回傳傳入日期時間前後[時]位移量的日期或時間<br>
	 * Ex:<br>
	 * date: 2017-04-02 12:33:00, offset: 12 => 2017-04-03 00:33:00<br>
	 * date: 2017-04-02 12:33:00, offset: -12 => 2017-04-02 00:33:00
	 *
	 * @see Calendar#HOUR
	 * @param date
	 *            基準日期, Date型別
	 * @param offset
	 *            時位移量, int型別
	 * @return 計算[時]位移量後的日期, Date型別
	 */
	public static Date addHours(Date date, int offset) {
		return add(date, Calendar.HOUR, offset);
	}
	
	/**
	 * 傳入Date型別的參數; 回傳傳入日期時間前後[分]位移量的日期或時間<br>
	 * Ex:<br>
	 * date: 2017-04-02 12:33:00, offset: 12 => 2017-04-02 12:45:00<br>
	 * date: 2017-04-02 12:33:00, offset: -12 => 2017-04-02 12:21:00
	 *
	 * @see Calendar#MINUTE
	 * @param date
	 *            基準日期, Date型別
	 * @param offset
	 *            分位移量, int型別
	 * @return 計算[分]位移量後的日期, Date型別
	 */
	public static Date addMinutes(Date date, int offset) {
		return add(date, Calendar.MINUTE, offset);
	}
	
	/**
	 * 傳入Date型別的參數; 回傳傳入日期時間前後[秒]位移量的日期或時間<br>
	 * Ex:<br>
	 * date: 2017-04-02 12:33:00, offset: 12 => 2017-04-02 12:45:00<br>
	 * date: 2017-04-02 12:33:00, offset: -12 => 2017-04-02 12:21:00
	 *
	 * @see Calendar#SECOND
	 * @param date
	 *            基準日期, Date型別
	 * @param offset
	 *            秒位移量, int型別
	 * @return 計算[秒]位移量後的日期, Date型別
	 */
	public static Date addSeconds(Date date, int offset) {
		return add(date, Calendar.SECOND, offset);
	}
	
	/**
	 * 	依指定區間間隔, 取得指定起訖日期區間中的所有值
	 * 
	 * @param start 起始日, Date型別
	 * @param end 結束日, Date型別
	 * @param type 間隔類型, 依java.util.Calendar定義, int型別
	 * @return 所有符合的結果, List型別
	 */
	public static List<Date> range(Date start, Date end, int type) {
		List<Date> resultList = new ArrayList<>();
		Calendar startCal = Calendar.getInstance();
		startCal.setTime(start);
		Calendar endCal = Calendar.getInstance();
		endCal.setTime(end);
		while (startCal.before(endCal)) {
			resultList.add(startCal.getTime());
			startCal.add(type, 1);
		}
		resultList.add(endCal.getTime());

		return resultList;
	}
	
	/**
	 * [方法多載]<br>
	 * 	支援格式化成指定字串格式; 依指定區間間隔, 取得指定起訖日期區間中的所有值
	 * 
	 * @param start 起始日, Date型別
	 * @param end 結束日, Date型別
	 * @param format 字串顯示格式, String型別
	 * @param type 間隔類型, 依java.util.Calendar定義, int型別
	 * @return 所有符合的結果, List型別
	 */
	public static List<String> range(String start, String end, String format, int type) {
		return range(parseStrToDate(format, start), parseStrToDate(format, end), type).stream()
				.map(d -> formatDateToStr(format, d))
				.collect(Collectors.toList());
	}
	
	/**
	 * 回傳傳入起訖日期之間的所有日期<br>
	 * 沒指定時, 預設使用yyyy-MM-dd格式<br>
	 * 可透過DateUtils.setDateFormat(String)方法去指定
	 *
	 * @see Calendar
	 * @see DateFormat
	 * @see DateUtils#defaultDateFormat
	 * @see DateUtils#setDateFormat(String)
	 * @param startDate
	 *            起始日期, Date型別
	 * @param endDate
	 *            結束日期, Date型別
	 * @return 存放日期字串的List, List型別
	 */
	public static List<Date> getDateInterval(Date startDate, Date endDate) {
		List<Date> dateList = new ArrayList<>();
		int offset = getTimeDiff(Calendar.DAY_OF_MONTH, startDate, endDate);
		for (int i = 0; i <= offset; i++) {
			dateList.add(addDays(startDate, i));
		}
		return dateList;
	}

	/**
	 * [方法多載]<br>
	 * 支援傳入字串型別的日期參數; 回傳傳入起訖日期之間的所有日期<br>
	 * 註: 自動識別傳入日期字串參數的格式
	 *
	 * @see Calendar
	 * @see DateFormat
	 * @see DateUtils#dateToFormat(String)
	 * @param startDate
	 *            起始日期, Date型別
	 * @param endDate
	 *            結束日期, Date型別
	 * @return 存放日期字串的List, List型別
	 */
	public static List<String> getDateInterval(String startDate, String endDate) {
		List<String> dateList = new ArrayList<>();
		try {
			DateFormat df = new SimpleDateFormat(dateToFormat(startDate));		
			Calendar startCal = Calendar.getInstance();
			Calendar endCal = Calendar.getInstance();
			
			// 設定起始日期, 和結束日期
			startCal.setTime(df.parse(startDate));
			endCal.setTime(df.parse(endDate));
			// 將起始日期, 先加入集合物件
			dateList.add(df.format(startCal.getTime()));
	
			while (startCal.before(endCal)) {
				// 針對日期去做"遞增"動作, 並逐一添加進集合物件中
				startCal.add(Calendar.DATE, 1);
				dateList.add(df.format(startCal.getTime()));
			}
		} catch (ParseException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return dateList;
	}

	/**
	 * 回傳傳入日期前後位移量的日期字串<br>
	 * 註: 自動識別傳入日期字串參數的日期格式<br>
	 * Ex:<br>
	 * date: 2017-04-02, offset: 12 => 2017-04-14<br>
	 * date: 2017-04-02, offset: -12 => 2017-03-21
	 *
	 * @see Calendar
	 * @see DateFormat
	 * @see DateUtils#dateToFormat(String)
	 * @param date
	 *            基準日期, String型別
	 * @param offset
	 *            日期位移量, int型別
	 * @return 計算位移量後的日期, String型別
	 */
	public static String getOtherDateStr(String date, int offset) {
		DateFormat df = new SimpleDateFormat(dateToFormat(date));
		Calendar day = Calendar.getInstance();
		try {
			day.setTime(df.parse(date));
		} catch (ParseException e) {
			LOGGER.error(e.getMessage(), e);
		}
		day.add(Calendar.DATE, offset);

		return df.format(day.getTime());
	}

	/**
	 * [方法多載]<br>
	 * 支援傳入Date型別的參數; 回傳傳入日期前後位移量的日期字串<br>
	 * Ex:<br>
	 * date: 2017-04-02, offset: 12 => 2017-04-14<br>
	 * date: 2017-04-02, offset: -12 => 2017-03-21
	 *
	 * @see Calendar
	 * @see DateFormat
	 * @see DateUtils#dateToFormat(String)
	 * @param date
	 *            基準日期, Date型別
	 * @param offset
	 *            日期位移量, int型別
	 * @return 計算位移量後的日期, String型別
	 */
	public static String getOtherDateStr(Date date, int offset) {
		return getOtherDateStr(formatDateToStr(defaultDateFormat, date), offset);
	}

	/**
	 * [方法多載]<br>
	 * 回傳當下日期前後位移量的日期字串<br>
	 * Ex:<br>
	 * date: 2017-04-02, offset: 12 => 2017-04-14<br>
	 * date: 2017-04-02, offset: -12 => 2017-03-21
	 *
	 * @see Calendar
	 * @see DateFormat
	 * @see DateUtils#dateToFormat(String)
	 * @param offset
	 *            日期位移量, int型別
	 * @return 計算位移量後的日期, String型別
	 */
	public static String getOtherDateStr(int offset) {
		return getOtherDateStr(formatDateToStr(defaultDateFormat, new Date()), offset);
	}

	/**
	 *
	 * 回傳傳入日期前後位移量的日期<br>
	 * Ex:<br>
	 * date: 2017-04-02, offset: 12 => 2017-04-14<br>
	 * date: 2017-04-02, offset: -12 => 2017-03-21
	 *
	 * @see Calendar
	 * @see DateFormat
	 * @see DateUtils#dateToFormat(String)
	 * @param date
	 *            基準日期, Date型別
	 * @param offset
	 *            日期位移量, int型別
	 * @return 計算位移量後的日期, Date型別
	 */
	public static Date getOtherDate(String date, int offset) {
		return parseStrToDate(getOtherDateStr(date, offset));
	}

	/**
	 * [方法多載]<br>
	 * 支援傳入Date型別的參數; 回傳傳入日期前後位移量的日期<br>
	 * Ex:<br>
	 * date: 2017-04-02, offset: 12 => 2017-04-14<br>
	 * date: 2017-04-02, offset: -12 => 2017-03-21
	 *
	 * @see Calendar
	 * @see DateFormat
	 * @see DateUtils#dateToFormat(String)
	 * @param date
	 *            基準日期, Date型別
	 * @param offset
	 *            日期位移量, int型別
	 * @return 計算位移量後的日期, Date型別
	 */
	public static Date getOtherDate(Date date, int offset) {
		return getOtherDate(formatDateToStr(date), offset);
	}

	/**
	 * [方法多載]<br>
	 * 回傳當下日期前後位移量的日期<br>
	 * Ex:<br>
	 * date: 2017-04-02, offset: 12 => 2017-04-14<br>
	 * date: 2017-04-02, offset: -12 => 2017-03-21
	 *
	 * @see Calendar
	 * @see DateFormat
	 * @see DateUtils#dateToFormat(String)
	 * @param offset
	 *            日期位移量, int型別
	 * @return 計算位移量後的日期, Date型別
	 */
	public static Date getOtherDate(int offset) {
		return getOtherDate(new Date(), offset);
	}

	/**
	 * 取得兩個時間物件中的時間差
	 *
	 * @param type
	 *            欲檢索時間差的單位, String型別
	 * @param startDate
	 *            起始時間, Date型別
	 * @param endDate
	 *            結束時間, Date型別
	 * @return 以指定單位計算後的差值
	 */
	public static int getTimeDiff(int type, Date startDate, Date endDate) {
		int offset = 0;
		if (startDate == null || endDate == null) return offset;
		switch (type) {
		case Calendar.SECOND:
			offset = (int) ((endDate.getTime() - startDate.getTime()) / 1000);
			break;
		case Calendar.MINUTE:
			offset = (int) ((endDate.getTime() - startDate.getTime()) / (60 * 1000));
			break;
		case Calendar.HOUR:
			offset = (int) ((endDate.getTime() - startDate.getTime()) / (60 * 60 * 1000));
			break;
		case Calendar.DAY_OF_MONTH:
			offset = (int) ((endDate.getTime() - startDate.getTime()) / (24 * 60 * 60 * 1000));
			break;
		case Calendar.WEEK_OF_MONTH:
			offset = (int) ((endDate.getTime() - startDate.getTime()) / (7 * 24 * 60 * 60 * 1000));
			break;
		case Calendar.MONTH:
			offset = (int) ((endDate.getTime() - startDate.getTime()) / (4 * 7 * 24 * 60 * 60 * 1000));
			break;

		default:
			break;
		}

		return Math.abs(offset);
	}

	/**
	 * 取得現在時間與指定時間的時間差
	 *
	 * @param type
	 *            欲檢索時間差的單位, String型別
	 * @param startDate
	 *            起始時間, Date型別
	 * @return 以指定單位計算後的差值
	 */
	public static int getTimeDiff(int type, Date startDate) {
		return getTimeDiff(type, startDate, new Date());
	}

	/**
	 * [方法多載]<br>
	 * 支援字串時間物件; 取得現在時間與指定時間的時間差
	 *
	 * @param type
	 *            欲檢索時間差的單位, String型別
	 * @param startDate
	 *            起始時間, String型別
	 * @return 以指定單位計算後的差值
	 */
	public static int getTimeDiff(int type, String startDate) {
		return getTimeDiff(type, parseStrToDate(startDate), new Date());
	}

	/**
	 * 取得兩個時間物件中的時間差
	 *
	 * @param type
	 *            欲檢索時間差的單位, String型別
	 * @param startDate
	 *            起始時間, String型別
	 * @param endDate
	 *            結束時間, String型別
	 * @return 以指定單位計算後的差值
	 */
	public static int getTimeDiff(int type, String startDate, String endDate) {
		return getTimeDiff(type, parseStrToDate(startDate), parseStrToDate(endDate));
	}
	
	/**
	 * [方法多載]
	 * 	取得相對指定日期的當月第一日
	 *
	 * @see Calendar
	 * @see Calendar#set(int, int)
	 * @return 相對指定日期當月的一號, Date型別
	 */
	public static Date getTheFirstDayOfMonth(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		
		return calendar.getTime();
	}

	/**
	 * [方法多載]
	 * 	取得當月的第一日
	 *
	 * @see Calendar
	 * @see Calendar#set(int, int)
	 * @return 當月的一號, Date型別
	 */
	public static Date getTheFirstDayOfMonth() {
		return getTheFirstDayOfMonth(new Date());
	}

	/**
	 * 支援回傳格式化後的日期字串; 取得當月的第一日; 預設日期格式為yyyy-MM-dd HH:mm:ss
	 *
	 * @see Calendar
	 * @see Calendar#set(int, int)
	 * @return 當月的一號, String型別
	 */
	public static String getTheFirstDayStrOfMonth() {
		return formatDateToStr(DASHED_DATETIME_FORMAT, getTheFirstDayOfMonth());
	}

	/**
	 * [方法多載]<br>
	 * 支援回傳格式化後的日期字串; 取得當月的第一日
	 *
	 * @param format
	 *            轉換後的日期格式, String型別
	 * @see Calendar
	 * @see Calendar#set(int, int)
	 * @return 當月的一號, String型別
	 */
	public static String getTheFirstDayStrOfMonth(String format) {
		return formatDateToStr(format, getTheFirstDayOfMonth());
	}
	
	/**
	 * [方法多載]<br>
	 * 支援回傳格式化後的日期字串; 取得指定日於當月的第一日
	 *
	 * @param format
	 *            轉換後的日期格式, String型別
	 * @see Calendar
	 * @see Calendar#set(int, int)
	 * @return 指定日期於當月的一號, String型別
	 */
	public static String getTheFirstDayStrOfMonth(Date date, String format) {
		return formatDateToStr(format, getTheFirstDayOfMonth(date));
	}

	/**
	 *	傳入指定日期; 取得相對該日期於當月的最後一日
	 *
	 * @param date
	 *            轉換後的日期格式, String型別
	 * @see Calendar
	 * @see Calendar#set(int, int)
	 * @return 當月的最後一號, Date型別
	 */
	public static Date getTheLastDayOfMonth(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));

		return calendar.getTime();
	}
	
	/**
	 * [方法多載]<br>
	 * 取得當月的最後一日
	 *
	 * @see Calendar
	 * @see Calendar#set(int, int)
	 * @return 當月的最後一號, Date型別
	 */
	public static Date getTheLastDayOfMonth() {
		return getTheLastDayOfMonth(new Date());
	}

	/**
	 * 支援回傳格式化後的日期字串; 取得當月的最後一日; 預設日期格式為yyyy-MM-dd HH:mm:ss
	 *
	 * @see Calendar
	 * @see Calendar#set(int, int)
	 * @return 當月的最後一號, String型別
	 */
	public static String getTheLastDayStrOfMonth() {
		return formatDateToStr(DASHED_DATETIME_FORMAT, getTheLastDayOfMonth());
	}

	/**
	 * [方法多載]<br>
	 * 支援回傳格式化後的日期字串; 取得當月的最後一日
	 *
	 * @param format
	 *            轉換後的日期格式, String型別
	 * @see Calendar
	 * @see Calendar#set(int, int)
	 * @return 當月的最後一號, String型別
	 */
	public static String getTheLastDayStrOfMonth(String format) {
		return formatDateToStr(format, getTheLastDayOfMonth());
	}
	
	/**
	 * [方法多載]<br>
	 * 支援回傳格式化後的日期字串; 取得指定日期於月的最後一日
	 *
	 * @param format
	 *            轉換後的日期格式, String型別
	 * @see Calendar
	 * @see Calendar#set(int, int)
	 * @return 指定日期於當月的最後一號, String型別
	 */
	public static String getTheLastDayStrOfMonth(Date date, String format) {
		return formatDateToStr(format, getTheLastDayOfMonth(date));
	}
	
	/**
	 * 將傳入的日期字串重新格式化為指定格式的日期字串
	 *
	 * @see DateFormat
	 * @param format
	 *            新的日期格式, String型別
	 * @param dateStr
	 *            欲格式化成字串的日期, String型別
	 * @return 重新格式化後的日期, String型別
	 */
	public static String formatRebuild(String format, String dateStr) {
		return formatDateToStr(format, parseStrToDate(dateStr));
	}
	
	/**
	 * 回傳指定日期格式的日期字串
	 *
	 * @see DateFormat
	 * @param format
	 *            日期格式, String型別
	 * @param date
	 *            欲格式化成字串的日期, Date型別
	 * @return 格式化後的日期, String型別
	 */
	public static String formatDateToStr(String format, Date date) {
		return ClassUtils.isValid(date) && ClassUtils.isValid(format) ? new SimpleDateFormat(format).format(date) : null;
	}

	/**
	 * [方法多載]<br>
	 * 支援java.sql.Date參數 回傳指定日期格式的日期字串
	 *
	 * @see DateFormat
	 * @param format
	 *            日期格式, String型別
	 * @param date
	 *            欲格式化成字串的日期, java.sql.Date型別
	 * @return 格式化後的日期, String型別
	 */
	public static String formatDateToStr(String format, java.sql.Date date) {
		return formatDateToStr(format, new Date(date.getTime()));
	}

	/**
	 * [方法多載]<br>
	 * 支援java.sql.Timestamp參數 回傳指定日期格式的日期字串
	 *
	 * @see DateFormat
	 * @param format
	 *            日期格式, String型別
	 * @param timestamp
	 *            欲格式化成字串的日期, java.sql.Timestamp型別
	 * @return 格式化後的日期, String型別
	 */
	public static String formatDateToStr(String format, Timestamp timestamp) {
		return formatDateToStr(format, new Date(timestamp.getTime()));
	}

	/**
	 * [方法多載]<br>
	 * 回傳指定日期格式的日期字串。<br>
	 * 當沒傳入日期格式參數時, 採用預設日期格式
	 *
	 * @param date
	 *            欲格式化成字串的日期, Date型別
	 * @return 格式化後的日期, String型別
	 */
	public static String formatDateToStr(Date date) {
		return formatDateToStr(DASHED_DATETIME_FORMAT, date);
	}

	/**
	 * [方法多載]<br>
	 * 支援java.sql.Date參數 回傳指定日期格式的日期字串<br>
	 * 當沒傳入日期格式參數時, 採用預設日期格式
	 *
	 * @param date
	 *            欲格式化成字串的日期, java.sql.Date型別
	 * @return 格式化後的日期, String型別
	 */
	public static String formatDateToStr(java.sql.Date date) {
		return formatDateToStr(defaultDatetimeFormat, new Date(date.getTime()));
	}

	/**
	 * [方法多載]<br>
	 * 支援java.sql.Timestamp參數 回傳指定日期格式的日期字串<br>
	 * 當沒傳入日期格式參數時, 採用預設日期格式
	 *
	 * @param timestamp
	 *            欲格式化成字串的日期, java.sql.Timestamp型別
	 * @return 格式化後的日期, String型別
	 */
	public static String formatDateToStr(Timestamp timestamp) {
		return timestamp != null ? 
				formatDateToStr(defaultDatetimeFormat, new Date(timestamp.getTime())) : null;
	}

	/**
	 * 回傳日期字串解析後的日期
	 *
	 * @see DateFormat
	 * @param format
	 *            日期字串格式, String型別
	 * @param date
	 *            日期參數, String型別
	 * @return 解析後的日期, Date型別
	 */
	public static Date parseStrToDate(String format, String date) {
		Date dateResult = null;
		DateFormat df = null;
		if (ClassUtils.isValid(format) && ClassUtils.isValid(date)) {
			try {
				switch (format) {
				case STRING_DATE_FORMAT:
				case OTHER_STRING_DATE_FORMAT:
					df = new SimpleDateFormat(format, Locale.ENGLISH);
					break;
				default:
					df = new SimpleDateFormat(format);
					break;
				}
				dateResult = df.parse(date);
			} catch (ParseException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return dateResult;
	}

	/**
	 * 回傳日期字串解析後的日期<br>
	 * 註: 自動識別傳入日期字串參數的格式
	 *
	 * @see DateFormat
	 * @see DateUtils#dateToFormat(String)
	 * @param date
	 *            日期字串格式, String型別
	 * @return 解析後的日期, Date型別
	 */
	public static Date parseStrToDate(String date) {
		return parseStrToDate(dateToFormat(date), date);
	}

	/**
	 * 將傳入的日期字串參數, 轉換為對應的日期格式<br>
	 * Ex:<br>
	 * 2017/04/21 => yyyy/MM/dd<br>
	 * 2017-04-21 15:22:30 => yyyy-MM-dd HH:mm:ss<br>
	 *
	 * @see String#format(String, Object...)
	 * @see DateUtils#SIMPLE_DATE_FORMAT
	 * @see DateUtils#DATE_FORMAT_REGEX
	 * @see DateUtils#DATETIME_FORMAT_REGEX
	 * @param date
	 *            欲分析格式的日期字串, String型別
	 * @return 解析後的日期格式, String型別
	 */
	public static String dateToFormat(String date) {
		String format = "";
		StringBuilder formatBuilder = new StringBuilder();
		if (date.matches("\\d+")) {
			format = SIMPLE_DATETIME_FORMAT.substring(0, date.length());
		} else if (date.matches(DATE_TOSTRING_FORMAT_REGEX)) {
			format =  STRING_DATE_FORMAT;
		} else if (date.matches(DATE_OTHER_TOSTRING_FORMAT_REGEX)) {
			format = OTHER_STRING_DATE_FORMAT;
		} else if (date.matches(DATE_SIMILAR_REGEX)) {
			String delimiter = StringTools.findFirstMatchSequence(date, "\\s|T");
			String[] dateTime = null;
			if (delimiter.matches("\\p{Alpha}")) {
				dateTime = date.split(delimiter);
				delimiter = "'" + delimiter + "'";
			} else {
				dateTime = date.split("\\s");
			}
			String datePart = dateTime[0];
			String[] dateTimeSegments = datePart.split("\\d+");
			String firstDelimiter = dateTimeSegments.length > 1 ? dateTimeSegments[1] : "";
			String secondDelimiter = dateTimeSegments.length > 2 ? dateTimeSegments[2] : "";
			formatBuilder.append("yyyy").append(firstDelimiter).append("MM").append(secondDelimiter).append("dd");
			if (dateTime.length >= 2) {
				String timePart = dateTime[1];
				String timeDelimiter = timePart.split("\\d+")[1];

				formatBuilder.append(delimiter).append("HH").append(timeDelimiter).append("mm");
				if (timePart.length() > 5) formatBuilder.append(timeDelimiter).append("ss");
			}
			format = formatBuilder.toString();
		}
		if (!format.isEmpty() && LOGGER.isDebugEnabled()) LOGGER.debug("Detect the date format: {}", format);
		
		return format;
	}

	/**
	 * 將花費時間的字串轉換為long型態資料<br>
	 * Ex:<br>
	 * 1小時1秒700毫秒 ==> 3601700
	 *
	 * @param costStr
	 *            花費時間, String型別
	 * @return 轉換後的數值, long型別
	 */
	public static long transferUsageTime(String costStr) {
		long costTime = 0L;
		String[] units = new String[] { "(小)?時", "分(鐘)?", "秒(鐘)?", "毫秒" };
		long unitVal = 3600000;
		long dividends = 60;
		for (String unit : units) {
			String regex = "\\d+" + unit;
			String matchesStr = StringTools.findFirstMatchSequence(costStr, regex);
			if (ClassUtils.isValid(matchesStr)) {
				int ammount = Integer.parseInt(matchesStr.replaceAll("\\D", ""));
				costTime += ammount * unitVal;
			}
			if (unitVal == 1000) dividends = 1000;
			unitVal /= dividends;
		}

		return costTime;
	}

	/**
	 * 將花費時間數值格式化為字串<br>
	 * Ex:<br>
	 * 3601700 ==> 1小時1秒700毫秒
	 *
	 * @param cost
	 *            花費時間, long型別
	 * @return 格式化後的字串, String型別
	 */
	public static String formatUsageTime(long cost) {
		StringBuilder sb = new StringBuilder();
		if (cost < 1000) return cost + "毫秒";
		String[] units = new String[] { "小時", "分", "秒" };
		long unitVal = 3600000L;
		for (String unit : units) {
			if (cost >= unitVal) {
				long resultVal = cost / unitVal;
				cost %= unitVal;
				sb.append(resultVal).append(unit);
			}
			unitVal /= 60;
		}
		if (cost > 0) sb.append(cost).append("毫秒");

		return sb.toString();
	}

	/**
	 * 依指定的模糊區間(單位: 分鐘) 將時間模糊化<br>
	 * Ex: blurry: 30, date: 2019-09-01 10:12:30 => 2019-09-01 10:00:00
	 * @param date 欲模糊化的時間物件, Date型別
	 * @param blurry 模糊區間(單位: 分鐘), int型別
	 * @return 模糊化後的時間物件, Date型別
	 */
	public static Date blurry(Date date, int blurry) {
		if (Objects.isNull(date)) return date;
		
		long time = date.getTime();
		long remain = time % (blurry * 60 * 1000);
		time = remain == 0 ? time : time - remain;
		
		return new Date(time);
	}
	
	/**
	 * [方法多載]<br>
	 * 支援日期字串參數; 依指定的模糊區間(單位: 分鐘) 將時間模糊化<br>
	 * Ex: blurry: 30, date: 2019-09-01 10:12:30 => 2019-09-01 10:00:00
	 * @param date 欲模糊化的時間物件, String型別
	 * @param blurry 模糊區間(單位: 分鐘), int型別
	 * @return 模糊化後的時間物件, String型別
	 */
	public static String blurry(String date, int blurry) {
		return formatDateToStr((blurry(parseStrToDate(date), blurry)));
	}

	public static boolean isSeries(String start, String next, int type) {
		Date startDate = DateUtils.parseStrToDate(start);
		Date nextDate = DateUtils.parseStrToDate(next);
		
		return Math.abs(getTimeDiff(type, startDate, nextDate)) <= 1;
	}
	
	public static List<String> timeSeriesMerge(List<Date> dateList, String format, CharSequence delimiter) {
		if (!ClassUtils.isValid(dateList)) return new ArrayList<>();
		
		int type = format.contains("dd") ? Calendar.DAY_OF_MONTH : Calendar.MONTH;
		DateFormat df = new SimpleDateFormat(format);
		List<String> mergeList = new ArrayList<>();
		List<Integer> list = dateList.stream()
					.map(df::format)
					.mapToInt(dateStr -> Integer.parseInt(dateStr.replaceAll("\\D", "")))
					.boxed()
					.collect(Collectors.toList());
		int start = list.get(0);
		int end = start;
		// Add the any end to ensure the original last element check
		list.add(Integer.parseInt(String.valueOf(start).replaceAll(".", "9")));
		int dateNum = list.size();
		for (int i = 1; i < dateNum; i++) {
			int next = list.get(i);
			// If date offset is less than 1, that is meaning date is sequence, moreover if is equal 89, that is, new year
//			int offset = next - end;
//			boolean keepDateSeq = offset <= 1 || offset % 89 == 0;
			String startStr = String.valueOf(start);
			String endStr = String.valueOf(end);
			String nextStr = String.valueOf(next);
			boolean keepSeries = isSeries(nextStr, endStr, type);
			if (!keepSeries) {
				// If last not sequence, then merge as range if start is not equal end
				String startDateStr = formatRebuild(format, startStr);
				String endDateStr = formatRebuild(format, endStr);
				mergeList.add(start != end ? String.join(delimiter, startDateStr, endDateStr) : endStr);
				start = next; // Assign new start if the next not sequence with previous
			}
			end = next;
		}
		
		return mergeList.stream().collect(Collectors.toList());
	}
	
	public static List<String> parseSeriesDesc(String desc, CharSequence delimterPerSubDesc, CharSequence delimterPerDate) {
		String[] segments = desc.split(delimterPerDate.toString());
		return Stream.of(segments).map(segment -> {
			String[] startAndEnd = segment.split(delimterPerSubDesc.toString());
			int start = Integer.parseInt(startAndEnd[0].replaceAll("\\D", ""));
			int end = startAndEnd.length < 2 ? start : Integer.parseInt(startAndEnd[1].replaceAll("\\D", ""));
			
			return IntStream.rangeClosed(start, end)
					.mapToObj(String::valueOf)
					.filter(dateStr -> dateStr.matches("^([1-9][0-9]{3})((0[1-9])|(1[0-2]))([012][0-9])?$"))
					.collect(Collectors.toList());
		}).flatMap(List::stream)
		.collect(Collectors.toList());
	}
}
