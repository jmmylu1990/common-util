package org.common.util;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

/**
 * 建立時間: 下午1:30:40<br>
 *
 * @author Van
 * @version 1.0
 */
public class FileOperationUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FileOperationUtils.class);

	private static final String PROPERTIES_SUFFIX = ".properties";
	private static String defaultCharset = "UTF-8";
	private static String[] defaultCharsets = { "UTF-16LE", "UTF-16BE", "UTF-8", "Big5", "ISO-8859-1" };
	private static String[] sizeUnits = new String[] { "B", "KB", "MB", "GB", "TB" };

	private FileOperationUtils() {
	}

	/**
	 * 設定檔案輸出的預設編碼
	 *
	 * @param charset
	 *            檔案編碼, String型別
	 */
	public static void setFileCharset(String charset) {
		defaultCharset = charset;
	}

	/**
	 * 檢視指定的檔案是否符合指定的檔案格式
	 *
	 * @see File
	 * @param file
	 *            檔案, File型別
	 * @param formats
	 *            檔案格式ㄤ; 支援Regex表示式, String參數列表
	 * @return 是否符合指定的檔案格式, boolean型別
	 */
	public static boolean formatVerify(File file, String... formats) {
		String fileName = file.getName().toLowerCase();
		
		return formats.length == 0 || Stream.of(formats)
				.map(format -> ".+(\\." + format + ")$")
				.filter(fileName::matches).count() > 0;
	}

	/**
	 * 檢視指定的檔案是否符合指定的檔案格式
	 *
	 * @see File
	 * @param file
	 *            檔案完整路徑, String型別
	 * @param formats
	 *            檔案格式, String參數列表
	 * @return 是否符合指定的檔案格式, boolean型別
	 */
	public static boolean formatVerify(String file, String... formats) {
		return formatVerify(new File(file), formats);
	}

	/**
	 * 檢視傳入資料夾內是否有包含檔案(即空資料夾)
	 *
	 * @param dir
	 *            來源資料夾, File型別
	 * @return 回傳資料夾內容是否為空, boolean型別
	 */
	public static boolean isEmpty(File dir) {
		return dir != null && !ClassUtils.isValid(dir.list());
	}

	/**
	 * [方法多載]<br>
	 * 支援路徑參數; 檢視傳入資料夾內是否有包含檔案(即空資料夾)
	 *
	 * @param dir
	 *            來源資料夾, String型別
	 * @return 回傳資料夾內容是否為空, boolean型別
	 */
	public static boolean isEmpty(String dir) {
		return isEmpty(new File(dir));
	}

	/**
	 * 檢索指定的檔案名稱是否存在於指定的資料來源中<br>
	 * 若存在回傳該檔案; 反之回傳null
	 *
	 * @param resource
	 *            欲檢索的資料來源, File型別
	 * @param fileName
	 *            檔案名稱, String型別
	 * @return 檢索到的檔案, File型別
	 */
	public static File contains(File resource, String fileName) {
		List<File> fileList = traversalAllFiles(resource);
		for (File file : fileList) {
			if (file.getName().equals(fileName)) { return file; }
		}
		return null;
	}

	/**
	 * [方法多載]<br>
	 * 檢索指定的檔案名稱是否存在於指定的資料來源中<br>
	 * 若存在回傳該檔案; 反之回傳null
	 *
	 * @param resource
	 *            欲檢索的資料來源完整路徑, String型別
	 * @param fileName
	 *            檔案名稱, String型別
	 * @return 檢索到的檔案, File型別
	 */
	public static File contains(String resource, String fileName) {
		return contains(new File(resource), fileName);
	}

	/**
	 * 回傳從指定來源資料夾中, 索引到符合指定檔案名稱的檔案
	 *
	 * @see File
	 * @see Arrays#asList(Object...)
	 * @param resource
	 *            來源資料夾, File型別
	 * @param fileNames
	 *            欲檢索的檔案名稱, String參數列表
	 * @return 檔案List, List型別
	 */
	public static List<File> matches(File resource, String... fileNames) {
		List<File> matchedFileList = new ArrayList<>();
		List<String> fileNameList = Arrays.asList(fileNames);
		List<File> resourceFileList = traversalAllFiles(resource);
		for (File file : resourceFileList) {
			if (fileNameList.contains(file.getName())) matchedFileList.add(file);
		}
		return matchedFileList;
	}

	/**
	 * 回傳從指定來源資料夾中, 索引到符合指定檔案名稱的檔案
	 *
	 * @see File
	 * @see File#listFiles()
	 * @param resource
	 *            來源資料夾完整路徑, String型別
	 * @param fileNames
	 *            欲檢索的檔案名稱, String參數列表
	 * @return 檔案List, List型別
	 */
	public static List<File> matches(String resource, String... fileNames) {
		return matches(new File(resource), fileNames);
	}

	/**
	 * 將指定來源的資料夾完整刪除(包含裏頭巢狀資料結構)
	 *
	 * @see File
	 * @see File#delete()
	 * @param dir
	 *            欲刪除的來源資料夾, File型別
	 * @return 刪除成功與否, boolean型別
	 */
	public static boolean remove(File dir) {
		try {
			if (dir == null || !dir.exists()) return false;
			if (dir.isDirectory()) {
				File[] contents = dir.listFiles();
				if (contents == null) return false;
				for (File content : contents) {
					remove(content);
				}
			}
			
			return dir.delete();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		return false;
	}

	/**
	 * 將指定來源的資料夾完整刪除(包含裏頭巢狀資料結構)
	 *
	 * @see File
	 * @see File#delete()
	 * @param dir
	 *            欲刪除的來源資料夾, String型別
	 * @return 刪除成功與否, boolean型別
	 */
	public static boolean remove(String dir) {
		return remove(new File(dir));
	}

	/**
	 * 刪除(複數)指定路徑的檔案或資料夾
	 *
	 * @param files
	 *            複數檔案路徑, File...型別
	 * @return 是否刪除成功, boolean型別
	 */
	public static boolean removeMultiple(File... files) {
		boolean isDeleted = false;
		for (File file : files) {
			isDeleted = remove(file);
		}

		return isDeleted;
	}

	/**
	 * [方法多載]<br>
	 * 檔案路徑支援字串型態; 刪除(複數)指定路徑的檔案或資料夾
	 *
	 * @param files
	 *            複數檔案路徑, String...型別
	 * @return 是否刪除成功, boolean型別
	 */
	public static boolean removeMultiple(String... files) {
		boolean isDeleted = false;
		for (String file : files) {
			isDeleted = remove(file);
		}

		return isDeleted;
	}

	/**
	 * 將指定來源移動到指定路徑<br>
	 *
	 * @see File
	 * @see File#renameTo(File)
	 * @param resource
	 *            來源(可以是資料夾或檔案), File型別
	 * @param destDir
	 *            於移動到的路徑, File型別
	 * @return 移動成功與否, boolean型別
	 */
	public static boolean move(File resource, File destDir) {
		boolean isMoved = false;
		// 如果目的地資料夾結構不存在, 則創建
		if (destDir.exists()) destDir.mkdirs();
		String destFileName = destDir.getPath() + File.separator + resource.getName();
		// 如果來源為資料夾, 則開始遍歷其中檔案並移動; 反之來源為檔案, 則直接移動
		if (resource.isDirectory()) {
			File[] files = resource.listFiles();
			if (files == null) return false;
			for (File file : files) {
				move(file.getPath(), destFileName);
			}
		} else {
			isMoved = resource.renameTo(new File(destFileName));
		}
		return isMoved && FileOperationUtils.remove(resource);
	}

	/**
	 * [方法多載]<br>
	 * 支援傳入字串參數; 將指定來源移動到指定路徑<br>
	 *
	 * @see File
	 * @see File#renameTo(File)
	 * @param resource
	 *            來源(可以是資料夾或檔案), File型別
	 * @param destDir
	 *            於移動到的路徑, File型別
	 * @return 移動成功與否, boolean型別
	 */
	public static boolean move(String resource, String destDir) {
		return move(new File(resource), new File(destDir));
	}

	/**
	 * 回傳該文件檔案的實際資料行數
	 *
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @param file
	 *            欲計算行數的文件檔案, File型別
	 * @return 資料行數, long型別
	 */
	public static long getLineCount(File file) {
		long totalLineCount = 0L;
		if (file == null) return totalLineCount;
		
		try (BufferedReader in = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = in.readLine()) != null) {
				if (ClassUtils.isValid(line)) totalLineCount++;
			}
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return totalLineCount;
	}
	
	/**
	 * 利用指定的編碼列表, 檢測檔案正確編碼
	 * 
	 * @param file
	 *            檔案物件, File型別
	 * @param charsets
	 *            欲檢查的編碼參數列表, String...型別
	 * @return 檔案的編碼, Charset型別
	 */
	public static Charset detectCharset(File file, String... charsets) {
		if (charsets.length == 0) charsets = defaultCharsets;
		Charset charset = Charset.forName(defaultCharset);
		for (String charsetName : charsets) {
			charset = detectCharset(file, Charset.forName(charsetName));
			if (Objects.nonNull(charset)) return charset;
		}

		return charset;
	}

	/**
	 * [方法多載]<br>
	 * 支援字串路徑參數, 利用指定的編碼列表, 檢測檔案正確編碼
	 * 
	 * @param file
	 *            檔案物件, File型別
	 * @param charsets
	 *            欲檢查的編碼參數列表, String...型別
	 * @return 檔案的編碼, Charset型別
	 */
	public static Charset detectCharset(String file, String... charsets) {
		return detectCharset(new File(file), charsets);
	}

	private static Charset detectCharset(File file, Charset charset) {
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
			CharsetDecoder decoder = charset.newDecoder();
			decoder.reset();
			byte[] buffer = new byte[2048];
			boolean identified = false;
			while ((in.read(buffer) != -1) && (!identified)) {
				identified = identify(buffer, decoder);
			}

			if (identified) {
				return charset;
			} else {
				return null;
			}

		} catch (Exception e) {
			return null;
		}
	}

	private static boolean identify(byte[] bytes, CharsetDecoder decoder) {
		try {
			decoder.decode(ByteBuffer.wrap(bytes));
		} catch (CharacterCodingException e) {
			return false;
		}
		return true;
	}

	/**
	 * 將一個檔案依照指定的編碼重新編碼後輸出, 若輸出檔名與原始檔案同名則覆蓋
	 *
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 *
	 * @param file
	 *            欲改變編碼檔案, File型別
	 * @param orignCharset
	 *            原始檔案的編碼, Sting型別
	 * @param charset
	 *            輸出檔案的編碼, Sting型別
	 * @param outputFileName
	 *            輸出檔案名稱, String型別
	 * @return 重新編碼後的檔案, File型別
	 */
	public static File charsetAsign(File file, String orignCharset, String charset, String outputFileName) {
		String filePath = file.getParent();
		String fileName = file.getName();
		outputFileName = fileName.equals(outputFileName) ? outputFileName + ".tmp" : outputFileName;
		File outputFile = new File(filePath, outputFileName);
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), orignCharset));
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), charset))) {
			String line;
			while ((line = in.readLine()) != null) {
				out.write(line);
				out.newLine();
			}
			out.flush();
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return (outputFileName.endsWith(".tmp") && remove(file)) ? renameExistsFile(outputFile, fileName) : outputFile;
	}

	/**
	 * [方法多載]<br>
	 * 支援字串路徑; 將一個檔案依照指定的編碼重新編碼後輸出, 若輸出檔名與原始檔案同名則覆蓋
	 *
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 *
	 * @param file
	 *            欲改變編碼檔案, String型別
	 * @param orignCharset
	 *            原始檔案的編碼, Sting型別
	 * @param charset
	 *            輸出檔案的編碼, Sting型別
	 * @param outputFileName
	 *            輸出檔案名稱, String型別
	 * @return 重新編碼後的檔案, File型別
	 */
	public static File charsetAsign(String file, String orignCharset, String charset, String outputFileName) {
		return charsetAsign(new File(file), orignCharset, charset, outputFileName);
	}

	/**
	 * [方法多載]<br>
	 * 預設檔案原始編碼為UTF-8; 將一個檔案依照指定的編碼重新編碼後輸出, 若輸出檔名與原始檔案同名則覆蓋
	 *
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 *
	 * @param file
	 *            欲改變編碼檔案, File型別
	 * @param charset
	 *            輸出檔案的編碼, Sting型別
	 * @param outputFileName
	 *            輸出檔案名稱, String型別
	 * @return 重新編碼後的檔案, File型別
	 */
	public static File charsetAsign(File file, String charset, String outputFileName) {
		Charset originCharset = detectCharset(file);
		LOGGER.debug("[{}] detect charset: {}", file, originCharset);
		return Objects.nonNull(originCharset) ? charsetAsign(file, originCharset.name(), charset, outputFileName) : null;
	}

	/**
	 * [方法多載]<br>
	 * 支援字串路徑及預設檔案原始編碼為UTF-8; 將一個檔案依照指定的編碼重新編碼後輸出, 若輸出檔名與原始檔案同名則覆蓋
	 *
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 *
	 * @param file
	 *            欲改變編碼檔案, String型別
	 *            原始檔案的編碼, Sting型別
	 * @param charset
	 *            輸出檔案的編碼, Sting型別
	 * @param outputFileName
	 *            輸出檔案名稱, String型別
	 * @return 重新編碼後的檔案, File型別
	 */
	public static File charsetAsign(String file, String charset, String outputFileName) {
		return charsetAsign(new File(file), charset, outputFileName);
	}
	
	/**
	 * 將檔案串流轉換成指定編碼的文字
	 *
	 * @see StringBuilder
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 *            檔案串流, InputStream型別
	 *            來源檔案的編碼, String型別
	 * @return 提取內容, String型別
	 */
	public static String extractContent(InputStream inStream, String charset) {
		StringBuilder sb = new StringBuilder();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(inStream, charset))) {
			String line;
			while ((line = in.readLine()) != null) {
				sb.append(line).append(StringTools.LF);
			}
		} catch (IOException e) {
			LOGGER.info(e.getMessage(), e);
		}

		return sb.toString();
	}

	/**
	 * 提取指定檔案中的文本內容
	 *
	 * @see StringBuilder
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @param resource
	 *            欲提取內容的檔案, File型別
	 *            來源檔案的編碼, String型別
	 * @return 提取內容, String型別
	 */
	public static String extractContent(File resource, String charset) {
		try {
			return extractContent(new FileInputStream(resource), charset);
		} catch (FileNotFoundException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return null;
	}

	/**
	 * [方法多載]<br>
	 * 將檔案串流轉換成指定編碼的文字
	 *
	 * @see StringBuilder
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 *            欲提取內容的檔案, File型別
	 * @param lineCount
	 *            擷取檔案行數, int型別
	 *            來源檔案的編碼, String型別
	 * @return 提取內容, String型別
	 */
	public static String extractContent(InputStream inStream, int lineCount, String charset) {
		StringBuilder sb = new StringBuilder();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(inStream, charset))) {
			String line;
			int lineIndex = 1;
			while ((line = in.readLine()) != null) {
				if (lineIndex++ > lineCount) break;
				sb.append(line).append(StringTools.LF);
			}
		} catch (IOException e) {
			LOGGER.info(e.getMessage(), e);
		}

		return sb.toString();
	}
	
	/**
	 * [方法多載]<br>
	 * 支援字串路徑參數; 提取指定檔案中的前n行文本內容
	 *
	 * @see StringBuilder
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @param resource
	 *            欲提取內容的檔案, String型別
	 * @param lineCount
	 *            擷取檔案行數, int型別
	 *            來源檔案的編碼, String型別
	 * @return 提取內容, String型別
	 */
	public static String extractContent(File resource, int lineCount, String charset) {
		try {
			return extractContent(new FileInputStream(resource), lineCount, charset);
		} catch (FileNotFoundException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return null;
	}

	/**
	 * [方法多載]<br>
	 * 支援字串路徑參數; 提取指定檔案中的文本內容
	 *
	 * @see StringBuilder
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @param resource
	 *            欲提取內容的檔案, String型別
	 *            來源檔案的編碼, String型別
	 * @return 提取內容, String型別
	 */
	public static String extractContent(String resource, String charset) {
		return extractContent(new File(resource), charset);
	}

	/**
	 * [方法多載]<br>
	 * 支援字串路徑參數及提供預設編碼(UTF-8); 將檔案串流轉換成UTF-8文字
	 *
	 * @see StringBuilder
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 *            欲提取內容的檔案, String型別
	 * @return 提取內容, String型別
	 */
	public static String extractContent(InputStream in) {
		return extractContent(in, defaultCharset);
	}
	
	/**
	 * [方法多載]<br>
	 * 支援字串路徑參數及編碼識別; 提取指定檔案中的文本內容
	 *
	 * @see StringBuilder
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @param resource
	 *            欲提取內容的檔案, String型別
	 * @return 提取內容, String型別
	 */
	public static String extractContent(String resource) {
		return extractContent(new File(resource));
	}

	/**
	 * [方法多載]<br>
	 * 支援字串路徑參數及提供預設編碼(UTF-8); 提取指定檔案中的文本內容
	 *
	 * @see StringBuilder
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @param resource
	 *            欲提取內容的檔案, File型別
	 * @return 提取內容, String型別
	 */
	public static String extractContent(File resource) {
		Charset charset = detectCharset(resource);
		LOGGER.debug("[{}] detect charset: {}", resource, charset);
		return Objects.nonNull(charset) ? extractContent(resource, charset.toString()) : null;
	}

	/**
	 * [方法多載]<br>
	 * 利用regex找出所有符合的內容, 並以指定的內容覆寫
	 *
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @param file
	 *            來源檔案, String型別
	 * @param pattern
	 *            要變更的字串pattern(regex), String型別
	 * @param editContent
	 *            覆寫符合pattern的內容
	 * @return 變更內容後的檔案, File型別
	 */
	public static File replaceContentByPattern(File file, String pattern, String editContent) {
		String orignFileName = file.getName();
		File tempFile = new File(file.getParent(), orignFileName + ".tmp");
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile)))) {
			String line;
			while ((line = in.readLine()) != null && ClassUtils.isValid(editContent)) {
				out.write(line.replaceAll(pattern, editContent));
				out.newLine();
			}
			out.flush();
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return remove(file) ? renameExistsFile(tempFile, orignFileName) : null;
	}

	/**
	 * 利用regex找出所有符合的內容, 並以指定的內容覆寫
	 *
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @param file
	 *            來源檔案, File型別
	 * @param pattern
	 *            要變更的字串pattern(regex), String型別
	 * @param editContent
	 *            覆寫符合pattern的內容
	 * @return 變更內容後的檔案, File型別
	 */
	public static File replaceContentByPattern(String file, String pattern, String editContent) {
		return replaceContentByPattern(new File(file), pattern, editContent);
	}

	/**
	 * 將指定內容覆寫進指定的檔案
	 *
	 * @see File
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @param file
	 *            欲變更內容的檔案, File型別
	 * @param data
	 *            變更後的內容, CharSequence型別
	 * @return 變更內容後的檔案, File型別
	 */
	public static File replaceContent(File file, CharSequence data) {
		try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
			if (data != null) {
				String content = data.toString();
				out.write(content);
				out.flush();
			}
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return file;
	}

	/**
	 * [方法多載]<br>
	 * 將指定內容覆寫進指定的檔案
	 *
	 * @see File
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @param file
	 *            欲變更內容的檔案, String型別
	 * @param data
	 *            變更後的內容, CharSequence型別
	 * @return 變更內容後的檔案, File型別
	 */
	public static File replaceContent(String file, CharSequence data) {
		return replaceContent(new File(file), data);
	}

	/**
	 * 讀取來源檔案(單一檔案)並輸出內容至另一個檔案
	 *
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @param resource
	 *            來源檔案, File型別
	 * @param outputFile
	 *            輸出檔案, File型別
	 * @param appenders
	 *            欲附加的內容, String參數列表
	 * @return 回傳輸出的檔案
	 */
	private static File readContents(File resource, File outputFile, String... appenders) {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(resource)));
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, true)))) {
			String line;
			while ((line = in.readLine()) != null) {
				if (appenders.length > 0) {
					for (String appender : appenders) {
						out.write(line);
						out.write(appender);
					}
				} else {
					out.write(line);
				}
				out.newLine();
			}
			out.flush();
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return outputFile;
	}

	/**
	 * 將指定資料下符合指定檔案格式的檔案內容合併成單一檔案
	 *
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @param resource
	 *            資料來源(資料夾), File型別
	 * @param outputFile
	 *            輸出檔案, File型別
	 * @param appenders
	 *            欲附加的內容, String參數列表
	 * @return 合併內容後的文件檔
	 */
	public static File mergeContents(File resource, File outputFile, String... appenders) {
		if (resource.isDirectory() && outputFile != null) {
			Stream.of(resource.listFiles()).forEach(file -> mergeContents(file, outputFile, appenders));
		} else {
			readContents(resource, outputFile, appenders);
		}
		LOGGER.info("Resource: [Merge Contents] {}.\nFile: {} is created.\nMerge contents complete!", resource, outputFile);

		return outputFile;
	}

	/**
	 * [方法多載]<br>
	 * 將指定資料下符合指定檔案格式的檔案內容合併成單一檔案
	 *
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @param resource
	 *            資料來源(資料夾)的完整路徑, String型別
	 * @param outputFile
	 *            輸出檔案的完整路徑, String型別
	 * @param appenders
	 *            欲附加的內容, String參數列表
	 * @return 合併內容後的文件檔
	 */
	public static File mergeContents(String resource, String outputFile, String... appenders) {
		return mergeContents(new File(resource), new File(outputFile), appenders);
	}

	/**
	 * 將指定TDCS資料下符合指定檔案格式的檔案內容合併成單一檔案
	 *
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @param tdcsResource
	 *            資料來源(資料夾)的完整路徑, File型別
	 * @param outputFile
	 *            輸出檔案的完整路徑, String型別
	 * @param compress
	 *            是否輸出為壓縮檔, boolean型別
	 * @return 合併內容後的文件檔
	 */
	public static File mergeTDCSFiles(File tdcsResource, File outputFile, boolean compress) {
		return mergeTDCSFiles(tdcsResource.getAbsolutePath(), outputFile.getAbsolutePath(), compress);
	}

	/**
	 * [方法多載]<br>
	 * 將指定TDCS資料下符合指定檔案格式的檔案內容合併成單一檔案
	 *
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @param tdcsResource
	 *            資料來源(資料夾)的完整路徑, String型別
	 * @param outputFile
	 *            輸出檔案的完整路徑, String型別
	 * @param compress
	 *            是否輸出為壓縮檔, boolean型別
	 * @return 合併內容後的文件檔
	 */
	public static File mergeTDCSFiles(String tdcsResource, String outputFile, boolean compress) {
		String suffix = compress ? ".gz" : "";
		File resource = new File(tdcsResource);
		File tdcsFile = new File(outputFile + suffix);
		if (!tdcsFile.getParentFile().exists()) tdcsFile.getParentFile().mkdirs();
		String updateTime = DateUtils.formatDateToStr(new Date());
		String infoDate = null;
		if (resource.isDirectory()) {
			try (FileOutputStream fos = new FileOutputStream(tdcsFile, true);
					OutputStream out = (!compress) ? new BufferedOutputStream(fos) : new GZIPOutputStream(fos) {
						{
							def.setLevel(Deflater.BEST_COMPRESSION);
						}
					};) {
				List<File> fileList = FileOperationUtils.traversalAllFiles(resource, "csv");
				for (File file : fileList) {
					try (BufferedReader in = new BufferedReader(new FileReader(file))) {
						String line;
						while ((line = in.readLine()) != null) {
							StringBuilder sb = new StringBuilder();
							String[] fileNameSegments = file.getName().split("_|\\.");
							int commaIndex = line.indexOf(',');
							if (commaIndex > 0) {
								String orignTime = line.substring(0, commaIndex);
								String infoTime = DateUtils.formatDateToStr(DateUtils.parseStrToDate(fileNameSegments[2] + fileNameSegments[3]));
								if (orignTime.matches(DateUtils.DATE_SIMILAR_REGEX)) line = line.replace(orignTime, infoTime);
								infoDate = infoTime.substring(0, infoTime.indexOf(' '));
								sb.append(line).append(",nfb,");
								sb.append(infoTime).append(",");
								sb.append(updateTime).append(",");
								sb.append(infoTime).append(",");
								sb.append(infoDate);
								out.write(sb.append("\r\n").toString().getBytes(defaultCharset));
							}
						}
					}
				}
				LOGGER.info("[{}] merge completely!", tdcsResource);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
			remove(resource);
		}
		return tdcsFile;
	}

	/**
	 * [方法多載]<br>
	 * M06A專用, 將TDCS資料下符合指定檔案格式的檔案內容合併成單一檔案; 支援檔案壓縮
	 *
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @param tdcsResource
	 *            資料來源(資料夾)的完整路徑, String型別
	 * @param outputFile
	 *            輸出檔案的完整路徑, String型別
	 * @param compress
	 *            是否輸出為壓縮檔, boolean型別
	 * @return 合併內容後的文件檔
	 */
	public static List<File> mergeTDCSM06Files(File tdcsResource, File outputFile, boolean compress) {
		return mergeTDCSM06Files(tdcsResource.getAbsolutePath(), outputFile.getPath(), compress);
	}

	/**
	 * [方法多載]<br>
	 * M06A專用, 將TDCS資料下符合指定檔案格式的檔案內容合併成單一檔案; 支援檔案壓縮
	 *
	 * @see File
	 * @see BufferedReader
	 * @see InputStreamReader
	 * @see FileInputStream
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @param tdcsResource
	 *            資料來源(資料夾)的完整路徑, String型別
	 * @param outputFile
	 *            輸出檔案的完整路徑, String型別
	 * @param compress
	 *            是否輸出為壓縮檔, boolean型別
	 * @return 合併內容後的文件檔
	 */
	public static List<File> mergeTDCSM06Files(String tdcsResource, String outputFile, boolean compress) {
		File resource = new File(tdcsResource);
		File tdcsFileMaster = new File(compress ? outputFile + ".gz" : outputFile);
		outputFile = outputFile.replace("Master", "Detail").replace("master", "detail");
		File tdcsFileDetail = new File(compress ? outputFile + ".gz" : outputFile);
		List<File> outputFileList = new ArrayList<>();
		if (!tdcsFileMaster.getParentFile().exists()) tdcsFileMaster.getParentFile().mkdirs();
		if (!tdcsFileDetail.getParentFile().exists()) tdcsFileDetail.getParentFile().mkdirs();
		String updateTime = DateUtils.formatDateToStr(new Date());
		if (resource.isDirectory()) {
			try (FileOutputStream fosMaster = new FileOutputStream(tdcsFileMaster, true);
					OutputStream outMaster = (!compress) ? new BufferedOutputStream(fosMaster) : new GZIPOutputStream(fosMaster) {
						{
							def.setLevel(Deflater.BEST_COMPRESSION);
						}
					};
					FileOutputStream fosDetail = new FileOutputStream(tdcsFileDetail, true);
					OutputStream outDetail = (!compress) ? new BufferedOutputStream(fosDetail) : new GZIPOutputStream(fosDetail) {
						{
							def.setLevel(Deflater.BEST_COMPRESSION);
						}
					}) {
				List<File> files = FileOperationUtils.traversalAllFiles(resource, "csv");
				if (files == null) return new ArrayList<>();
				for (File file : files) {
					if (file.isFile()) {
						try (BufferedReader in = new BufferedReader(new FileReader(file))) {
							String line;
							while ((line = in.readLine()) != null) {
								int lastCommaIndex = line.lastIndexOf(',');
								int serialNo = 1;
								String mainInfo = line;
								StringBuilder sbMaster = new StringBuilder();
								String uuid = UUID.randomUUID().toString();
								String vehicleType = line.substring(0, line.indexOf(','));
								String tripInformation = line.substring(lastCommaIndex + 1);
								String[] tripInfos = tripInformation.split(";\\s");
								String[] fileNameSegments = file.getName().split("_|\\.");
								String infoTime = DateUtils.formatDateToStr(DateUtils.parseStrToDate(fileNameSegments[2] + fileNameSegments[3]));
								String infoDate = infoTime.substring(0, infoTime.indexOf(' '));

								int tripInfoSize = tripInfos.length - 1;
								tripInfoSize = (tripInfoSize > 0) ? tripInfoSize : 1;
								for (int i = 0; i < tripInfoSize; i++) {
									StringBuilder sbDetail = new StringBuilder();
									String[] startDetails = tripInfos[i].split("\\+");
									String[] endDetails = tripInfos[(tripInfoSize == 1) ? 0 : i + 1].split("\\+");
									for (int j = 0; j < startDetails.length; j += 2) {
										sbDetail.append(uuid).append(",").append(serialNo++).append(",")
												.append(vehicleType).append(",").append(startDetails[0]).append(",").append(startDetails[1])
												.append(",").append(endDetails[0]).append(",").append(endDetails[1]);
										sbDetail.append(",nfb,");
										sbDetail.append(infoTime).append(",");
										sbDetail.append(updateTime).append(",");
										sbDetail.append(infoTime).append(",");
										sbDetail.append(infoDate);

										outDetail.write(sbDetail.append("\r\n").toString().getBytes(defaultCharset));
									}
								}

								sbMaster.append(uuid).append(",").append(mainInfo).append(",nfb,");
								sbMaster.append(infoTime).append(",");
								sbMaster.append(updateTime).append(",");
								sbMaster.append(infoTime).append(",");
								sbMaster.append(infoDate);

								outMaster.write(sbMaster.append("\r\n").toString().getBytes(defaultCharset));
							}
							outDetail.flush();
							outMaster.flush();
						}
					}
				}
				outputFileList.add(tdcsFileMaster);
				outputFileList.add(tdcsFileDetail);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
			remove(resource);
		}
		return outputFileList;
	}

	/**
	 * 將字串內容輸出成一個檔案
	 *
	 * @see StringBuilder
	 * @see File
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @see FileOperationUtils#renameExistsFile(File, String)
	 * @param outputFile
	 *            輸出的檔案, File型別
	 * @param data
	 *            欲輸出的內容, CharSequence型別
	 * @param charset
	 *            輸出的檔案編碼, String型別
	 * @return 輸出的檔案, File型別
	 */
	public static File generateTextFile(File outputFile, CharSequence data, String charset) {
		return generateTextFile(outputFile, data, charset, false);
	}

	/**
	 * [方法多載]<br>
	 * 支援檔案覆寫; 將字串內容輸出成一個檔案
	 *
	 * @see StringBuilder
	 * @see File
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @see FileOperationUtils#renameExistsFile(File, String)
	 * @param outputFile
	 *            輸出的檔案, File型別
	 * @param data
	 *            欲輸出的內容, CharSequence型別
	 * @param charset
	 *            輸出的檔案編碼, String型別
	 * @param override
	 *            若檔案已存在是否覆蓋原檔, boolean型別
	 * @return 輸出的檔案, File型別
	 */
	public static File generateTextFile(File outputFile, CharSequence data, String charset, boolean override) {
		String dataContent = data.toString();
		if (outputFile.exists() && !override) return null;
		File parentFile = outputFile.getParentFile();
		if (!parentFile.exists()) parentFile.mkdirs();
		try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, !override), charset))) {
			// If the data is not empty, then flush the data to target file
			if (ClassUtils.isValid(dataContent)) {
				out.write(dataContent);
				out.flush();
			}
			LOGGER.debug("File: {} is created.", outputFile);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return outputFile;
	}

	/**
	 * [方法多載]<br>
	 * 支援字串路徑參數; 將字串內容輸出成一個檔案
	 *
	 * @see StringBuilder
	 * @see File
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @see FileOperationUtils#renameExistsFile(File, String)
	 * @param outputFile
	 *            輸出的檔案, String型別
	 * @param data
	 *            欲輸出的內容, CharSequence型別
	 * @param charset
	 *            輸出的檔案編碼, String型別
	 * @return 輸出的檔案, File型別
	 */
	public static File generateTextFile(String outputFile, CharSequence data, String charset) {
		return generateTextFile(new File(outputFile), data, charset);
	}

	/**
	 * [方法多載]<br>
	 * 支援字串路徑參數; 將字串內容輸出成一個檔案
	 *
	 * @see StringBuilder
	 * @see File
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @see FileOperationUtils#renameExistsFile(File, String)
	 * @param outputFile
	 *            輸出的檔案, String型別
	 * @param data
	 *            欲輸出的內容, CharSequence型別
	 * @param charset
	 *            輸出的檔案編碼, String型別
	 * @param override
	 *            若檔案已存在是否覆蓋原檔, boolean型別
	 * @return 輸出的檔案, File型別
	 */
	public static File generateTextFile(String outputFile, CharSequence data, String charset, boolean override) {
		return generateTextFile(new File(outputFile), data, charset, override);
	}

	/**
	 * [方法多載]<br>
	 * 設定預設編碼為`UTF-8`; 將字串內容輸出成一個檔案
	 *
	 * @see StringBuilder
	 * @see File
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @see FileOperationUtils#renameExistsFile(File, String)
	 * @param outputFile
	 *            輸出的檔案, File型別
	 * @param data
	 *            欲輸出的內容, CharSequence型別
	 * @return 輸出的檔案, File型別
	 */
	public static File generateTextFile(File outputFile, CharSequence data) {
		return generateTextFile(outputFile, data, defaultCharset);
	}

	/**
	 * [方法多載]<br>
	 * 設定預設編碼為`UTF-8`; 且支援字串路徑; 將字串內容輸出成一個檔案
	 *
	 * @see StringBuilder
	 * @see File
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @see FileOperationUtils#renameExistsFile(File, String)
	 * @param outputFile
	 *            輸出的檔案, String型別
	 * @param data
	 *            欲輸出的內容, CharSequence型別
	 * @return 輸出的檔案, File型別
	 */
	public static File generateTextFile(String outputFile, CharSequence data) {
		return generateTextFile(new File(outputFile), data, defaultCharset);
	}

	/**
	 * [方法多載]<br>
	 * 設定預設編碼為`UTF-8`; 且支援字串路徑; 將字串內容輸出成一個檔案
	 *
	 * @see StringBuilder
	 * @see File
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @see FileOperationUtils#renameExistsFile(File, String)
	 * @param outputFile
	 *            輸出的檔案, String型別
	 * @param data
	 *            欲輸出的內容, CharSequence型別
	 * @param override
	 *            若檔案已存在是否覆蓋原檔, boolean型別
	 * @return 輸出的檔案, File型別
	 */
	public static File generateTextFile(String outputFile, CharSequence data, boolean override) {
		return generateTextFile(new File(outputFile), data, defaultCharset, override);
	}

	/**
	 * [方法多載]<br>
	 * 設定預設編碼為`UTF-8`;支援檔案覆寫; 將字串內容輸出成一個檔案
	 *
	 * @see StringBuilder
	 * @see File
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 * @see FileOperationUtils#renameExistsFile(File, String)
	 * @param outputFile
	 *            輸出的檔案, File型別
	 * @param data
	 *            欲輸出的內容, CharSequence型別
	 * @param override
	 *            若檔案已存在是否覆蓋原檔, boolean型別
	 * @return 輸出的檔案, File型別
	 */
	public static File generateTextFile(File outputFile, CharSequence data, boolean override) {
		return generateTextFile(outputFile, data, defaultCharset, override);
	}

	/**
	 * 將指定內容添加至指定檔案後方
	 *
	 * @see File
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 *
	 * @param file
	 *            欲添加內容的檔案, File型別
	 * @param data
	 *            欲添加的內容, CharSequence型別
	 * @param charset
	 *            欲添加的內容編碼, String型別
	 * @return 添加完後的檔案, File型別
	 */
	public static File append(File file, CharSequence data, String charset) {
		if (!file.exists()) return generateTextFile(file, data, charset);
		try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), charset))) {
			out.write(data.toString());
			out.flush();
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return file;
	}

	/**
	 * [方法多載]<br>
	 * 支援字串路徑; 將指定內容添加至指定檔案後方
	 *
	 * @see File
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 *
	 * @param file
	 *            欲添加內容的檔案, File型別
	 * @param data
	 *            欲添加的內容, CharSequence型別
	 * @param charset
	 *            欲添加的內容編碼, String型別
	 * @return 添加完後的檔案, File型別
	 */
	public static File append(String file, CharSequence data, String charset) {
		return append(new File(file), data, charset);
	}

	/**
	 * [方法多載]<br>
	 * 採用預設編碼添加; 將指定內容添加至指定檔案後方
	 *
	 * @see File
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 *
	 * @param file
	 *            欲添加內容的檔案, File型別
	 * @param data
	 *            欲添加的內容, CharSequence型別
	 * @return 添加完後的檔案, File型別
	 */
	public static File append(File file, CharSequence data) {
		return append(file, data, defaultCharset);
	}

	/**
	 * [方法多載]<br>
	 * 支援字串路徑, 將指定內容添加至指定檔案後方
	 *
	 * @see File
	 * @see BufferedWriter
	 * @see OutputStreamWriter
	 * @see FileOutputStream
	 *
	 * @param file
	 *            欲添加內容的檔案, String型別
	 * @param data
	 *            欲添加的內容, CharSequence型別
	 * @return 添加完後的檔案, File型別
	 */
	public static File append(String file, CharSequence data) {
		return append(new File(file), data);
	}

	/**
	 * 將指定的檔案更命, 若檔案名稱已存在, 則會自動在已存在的檔案名稱後頭添加_bak_%d<br>
	 * Ex:<br>
	 * 2017-05-01.xml => 2017-05-01_bak_1.xml
	 *
	 * @see String#format(String, Object...)
	 * @see String#replaceAll(String, String)
	 * @see File
	 * @see File#renameTo(File)
	 * @param file
	 *            欲更名的檔案, File型別
	 * @param newName
	 *            新的檔案名稱, String型別
	 * @return 更名後的檔案, File型別
	 */
	public static File renameExistsFile(File file, String newName) {
		if (file == null || !file.exists()) {
			LOGGER.info("File is null or not exist!");
			return null;
		}
		String tempName = newName;
		String path = file.getParent();
		File finalFile = new File(path, newName);
		// Verify that the file name is duplicated or not
		for (int i = 1; new File(path, tempName).exists(); i++) {
			LOGGER.info("Existed File: {}", tempName);
			// Replace the bak index suffix if it existed
			tempName = tempName.replaceFirst("(_bak\\d)?(?=\\.)", "_bak" + i);
			if (tempName.equals(newName)) break;
		}
		new File(path, newName).renameTo(new File(path, tempName));
		
		return file.renameTo(finalFile) ? finalFile : null;

	}

	/**
	 * [方法多載]<br>
	 * 將指定的檔案更命, 若檔案名稱已存在, 則會自動在已存在的檔案名稱後頭添加_bak_%d<br>
	 * Ex:<br>
	 * 2017-05-01.xml => 2017-05-01_bak_1.xml
	 *
	 * @see String#format(String, Object...)
	 * @see String#replaceAll(String, String)
	 * @see File
	 * @see File#renameTo(File)
	 * @param file
	 *            欲更名的檔案的完整路徑, String型別
	 * @param newName
	 *            新的檔案名稱, String型別
	 * @return 更名後的檔案, File型別
	 */
	public static File renameExistsFile(String file, String newName) {
		return renameExistsFile(new File(file), newName);
	}

	/**
	 * 取得指定類別檔下相對應的properties file<br>
	 * 可以省略副檔名, 預設抓取*.properties格式的檔案
	 *
	 * @see Class
	 * @see Class#getResourceAsStream(String)
	 * @see Properties
	 * @param beanClass
	 *            指定類別, Class型別
	 * @param propertiesFileName
	 *            properties檔案名稱, String型別
	 * @return Properties物件, Properties型別
	 */
	public static Properties getProperties(Class<?> beanClass, String propertiesFileName) {
		Properties properties = new Properties();
		try {
			propertiesFileName = propertiesFileName.endsWith(PROPERTIES_SUFFIX) ? propertiesFileName : propertiesFileName + PROPERTIES_SUFFIX;
			properties.load(beanClass.getResourceAsStream(propertiesFileName));
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return properties;
	}

	/**
	 * [方法多載]<br>
	 * 取得resources下相對應的properties file<br>
	 * 可以省略副檔名, 預設抓取*.properties格式的檔案
	 *
	 * @see Class
	 * @see Class#getResourceAsStream(String)
	 * @see Properties
	 *            指定類別, Class型別
	 * @param propertiesFileName
	 *            properties檔案名稱, String型別
	 * @return Properties物件, Properties型別
	 */
	public static Properties getProperties(String propertiesFileName) {
		propertiesFileName = propertiesFileName.endsWith(PROPERTIES_SUFFIX) ? propertiesFileName : propertiesFileName + PROPERTIES_SUFFIX;
		Properties properties = new Properties();
		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(propertiesFileName.replaceAll("^/", ""))) {
			properties.load(in);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return properties;
	}

	/**
	 * 取得指定類別檔下相對應的properties file並將指定的key/value一併寫入<br>
	 * 可以省略副檔名, 預設抓取*.properties格式的檔案
	 *
	 * @see Class
	 * @see Class#getResourceAsStream(String)
	 * @see Properties
	 *            指定類別, Class型別
	 * @param propertiesFileName
	 *            properties檔案名稱, String型別
	 * @param key
	 *            資料鍵值, String型別
	 * @param value
	 *            相對於鍵值的對應值, Object型別
	 * @return Properties物件, Properties型別
	 */
	public static Properties setProperties(String propertiesFileName, String key, Object value) {
		propertiesFileName = propertiesFileName.endsWith(PROPERTIES_SUFFIX) ? propertiesFileName : propertiesFileName + PROPERTIES_SUFFIX;
		String propPath = Thread.currentThread().getContextClassLoader().getResource(propertiesFileName).getPath();
		Properties properties = getProperties(propertiesFileName);
		try (OutputStream out = new FileOutputStream(propPath)) {
			properties.setProperty(key, value.toString());
			properties.store(out, null);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return properties;
	}

	/**
	 * 遍歷指定資料夾底下所有的檔案, 並回傳存放於List後回傳
	 *
	 * @see List
	 * @see File
	 * @param resource
	 *            資料來源, File型別
	 * @return 檔案List, List型別
	 */
	public static List<File> traversalAllFiles(File resource, String... formats) {
		if (!resource.exists() || !resource.isDirectory()) return new ArrayList<>();

		boolean formatCheckDisabled = formats.length == 0;
		try (Stream<Path> paths = Files.walk(Paths.get(resource.getCanonicalPath())).parallel()) {
			return paths.parallel()
				.map(Path::toFile)
				.filter(f -> f.isFile() && (formatCheckDisabled || formatVerify(f.getName(), formats)))
				.collect(Collectors.toList());
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return new ArrayList<>();
	}

	/**
	 * [方法多載]<br>
	 * 遍歷指定資料夾底下所有的檔案, 並回傳存放於List後回傳
	 *
	 * @see List
	 * @see File
	 * @param resource
	 *            資料來源完整路徑, String型別
	 * @return 檔案List, List型別
	 */
	public static List<File> traversalAllFiles(String resource, String... formats) {
		return traversalAllFiles(new File(resource), formats);
	}

	/**
	 * Convert byte array into hex. @param hash @return
	 */
	public static String byteArray2Hex(byte[] hash) {
		try (Formatter formatter = new Formatter()) {
			for (byte b : hash) {
				formatter.format("%02x", b);
			}

			return formatter.toString();
		}
	}

	/**
	 * 取得檔案唯一識別值(MD5)
	 * 
	 * @see FileInputStream
	 * @see MessageDigest
	 * @see Hex#encodeHex(byte[])
	 * 
	 * @param file
	 *            欲取得MD5的檔案, File型別
	 * @return 回傳MD5值, String型別
	 */
	public static String getMD5(File file) {
		if (file.exists()) {
			MessageDigest messageDigest = null;
			byte[] buffer = new byte[2048];
			try (FileInputStream in = new FileInputStream(file);
					DigestInputStream dis = new DigestInputStream(in, messageDigest = MessageDigest.getInstance("MD5"))) {
				// Read bytes from the file.
				int bytes;
				while ((bytes = dis.read(buffer)) != -1) {
					messageDigest.update(buffer, 0, bytes);
				}

				return new String(Hex.encodeHex(messageDigest.digest()));
			} catch (IOException | NoSuchAlgorithmException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}

		return null;
	}

	/**
	 * [方法多載]<br>
	 * 支援檔案路徑參數; 取得檔案唯一識別值(MD5)
	 * 
	 * @see FileInputStream
	 * @see MessageDigest
	 * @see Hex#encodeHex(byte[])
	 * 
	 * @param file
	 *            欲取得MD5的檔案, String型別
	 * @return 回傳MD5值, String型別
	 */
	public static String getMD5(String file) {
		return getMD5(new File(file));
	}

	/**
	 * 比較兩個文檔的內容是否一致
	 *
	 * @param file
	 *            比較檔案一, File型別
	 * @param otherFile
	 *            比較檔案二, File型別
	 * @return 檔案比較結果, boolean型別
	 */
	public static boolean compareFile(File file, File otherFile) {
		String srcMD5 = getMD5(file);
		String otherMD5 = getMD5(otherFile);
		return Objects.nonNull(srcMD5) && Objects.nonNull(otherMD5) && srcMD5.equals(otherMD5);
	}

	/**
	 * [方法多載]<br>
	 * 比較兩個文檔的內容是否一致
	 *
	 * @param file
	 *            比較檔案一完整路徑, String型別
	 * @param otherFile
	 *            比較檔案二完整路徑, String型別
	 * @return 檔案比較結果, boolean型別
	 */
	public static boolean compareFile(String file, String otherFile) {
		return compareFile(new File(file), new File(otherFile));
	}

	/**
	 * 將InputStream輸入流轉換成OutputStream輸出流
	 *
	 * @see InputStream
	 * @see OutputStream
	 * @param in
	 *            檔案串流(輸入), InputStream型別
	 * @param out
	 *            檔案串流(輸出), OutputStream型別
	 * @param bufferSize
	 *            串流緩衝區的大小, int型別
	 *            
	 * @return 是否轉換成功, boolean型別
	 */
	public static boolean transfer(InputStream in, OutputStream out, int bufferSize) {
		boolean isSuccess = false;
		int maxBufferSize = 12288;
		try {
			bufferSize = Math.min(bufferSize, maxBufferSize);
			byte[] bytes = new byte[bufferSize]; // 8K=8192 12K=12288
			int size;
			while ((size = in.read(bytes)) != -1) {
				out.write(bytes, 0, size);
			}
			out.flush();
			isSuccess = true;
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return isSuccess;
	}
	
	/**
	 * [方法多載]<br />
	 * 將InputStream輸入流轉換成至目標檔案內
	 *
	 * @see File
	 * @see InputStream
	 * @param in
	 *            檔案串流(輸入), InputStream型別
	 * @param file
	 *            目標檔案, File型別
	 * @param bufferSize
	 *            串流緩衝區的大小, int型別
	 *            
	 * @return 是否轉換成功, boolean型別
	 */
	public static boolean transfer(InputStream in, File file, int bufferSize) {
		try (OutputStream out = new FileOutputStream(file)) {
			return transfer(in, out, bufferSize);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return false;
	}
	
	/**
	 * 將InputStream輸入流轉換成byte
	 *
	 * @see InputStream
	 * @see OutputStream
	 * @param in
	 *            檔案串流(輸入), InputStream型別
	 *            串流緩衝區的大小, int型別
	 * @return 是否轉換成功, boolean型別
	 */
	public static byte[] toByteArray(InputStream in) {
		int bufferSize = 12288;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			byte[] bytes = new byte[bufferSize]; // 8K=8192 12K=12288
			int size;
			while ((size = in.read(bytes)) != -1) {
				out.write(bytes, 0, size);
			}
			out.flush();
			
			return out.toByteArray();
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return new byte[0];
	}
	
	/** 
	 * 將byte陣列資料寫入檔案
	 * 
	 * @see FileOutputStream
	 * @see BufferedOutputStream
	 * 
	 * @param file 來源檔案, File型別
	 * @param bytes 欲輸出串流, byte[]型別
	 * @param append 是否添加至現有檔案, boolean型別
	 * 
	 * @return 寫入byte數, long型別
	 */
	public static long writeBytes(File file, byte[] bytes, boolean append) {
		try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file, append))) {
			int size = bytes.length;
			out.write(bytes, 0, size);
			out.flush();
			
			return size;
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return -1;
	}
	
	/**
	 * [方法多載]<br /> 
	 * 欲設在檔案存在時直接覆寫, 並將byte陣列資料寫入檔案
	 * 
	 * @see FileOutputStream
	 * @see BufferedOutputStream
	 * 
	 * @param file 來源檔案, File型別
	 * @param bytes 欲輸出串流, byte[]型別
	 * 
	 * @return 寫入byte數, long型別
	 */
	public static long writeBytes(File file, byte[] bytes) {
		return writeBytes(file, bytes, false);
	}

	/**
	 * 將檔案大小字串轉換為實際bytes數大小<br>
	 * Ex:<br>
	 * 2G ==> 2147483648
	 *
	 * @param blockSize
	 *            檔案大小, String型別
	 * @return 轉換後的bytes數, long型別
	 */
	public static long parseFileSize(String blockSize) {
		float resultSize = 0f;
		int size = 1;
		for (String sizeUnit : sizeUnits) {
			if (blockSize.matches("(?i)((\\d)+\\.?\\d*\\s?" + sizeUnit + "?)")) {
				resultSize = Float.parseFloat(blockSize.replaceAll("[^0-9\\.]", "")) * size;
				break;
			}
			size *= 1024;
		}

		return (long) resultSize;
	}

	/**
	 * 格式化資料大小 <br>
	 * Ex:<br>
	 * 1024 ==> 1MB<br>
	 *
	 * @see DecimalFormat
	 * @param fileSize
	 *            檔案大小, long型別
	 * @return 格式化後資料大小, String型別
	 */
	public static String formatFileSize(long fileSize) {
		DecimalFormat df = new DecimalFormat("#.00");
		String fileSizeStr = null;
		long size = 1024L;
		for (String sizeUnit : sizeUnits) {
			if (fileSize < size) {
				fileSizeStr = df.format((double) fileSize * 1024 / size) + sizeUnit;
				break;
			}
			size *= 1024;
		}
		return fileSizeStr;
	}

}
