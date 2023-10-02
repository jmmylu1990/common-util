package org.common.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Van
 * @version 1.3
 */
public class StringTools {

	private static final Logger LOGGER = LoggerFactory.getLogger(StringTools.class);
	public static final String BLANK = "";
	public static final String SPACE = " ";
	public static final String COMMA = ",";
	public static final String DOT = ".";
	public static final String SLASH = "/";
	public static final String DASH = "-";
	public static final String NULL = "null";
	public static final String UTF8_BOM = "\uFEFF";
	public static final String TAB = "\t";
	public static final String CR = "\r";
	public static final String CRLF = "\r\n";
	public static final String LF = "\n";
	
	// Regexp
	public static final String MAIL_RULE = "^\\w+((-\\w+)|(\\.\\w+))*\\@[A-Za-z0-9]+((\\.|-)[A-Za-z0-9]+)*\\.[A-Za-z]+$";
	
	protected static final char[] CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@%^".toCharArray();

	private StringTools() {
	}

	/**
	 * 將字串以指定的index範圍做切割, 若index為負值, 則從字串末往前開始算<br>
	 * Ex:<br>
	 * <code>StringTools("ABCDE", 1, -1) --> "BCD"</code><br>
	 * <code>StringTools("ABCDE", -2, -1) --> "D"</code><br>
	 * 
	 * @since 1.2
	 * 
	 * @see String#substring(int, int)
	 *
	 * @param charSequence
	 *            欲裁切字串, CharSequence型別
	 * @param startIndex
	 *            切割起始index, int型別
	 * @param endIndex
	 *            切割結束index, int型別
	 * @return 切該完成的字串, String型別
	 */
	public static String slice(CharSequence charSequence, int startIndex, int endIndex) {
		int strLength = charSequence.length();
		startIndex = startIndex < 0 ? strLength + startIndex : startIndex;
		endIndex = endIndex < 0 ? strLength + endIndex : endIndex;

		return charSequence.toString().substring(startIndex, endIndex);
	}

	/**
	 * [方法多載]<br>
	 * 將字串以指定的index範圍切割至字串尾, 若index為負值, 則從字串末往前開始算<br>
	 * Ex:<br>
	 * <code>StringTools("ABCDE", 1, -1) --> "BCD"</code><br>
	 * <code>StringTools("ABCDE", -2, -1) --> "D"</code><br>
	 * 
	 * @since 1.2
	 * 
	 * @see String#substring(int)
	 *
	 * @param charSequence
	 *            欲裁切字串, CharSequence型別
	 * @param startIndex
	 *            切割起始index, int型別
	 * @return 切該完成的字串, String型別
	 */
	public static String slice(CharSequence charSequence, int startIndex) {
		int strLength = charSequence.length();
		startIndex = startIndex < 0 ? strLength + startIndex : startIndex;

		return charSequence.toString().substring(startIndex);
	}

	/**
	 * 回傳以指定符號串接的字串
	 *
	 * @since 1.0
	 *
	 * @see StringBuilder
	 * 
	 * @param token
	 *            分隔符號, String型別
	 * @param strs
	 *            欲串接組合的物件, Object參數列表
	 * @return 串街後的字串, String型別
	 */
	public static String join(CharSequence token, CharSequence... strs) {
		return Stream.of(strs).collect(Collectors.joining(token));
	}

	/**
	 * [方法多載] 支援集合物件類別的參數ㄤ; 回傳以指定符號串接的字串
	 *
	 * @since 1.0
	 *
	 * @see Collection
	 * @see StringBuilder
	 * 
	 * @param token
	 *            分隔符號, String型別
	 * @param strs
	 *            欲串接組合的物件, Collection型別
	 * @return 串街後的字串, String型別
	 */
	public static String join(CharSequence token, Collection<Object> strs) {
		StringBuilder sb = new StringBuilder();
		int tokenSize = token.length();
		int resultStrSize = 0;
		for (Object str : strs) {
			sb.append(str);
			sb.append(token);
		}
		resultStrSize = sb.length();

		return sb.toString().substring(0, (resultStrSize - tokenSize));
	}

	/**
	 * 回傳傳入參數首字母轉換為小寫的字串<br>
	 * Ex:<br>
	 * <code>Hello --> hello</code><br>
	 * <code>WORLD --> wORLD</code><br>
	 * <code>_JAVA --> _jAVA</code><br>
	 *
	 * @since 1.0
	 *
	 * @see CharSequence
	 * @see Character#isLetter(char)
	 * @param variable
	 *            欲轉換的參數, CharSequence型別
	 * @return 首字母小寫的字串, String型別
	 */
	public static String lowerFirstCase(CharSequence variable) {
		int index = 0;
		String str = variable.toString();
		char firstLetter = str.charAt(index);
		while (!Character.isLetter(firstLetter)) {
			firstLetter = str.charAt(++index);
			LOGGER.info("First letter of string: {}", firstLetter);
		}
		String startAlpha = String.valueOf(firstLetter);
		startAlpha = startAlpha.toLowerCase();
		str = (index > 0) ? str.substring(0, index) + startAlpha + str.substring(index + 1) : startAlpha + str.substring(1);

		return str;
	}

	/**
	 * 回傳傳入參數首字母轉換為大寫的字串<br>
	 * Ex:<br>
	 * <code></code>hello => Hello</code><br>
	 * <code></code>world = > World</code><br>
	 * <code></code>_java = > _Java</code><br>
	 *
	 * @since 1.0
	 *
	 * @see CharSequence
	 * @see Character#isLetter(char)
	 * 
	 * @param variable
	 *            欲轉換的參數, CharSequence型別
	 * @return 首字母大寫的字串, String型別
	 */
	public static String upperFirstCase(CharSequence variable) {
		int index = 0;
		String str = variable.toString();
		char firstLetter = str.charAt(index);
		while (!Character.isLetter(firstLetter)) {
			firstLetter = str.charAt(++index);
			LOGGER.info("First letter of string {}", firstLetter);
		}
		String startAlpha = String.valueOf(firstLetter);
		startAlpha = startAlpha.toUpperCase();
		str = (index > 0) ? str.substring(0, index) + startAlpha + str.substring(index + 1) : startAlpha + str.substring(1);

		return str;
	}

	/**
	 * 反轉字串內容
	 * 
	 * @since 1.1
	 * 
	 * @param orignSequence
	 *            原始字串內容,String型別
	 * @return 反轉過後的字串內容, String型別
	 */
	public static String reverse(CharSequence orignSequence) {
		StringBuilder sb = new StringBuilder();
		char[] charAry = orignSequence.toString().toCharArray();
		int lastIndex = charAry.length - 1;
		for (int i = lastIndex; i >= 0; i--) {
			sb.append(charAry[i]);
		}

		return sb.toString();
	}

	/**
	 * 將傳入的集合物件, 解析成每一列的物件toString
	 * 
	 * @since 1.1
	 *
	 * @see Collection
	 * @see Object#toString()
	 * 
	 * @param collection
	 *            集合物件, Collection型別
	 * @return 拆解後的字串, String型別
	 */
	public static String collectionToString(Collection<?> collection) {
		String collectionStr = collection.toString();
		StringBuilder sb = new StringBuilder();
		String[] objStrs = collectionStr.replaceAll("(^\\[)|(\\]$)", "").split(",\\s");
		for (String objStr : objStrs) {
			sb.append(objStr).append(CRLF);
		}
		sb.setLength(sb.length() - 2);

		return sb.toString();
	}

	/**
	 * 找出第一個符合指定Regex的子字串
	 * 
	 * @since 1.1
	 *
	 * @param content
	 *            字串內容, CharSequence型別
	 * @param pattern
	 *            正規表達式之Pattern物件, Pattern型別
	 * @return 依照Regex找尋到的子字串, String型別
	 */
	public static String findFirstMatchSequence(CharSequence content, Pattern pattern) {
		Matcher matcher = pattern.matcher(content);
		return matcher.find() ? matcher.group() : "";
	}
	
	/**
	 * [方法多載]<br>
	 * 找出第一個符合指定Regex的子字串
	 * 
	 * @since 1.1
	 *
	 * @param content
	 *            字串內容, CharSequence型別
	 * @param regex
	 *            正規表達式, String型別
	 * @return 依照Regex找尋到的子字串, String型別
	 */
	public static String findFirstMatchSequence(CharSequence content, String regex) {
		return findFirstMatchSequence(content, Pattern.compile(regex));
	}

	/**
	 * 找出所有符合指定Regex的子字串
	 * 
	 * @since 1.1
	 *
	 * @param content
	 *            字串內容, CharSequence型別
	 * @param pattern
	 *             正規表達式之Pattern物件, Pattern型別
	 * @return 依照Regex找尋到的所有子字串, List型別
	 */
	public static List<String> findAllMatchSequences(CharSequence content, Pattern pattern) {
		List<String> resultList = new ArrayList<>();
		Matcher matcher = pattern.matcher(content);
		while (matcher.find()) resultList.add(matcher.group());
		
		return resultList;
	}
	
	/**
	 * [方法多載]<br>
	 * 找出所有符合指定Regex的子字串
	 * 
	 * @since 1.1
	 *
	 * @param content
	 *            字串內容, CharSequence型別
	 * @param regex
	 *            正規表達式, String型別
	 * @return 依照Regex找尋到的所有子字串, List型別
	 */
	public static List<String> findAllMatchSequences(CharSequence content, String regex) {
		return findAllMatchSequences(content, Pattern.compile(regex));
	}

	/**
	 * 將物件陣列轉換為字串陣列
	 * 
	 * @since 1.1
	 *
	 * @param objAry
	 *            物件陣列, Object[]型別
	 * @return 每個陣列值呼叫toString方法後的陣列, String[]型別
	 */
	public static String[] toStringArray(Object[] objAry) {
		if (objAry != null) {
			int aryLength = objAry.length;
			String[] strAry = new String[aryLength];
			for (int i = 0; i < aryLength; i++) {
				strAry[i] = objAry[i].toString();
			}

			return strAry;
		}

		return new String[0];
	}

	/**
	 * 將boolean陣列轉換為字串陣列
	 *
	 * @since 1.1
	 * 
	 * @param booleanAry
	 *            boolean陣列, boolean[]型別
	 * @return 每個陣列值呼叫toString方法後的陣列, String[]型別
	 */
	public static String[] toStringArray(boolean[] booleanAry) {
		if (booleanAry != null) {
			int aryLength = booleanAry.length;
			String[] strAry = new String[aryLength];
			for (int i = 0; i < aryLength; i++) {
				strAry[i] = String.valueOf(booleanAry[i]);
			}

			return strAry;
		}

		return new String[0];
	}

	/**
	 * 將byte陣列轉換為字串陣列
	 * 
	 * @since 1.1
	 *
	 * @param byteAry
	 *            byte陣列, byte[]型別
	 * @return 每個陣列值呼叫toString方法後的陣列, String[]型別
	 */
	public static String[] toStringArray(byte[] byteAry) {
		if (byteAry != null) {
			int aryLength = byteAry.length;
			String[] strAry = new String[aryLength];
			for (int i = 0; i < aryLength; i++) {
				strAry[i] = String.valueOf(byteAry[i]);
			}

			return strAry;
		}

		return new String[0];
	}

	/**
	 * 將short陣列轉換為字串陣列
	 * 
	 * @since 1.1
	 *
	 * @param shortAry
	 *            short陣列, short[]型別
	 * @return 每個陣列值呼叫toString方法後的陣列, String[]型別
	 */
	public static String[] toStringArray(short[] shortAry) {
		if (shortAry != null) {
			int aryLength = shortAry.length;
			String[] strAry = new String[aryLength];
			for (int i = 0; i < aryLength; i++) {
				strAry[i] = String.valueOf(shortAry[i]);
			}

			return strAry;
		}

		return new String[0];
	}

	/**
	 * 將int陣列轉換為字串陣列
	 * 
	 * @since 1.1
	 *
	 * @param intAry
	 *            int陣列, int[]型別
	 * @return 每個陣列值呼叫toString方法後的陣列, String[]型別
	 */
	public static String[] toStringArray(int[] intAry) {
		if (intAry != null) {
			int aryLength = intAry.length;
			String[] strAry = new String[aryLength];
			for (int i = 0; i < aryLength; i++) {
				strAry[i] = String.valueOf(intAry[i]);
			}

			return strAry;
		}

		return new String[0];
	}

	/**
	 * 將long陣列轉換為字串陣列
	 * 
	 * @since 1.1
	 *
	 * @param longAry
	 *            long陣列, long[]型別
	 * @return 每個陣列值呼叫toString方法後的陣列, String[]型別
	 */
	public static String[] toStringArray(long[] longAry) {
		if (longAry != null) {
			int aryLength = longAry.length;
			String[] strAry = new String[aryLength];
			for (int i = 0; i < aryLength; i++) {
				strAry[i] = String.valueOf(longAry[i]);
			}

			return strAry;
		}

		return new String[0];
	}

	/**
	 * 將char陣列轉換為字串陣列
	 * 
	 * @since 1.1
	 *
	 * @param charAry
	 *            char陣列, char[]型別
	 * @return 每個陣列值呼叫toString方法後的陣列, String[]型別
	 */
	public static String[] toStringArray(char[] charAry) {
		if (charAry != null) {
			int aryLength = charAry.length;
			String[] strAry = new String[aryLength];
			for (int i = 0; i < aryLength; i++) {
				strAry[i] = String.valueOf(charAry[i]);
			}

			return strAry;
		}

		return new String[0];
	}

	/**
	 * 將float陣列轉換為字串陣列
	 * 
	 * @since 1.1
	 *
	 * @param floatAry
	 *            float陣列, float[]型別
	 * @return 每個陣列值呼叫toString方法後的陣列, String[]型別
	 */
	public static String[] toStringArray(float[] floatAry) {
		if (floatAry != null) {
			int aryLength = floatAry.length;
			String[] strAry = new String[aryLength];
			for (int i = 0; i < aryLength; i++) {
				strAry[i] = String.valueOf(floatAry[i]);
			}

			return strAry;
		}

		return new String[0];
	}

	/**
	 * 將double陣列轉換為字串陣列
	 * 
	 * @since 1.1
	 *
	 * @param doubleAry
	 *            double陣列, double[]型別
	 * @return 每個陣列值呼叫toString方法後的陣列, String[]型別
	 */
	public static String[] toStringArray(double[] doubleAry) {
		if (doubleAry != null) {
			int aryLength = doubleAry.length;
			String[] strAry = new String[aryLength];
			for (int i = 0; i < aryLength; i++) {
				strAry[i] = String.valueOf(doubleAry[i]);
			}

			return strAry;
		}

		return new String[0];
	}

	/**
	 * 將指定的Regext查找出的第一個字串做為資料群組的鍵值，將集合物件的元素做分群
	 * 
	 * @param regex
	 *            指定的正規表示式, String型別
	 * @param beanCollection
	 *            集合物件, Collection型別
	 * @return 分群後的資料集合, Map型別
	 */
	public static <E> Map<String, Collection<E>> group(String regex, Collection<E> beanCollection) {
		Map<String, Collection<E>> dataMap = new LinkedHashMap<>();
		Pattern pattern = Pattern.compile(regex);
		for (E element : beanCollection) {
			String elementStr = element.toString();
			Matcher matcher = pattern.matcher(elementStr);
			if (matcher.find()) {
				String key = matcher.group();
				if (dataMap.containsKey(key)) {
					Collection<E> elementList = dataMap.get(key);
					elementList.add(element);
					dataMap.put(key, elementList);
				} else {
					Collection<E> elementList = new ArrayList<>();
					elementList.add(element);
					dataMap.put(key, elementList);
				}
			}
		}

		return dataMap;
	}
	
	/**
	 * 	產生指定長度知任意字串
	 * @param length 指定長度, int型別
	 * @param chars 可能字符, char參數列表
	 * 
	 * @return 指定長度的隨機字符組合, String型別
	 */
	public static String randomString(int length, char... chars) {
		return RandomStringUtils.random(length, 0, chars.length - 1, false, false, chars, new SecureRandom());
	}
	
	/**
	 * [方法多載]
	 * 	支援採預設字元自原陣列; 產生指定長度知任意字串
	 * @param length 指定長度, int型別
	 * @return 指定長度的隨機字符組合, String型別
	 */
	public static String randomString(int length) {
		return randomString(length, CHARACTERS);
	}
	
	/**
	 * 將指定字串於傳入字串對像左側添加至指定長度
	 * 
	 * @param str 初始字串對像, String型別
	 * @param charSequence 添加字串, CharSequence型別 
	 * @param totalNum 總長度, int型別
	 * @since 1.2
	 * 
	 * @return 添加後字串, String型別
	 */
	public static String leftPad(String str, CharSequence charSequence, int totalNum) {
		int originLength = str.length();
		StringBuilder builder = new StringBuilder();
		while (originLength++ < totalNum) builder.append(charSequence);
		
		return builder.append(str).toString();
	}
	
	/**
	 * 將指定字串於傳入字串對像右側添加至指定長度
	 * 
	 * @param str 初始字串對像, String型別
	 * @param charSequence 添加字串, CharSequence型別 
	 * @param totalNum 總長度, int型別
	 * @since 1.2
	 * 
	 * @return 添加後字串, String型別
	 */
	public static String rightPad(String str, CharSequence charSequence, int totalNum) {
		int originLength = str.length();
		StringBuilder builder = new StringBuilder(str);
		while (originLength++ < totalNum) builder.append(charSequence);
		
		return builder.toString();
	}
	
	/**
     * 全型字串轉換半型字串
     * <pre>
     * 1. 半形字元是從33開始到126結束
     * 2. 與半形字元對應的全形字元是從65281開始到65374結束
     * 3. 其中半形的空格是32對應的全形空格是12288，除空格外的字元偏移量是65248(65281 - 33)
     * </pre>
     *
     * @param fullWidthStr 非空的全形字串
     * @since 1.3
     * 
     * @return 半型字串
     */
    public static String fullToHalf(String fullWidthStr) {
        if (Objects.isNull(fullWidthStr)) return null;
        char[] charArray = fullWidthStr.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            int charIntValue = charArray[i];
            if (charIntValue >= 65281 && charIntValue <= 65374) {
                charArray[i] = (char) (charIntValue - 65248);
            } else if (charIntValue == 12288) {
                charArray[i] = (char) 32;
            }
        }
        return new String(charArray);
    }
    
    /**
     * 半型字串轉換全型字串
     * <pre>
     * 1. 半形字元是從33開始到126結束
     * 2. 與半形字元對應的全形字元是從65281開始到65374結束
     * 3. 其中半形的空格是32對應的全形空格是12288，除空格外的字元偏移量是65248(65281 - 33)
     * </pre>
     *
     * @param fullWidthStr 非空的全形字串
     * @since 1.3
     * 
     * @return 全型字串
     */
    public static String halfToFull(String fullWidthStr) {
        if (Objects.isNull(fullWidthStr)) return null;
        char[] charArray = fullWidthStr.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
        	char charVal = charArray[i];
            if (charVal > '\200') continue; // Bypass Chinese character
            if (charVal == 32 || !Character.isLetterOrDigit(charVal)) {
                charArray[i] = (char) 12288;
            } else {
                charArray[i] = (char) (charVal + 65248);
            }
        }
        return new String(charArray);
    }
}
