package org.common.util;

import org.apache.commons.text.StringEscapeUtils;
import org.common.util.annotation.AssignFrom;
import org.common.util.annotation.FieldAlias;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.HtmlUtils;

import java.io.*;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Van
 * @version 1.2
 */
public class ClassUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClassUtils.class);
	private static final int RAW_DATA_BUFFER = 5000;
	private static final String FIELD_DELIMITER_PATTERN = ",|([@\t])+";
	private static final String ROW_DELIMITER_PATTERN = "(\r)?\n";
	
	public static final String SERIAL_VERSION_UID = "serialVersionUID";
	public static final String CLASS_FILE_SUFFIX = ".class";
	public static final String WRONG_PACKAGE_ERROR_MSG = "Unable to get resources from path '%s'. Are you sure the package '%s' exists?";
	
	private static final Map<Class<?>, List<Field>> FILEDS_CACHE = new HashMap<>();

	enum DatabaseTypeEnum {
		STRING, TEXT, VARCHAR, NVARCHAR, NUMBER, INT, FLOAT, DOUBLE, DATE, TIME, DATETIME
	}

	private ClassUtils() {
	}
	
	public static Class<?> getGenericType(Field field, int index) {
	    Type type = field.getGenericType();
	    if (type instanceof ParameterizedType) {
	    	Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();

	    	return (Class<?>) typeArguments[index < typeArguments.length ? index : 0];
	    }
	    
	    return field.getType();
	}
	
	public static Class<?> getGenericType(Field beanClass) {
		return getGenericType(beanClass, 0);
	}
	
	/**
	 * 列出指定package下的所有Java類
	 * 
	 * @param packageName 指定掃描的package名稱, String型別
	 * @return pakcage下所有的Class, List型別
	 */
	public static List<Class<?>> list(String packageName) {
		String scannedPath = packageName.replace(StringTools.DOT, StringTools.SLASH);
        URL scannedUrl = Thread.currentThread().getContextClassLoader().getResource(scannedPath);
        if (Objects.isNull(scannedUrl)) {
            throw new IllegalArgumentException(String.format(WRONG_PACKAGE_ERROR_MSG, scannedPath, packageName));
        }
        try (Stream<Path> pathStream = Files.walk(Paths.get(scannedUrl.toURI()))) {
        	return pathStream.filter(path -> path.toString().endsWith(CLASS_FILE_SUFFIX)).map(path -> {
				try {
					String pathName = packageName + StringTools.DOT + path.getFileName();
					return Class.forName(StringTools.slice(pathName, 0, -1 * CLASS_FILE_SUFFIX.length()));
				} catch (ClassNotFoundException e) {
					return null;
				}
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		} catch (URISyntaxException | IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

        return new ArrayList<>(0);
	}

	/**
	 * 判斷傳入物件是否為有效參數<br>
	 * 包含Array型別物件及Collection物件的判斷<br>
	 * Ex:<br>
	 * <code>isValid(null) --> false</code><br>
	 * <code>isValid("") --> false </code><br>
	 * <code>isValid("null") --> false </code><br>
	 * <code>isValid("NULL") --> false </code><br>
	 * <code>isValid(new String[0]) --> false</code><br>
	 * <code>isValid(new ArrayList<>()) --> false</code><br>
	 *
	 * @param obj
	 *            傳入物件, Object型別
	 * @return 是否為有效物件, boolean型別
	 */
	public static boolean isValid(Object obj) {
		if (obj == null) {
			return false;
		} else if (obj instanceof String) {
			String objStr = ((String) obj).trim();
			return !(objStr.isEmpty() || objStr.matches("(?i)(null)|(\\\\N)"));
		} else if (obj.getClass().isArray()) {
			return ((Object[]) obj).length > 0;
		} else if (obj instanceof Collection) {
			return !((Collection<?>) obj).isEmpty();
		} else if (obj instanceof Map) {
			return !((Map<?, ?>) obj).isEmpty();
		} else {
			return obj != null;
		}
	}
	
	/**
	 * 取得所有宣告欄位, 包含父類別宣告欄位
	 * 
	 * @param beanClass 指定類別, Class型別
	 * 
	 * @return 取得所有定義欄位, List型別
	 */
	public static List<Field> getAllFields(Class<?> beanClass) {
		Field[] superDeclaredFields = !Modifier.isAbstract(beanClass.getModifiers()) ? beanClass.getSuperclass().getDeclaredFields() : new Field[0];
		Field[] declaredFields = beanClass.getDeclaredFields();
		
		// If the cache of column list that is defined in model existed, then return directly
		if (FILEDS_CACHE.containsKey(beanClass)) return FILEDS_CACHE.get(beanClass);
		
		List<Field> fieldList = Stream.of(superDeclaredFields, declaredFields)
				.flatMap(Stream::of)
				.collect(Collectors.toList());
		FILEDS_CACHE.put(beanClass, fieldList);
		
		return fieldList;
	}
	
	public static <E> void selfAssign(E bean) {
		Class<?> beanClass = bean.getClass();
		List<Field> fieldList = getAllFields(beanClass);
		// Set all field accessible
		fieldList.stream().forEach(f -> f.setAccessible(true));
		Map<String, Field> fieldMap = fieldList.parallelStream().collect(Collectors.toMap(Field::getName, Function.identity()));
		for (Field field : fieldList) {
			try {
				if (!field.isAnnotationPresent(AssignFrom.class)) continue;
				
				String[] targetFieldNames = field.getAnnotation(AssignFrom.class).name();
				for (String targetFieldName : targetFieldNames) {
					Field targetField = fieldMap.get(targetFieldName);
					Object targetValue = targetField.get(bean);
					if (Objects.nonNull(targetValue)) {
						field.set(bean, targetValue);
						break;
					}
				}
			} catch (ReflectiveOperationException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * 將傳入類別的欄位以指定符號做間隔列出字串<br>
	 * Ex:<br>
	 * field1,field2,field2,....
	 *
	 * @param beanClass
	 *            欲列出所有欄位的類別, Class型別
	 * @param token
	 *            指定的分隔符號, String型別
	 * @return 以指定符號分隔欄位的字串列, String型別
	 */
	public static String joinFields(Class<?> beanClass, String token) {
		return getAllFields(beanClass).stream().map(Field::getName).collect(Collectors.joining(token));
	}

	/**
	 * [方法多載]<br>
	 * 將傳入類別的欄位以"逗號"做間隔符號列出字串<br>
	 * Ex:<br>
	 * field1,field2,field2,....
	 *
	 * @param beanClass
	 *            欲列出所有欄位的類別, Class型別
	 * @return 以指定符號分隔欄位的字串列, String型別
	 */
	public static String joinFields(Class<?> beanClass) {
		return joinFields(beanClass, StringTools.COMMA);
	}
	
	/**
	 * 取得指定類別所有宣告的欄位
	 * 
	 * @param beanClass 物件類別 
	 * @return 欄位List
	 */
	public static List<Field> getAllDeclaredFieldList(Class<?> beanClass) {
		return Stream.of(beanClass.getDeclaredFields())
				.filter(f -> !f.getName().equals(SERIAL_VERSION_UID))
				.collect(Collectors.toList());
	}

	/**
	 * 取得指定類別所有宣告的欄位名稱字串
	 * 
	 * @param beanClass 物件類別 
	 * @return 欄位名稱字串List
	 */
	public static List<String> getAllDeclaredFieldStrList(Class<?> beanClass) {
		return getAllDeclaredFieldList(beanClass).stream()
				.map(Field::getName)
				.collect(Collectors.toList());
	}
	
	/**
	 * 取得指定類別中所有宣告欄位的名稱對應Map
	 * @param beanClass 物件類別 
	 * @return 欄位名稱為Key, 欄位本身為Value的Map
	 */
	public static Map<String, Field> getAllDeclaredFieldMap(Class<?> beanClass) {
		return getAllDeclaredFieldList(beanClass).parallelStream().collect(Collectors.toMap(Field::getName, Function.identity()));
	} 

	/**
	 * 取得該類別中宣告的所有getter方法, 並存放於Map物件中<br>
	 * 可透過欄位名稱作為key取出對應的getter方法
	 *
	 * @see Method
	 * @param beanClass
	 *            物件類別
	 * @return 存放所有getter方法的Map物件
	 */
	public static Map<String, Method> getAllGetterMethods(Class<?> beanClass) {
		Map<String, Method> methodMap = new HashMap<>();
		Field[] fields = beanClass.getDeclaredFields();
		for (Field field : fields) {
			try {
				String fieldName = field.getName();
				if (!fieldName.equalsIgnoreCase(SERIAL_VERSION_UID)) {
					String methodName = String.format("get%s", StringTools.upperFirstCase(fieldName));
					methodMap.put(fieldName, beanClass.getMethod(methodName));
				}
			} catch (NoSuchMethodException | SecurityException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return methodMap;
	}

	/**
	 * 取得該類別中宣告的所有setter方法, 並存放於Map物件中<br>
	 * 可透過欄位名稱作為key取出對應的setter方法
	 *
	 * @see Method
	 * @param beanClass
	 *            物件類別
	 * @return 存放所有setter方法的Map物件
	 */
	public static Map<String, Method> getAllSetterMethods(Class<?> beanClass) {
		Map<String, Method> methodMap = new HashMap<>();
		Field[] fields = beanClass.getDeclaredFields();
		for (Field field : fields) {
			try {
				String fieldName = field.getName();
				if (!fieldName.equalsIgnoreCase(SERIAL_VERSION_UID)) {
					String methodName = String.format("set%s", StringTools.upperFirstCase(fieldName));
					methodMap.put(fieldName, beanClass.getMethod(methodName, field.getType()));
				}
			} catch (NoSuchMethodException | SecurityException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return methodMap;
	}
	
	
	/**
	 * 將傳入的{String, StringBuffer, StringBuilder}物件轉換成指定類別的List物件
	 *
	 * @see String
	 * @see StringBuffer
	 * @see StringBuilder
	 * @see CharSequence
	 * @param beanClass
	 *            物件類別
	 * @param data
	 *            父類別為CharSequence的物件
	 * @param fieldDelimiter
	 *            欄位間分隔字符, String型別
	 * @param rowDelimiter
	 *            列資料間換行字符, String型別
	 * @param hasHeader
	 *            資料是否包含標頭, boolean型別
	 * @return 傳入類別的List物件
	 */
	public static <E> List<E> stringToBeanList(Class<E> beanClass, CharSequence data, String fieldDelimiter, String rowDelimiter, boolean hasHeader) {
		String[] rowDatas = data.toString().split(rowDelimiter);
		int rowIndexBegin = hasHeader ? 1 : 0;
		String header = hasHeader ? rowDatas[0] : joinFields(beanClass, fieldDelimiter);
		List<E> beanList = new ArrayList<>(rowDatas.length - 1);
		List<String> columnList = Stream.of(header.replace("\"", "").split(fieldDelimiter))
				.map(String::toLowerCase)
				.collect(Collectors.toList());
		Map<Field, Integer> fieldsMapping = new LinkedHashMap<>();
		try {
			// Set the field mapping
			getAllFields(beanClass).stream().forEach(field -> {
				field.setAccessible(true);
				FieldAlias fieldAlias = field.getAnnotation(FieldAlias.class);
				int index = columnList.indexOf(field.getName().toLowerCase());
				// If the value has no field related then check FieldAlias annotation
				if (index < 0 && Objects.nonNull(fieldAlias)) {
					index = Stream.of(fieldAlias.name()).map(String::toLowerCase)
						.filter(columnList::contains)
						.findAny()
						.map(columnList::indexOf)
						.orElse(-1);
				}
				fieldsMapping.put(field, index);
			});
//			Collections.shuffle(columnList);
			Constructor<E> constructor = beanClass.getConstructor();
			Set<Entry<Field, Integer>> entrySet = fieldsMapping.entrySet();
			for (int i = rowIndexBegin; i < rowDatas.length; i++) {
				String rowData = rowDatas[i];
				// Split the data by asign field delimiter
				String[] fieldData = rowData.split(fieldDelimiter);
				E instance = constructor.newInstance();
				entrySet.parallelStream().filter(entry -> entry.getValue() >= 0).forEach(entry -> {
					try {
						Field field = entry.getKey();
						Integer valueIndex = entry.getValue();
						String fieldValue = fieldData.length == valueIndex ? "" : fieldData[valueIndex];
						// Set the field value to instance
						field.set(instance, convertValue(fieldValue, field.getType()));
					} catch (IllegalAccessException | IllegalArgumentException e) {
						LOGGER.error(e.getMessage(), e);
					}
				});
				beanList.add(instance);
			}
		} catch (IllegalAccessException | InstantiationException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return beanList;
	}
	
	/**
	 * [方法多載]<br>
	 * 支援自動偵測分隔字符; 將傳入的{String, StringBuffer, StringBuilder}物件轉換成指定類別的List物件
	 *
	 * @see String
	 * @see StringBuffer
	 * @see StringBuilder
	 * @see CharSequence
	 * @param beanClass 物件類別
	 * @param data
	 *       父類別為CharSequence的物件
	 * @param hasHeader
	 * 		 資料是否包含標頭, boolean型別
	 * @return 傳入類別的List物件
	 */
	public static <E> List<E> stringToBeanList(Class<E> beanClass, CharSequence data, boolean hasHeader) {
		String content = data.toString();
		String rowDelimiter = StringTools.findFirstMatchSequence(data, ROW_DELIMITER_PATTERN);
		String fieldDelimiter = StringTools.findFirstMatchSequence(content.trim().substring(0, content.indexOf(rowDelimiter)), FIELD_DELIMITER_PATTERN);
		String escapeRowDelimiter = StringEscapeUtils.escapeJava(rowDelimiter);
		LOGGER.info("FieldDelimiter: {} / RowDelimiter: {}", fieldDelimiter, escapeRowDelimiter);
		
		return stringToBeanList(beanClass, data, fieldDelimiter, rowDelimiter, hasHeader);
	}
	
	/**
	 * 將傳入的{String, StringBuffer, StringBuilder}物件轉換成指定類別的物件
	 *
	 * @see String
	 * @see StringBuffer
	 * @see StringBuilder
	 * @see CharSequence
	 * @param beanClass
	 *            物件類別
	 * @param data
	 *            父類別為CharSequence的物件
	 * @param header
	 *            資料標頭, String型別
	 * @param fieldDelimiter
	 *            欄位分隔字符, String型別
	 * @return 傳入類別物件
	 */
	public static <E> E stringToBean(Class<E> beanClass, String header, CharSequence data, String fieldDelimiter) {
		StringBuilder builder = new StringBuilder(header).append(StringTools.CRLF).append(data);
		List<E> dataList = stringToBeanList(beanClass, builder, fieldDelimiter, StringTools.CRLF, true);

		return dataList.isEmpty() ? null : dataList.get(0);
	}
	
	/**
	 * [方法多載]<br>
	 * 支援自動偵測分隔字符; 將傳入的{String, StringBuffer, StringBuilder}物件轉換成指定類別的物件
	 *
	 * @see String
	 * @see StringBuffer
	 * @see StringBuilder
	 * @see CharSequence
	 * @param beanClass
	 *            物件類別
	 * @param header
	 *            資料標頭, String型別
	 * @param data
	 *            父類別為CharSequence的物件
	 * @return 傳入類別的物件
	 */
	public static <E> E stringToBean(Class<E> beanClass, String header, CharSequence data) {
		List<E> dataList = stringToBeanList(beanClass, new StringBuilder(header).append(StringTools.CRLF).append(data), StringTools.findFirstMatchSequence(data, FIELD_DELIMITER_PATTERN), StringTools.CRLF, true);
		return dataList.isEmpty() ? null : dataList.get(0);
	}
	
	/**
	 * [方法多載]<br>
	 * 支援自動偵測分隔字符及物件欄位; 將傳入的{String, StringBuffer, StringBuilder}物件轉換成指定類別的物件
	 *
	 * @see String
	 * @see StringBuffer
	 * @see StringBuilder
	 * @see CharSequence
	 * @param beanClass
	 *            物件類別
	 * @param data
	 *            父類別為CharSequence的物件
	 * @return 傳入類別的物件
	 */
	public static <E> E stringToBean(Class<E> beanClass, CharSequence data) {
		String fieldDelimiter = StringTools.findFirstMatchSequence(data, FIELD_DELIMITER_PATTERN);
		return stringToBean(beanClass, joinFields(beanClass, fieldDelimiter), data, fieldDelimiter);
	}
	
	/**
	 * 支援傳入型別為File,將File物件反序列化成指定類別的List物件
	 *
	 * @see String
	 * @see StringBuffer
	 * @see StringBuilder
	 * @see CharSequence
	 * @param beanClass
	 *            物件類別
	 * @param files
	 *            存放字串資料的File參數列表
	 * @return 傳入類別的List物件
	 */
	public static <E> List<E> fileToBeanList(Class<E> beanClass, boolean hasHeader, File... files) {
		List<E> beanList = new ArrayList<>();
		Stream.of(files).forEach(file -> {
			Charset charset = FileOperationUtils.detectCharset(file);
			AtomicInteger atomicInteger = new AtomicInteger(1);
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset))) {
				String line;
				String header = hasHeader ? in.readLine().replace(StringTools.UTF8_BOM, "") : "";
				StringBuilder contentBuilder = new StringBuilder(header).append(StringTools.CRLF);
				while ((line = in.readLine()) != null) {
					contentBuilder.append(line).append(StringTools.CRLF);
					if (atomicInteger.getAndIncrement() % RAW_DATA_BUFFER == 0) {
						beanList.addAll(stringToBeanList(beanClass, contentBuilder, hasHeader));
						contentBuilder.setLength(0);
						if (hasHeader) contentBuilder.append(header).append(StringTools.CRLF);
					}
				}
				if (contentBuilder.length() > 0) beanList.addAll(stringToBeanList(beanClass, contentBuilder, hasHeader));
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		});
		
		return beanList;
	}
	
	/**
	 * 將傳入的{String, StringBuffer, StringBuilder}物件轉換成指定類別的Set物件
	 *
	 * @see String
	 * @see StringBuffer
	 * @see StringBuilder
	 * @see CharSequence
	 * @param beanClass
	 *            物件類別
	 * @param files
	 *            存放字串資料的File參數列表
	 * @return 傳入類別的Set物件
	 */
	public static <E> Set<E> fileToBeanSet(Class<E> beanClass, boolean hasHeader, File... files) {
		return new HashSet<>(fileToBeanList(beanClass, hasHeader, files));
	}

	/**
	 * 將傳入的字串值, 轉換成指定的型別值
	 *
	 * @param value
	 *            欲轉換的值, Sring型別
	 * @param fieldType
	 *            轉換後的型別
	 * @return 轉換後的值, Object型別
	 */
	public static Object convertValue(String value, Class<?> fieldType) {
		Object result = null;
		if (!isValid(value)) return result;
		
		if (Boolean.class.isAssignableFrom(fieldType) || boolean.class.isAssignableFrom(fieldType)) {
			result = Boolean.parseBoolean(value);
		} else if (Long.class.isAssignableFrom(fieldType) || long.class.isAssignableFrom(fieldType)) {
			result = Long.parseLong(value);
		} else if (Integer.class.isAssignableFrom(fieldType) || int.class.isAssignableFrom(fieldType)) {
			result = Integer.parseInt(value);
		} else if (Float.class.isAssignableFrom(fieldType) || float.class.isAssignableFrom(fieldType)) {
			result = Float.parseFloat(value);
		} else if (Double.class.isAssignableFrom(fieldType) || double.class.isAssignableFrom(fieldType)) {
			result = Double.parseDouble(value);
		} else if (Date.class.isAssignableFrom(fieldType) && value.matches(DateUtils.DATE_SIMILAR_REGEX)) {
			result = DateUtils.parseStrToDate(value);
		} else {
			result = value;
		}

		return result;
	}

	/**
	 * 將傳入的字串解析為為對應的class類別
	 *
	 * @see String
	 * @see Number
	 * @see Integer
	 * @see Float
	 * @see Double
	 * @see Date
	 * @param typeName
	 *            class名稱的字串, String型別
	 * @return 由字面轉換後的class類, Class型別
	 */
	public static Class<?> literalToType(String typeName) {
		Class<?> beanlass = null;
		DatabaseTypeEnum dbType = DatabaseTypeEnum.valueOf(typeName.toUpperCase());
		switch (dbType) {
		case STRING:
		case TEXT:
		case VARCHAR:
		case NVARCHAR:
			beanlass = String.class;
			break;
		case NUMBER:
			beanlass = Number.class;
			break;
		case INT:
			beanlass = Integer.class;
			break;
		case FLOAT:
			beanlass = Float.class;
			break;
		case DOUBLE:
			beanlass = Float.class;
			break;
		case DATE:
			beanlass = java.sql.Date.class;
			break;
		case TIME:
			beanlass = java.sql.Time.class;
			break;
		case DATETIME:
			beanlass = java.sql.Timestamp.class;
			break;
		default:
			break;
		}

		return beanlass;
	}

	/**
	 * 判斷一個物件實例是否為一個集合物件
	 *
	 * @see Collection
	 * @see List
	 * @see Set
	 * @see Map
	 * @param obj
	 *            物件
	 * @return 回傳true/false
	 */
	public static boolean isCollectionType(Object obj) {
		String typeName = obj.getClass().getTypeName();
		return (typeName.equals(Collection.class.getTypeName()) || typeName.equals(List.class.getTypeName())
				|| typeName.equals(Set.class.getTypeName()) || typeName.equals(Map.class.getTypeName()));
	}
	
	/**
	 * 取得完整例外訊息
	 * @param e 例外物件, Exception型別
	 * @return 完整例外訊息, String型別
	 * @see Exception
	 * @see Throwable
	 * @see StringWriter
	 * 
	 * @since 1.2
	 */
	public static String fetchStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		
		return sw.toString();
	}

	
	public static <E> E mapToBean(Map<String, Object> dataMap, Class<E> beanClass) {
		E instance = null;
		try {
			instance = beanClass.getConstructor().newInstance();
			Set<Entry<String, Object>> entrySet = dataMap.entrySet();
			for (Entry<String, Object> entry : entrySet) {
				Field field = beanClass.getDeclaredField(entry.getKey());
				if (field != null) {
					field.setAccessible(true);
					Object orignValue = entry.getValue();
					Object value = convertValue(orignValue != null ? orignValue.toString() : null, field.getType());
					field.set(instance, value);
				}
			}
		} catch (InstantiationException | IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return instance;
	}
	
	public static Object htmlEscape(Object obj, String... excludeFileds) {
		Class<? extends Object> beanClass = obj.getClass();
		Method[] methods = beanClass.getDeclaredMethods();
		List<String> excludeFieldsList = Arrays.asList(excludeFileds);
		try {
			for (Method method : methods) {
				String methodName = method.getName();
				Class<?> returnType = method.getReturnType();
				String fieldName = StringTools.lowerFirstCase(methodName.substring(3));
				if (methodName.matches("get(\\p{Alpha}+)") && returnType.equals(String.class)) {
					String paramValue = (String) method.invoke(obj);
					if (paramValue == null) continue;
					String setterMethodName = String.format("set%s", methodName.substring(3));
					String escapeValue = excludeFieldsList.contains(fieldName) ? Jsoup.clean(paramValue, Whitelist.relaxed()) : HtmlUtils.htmlEscape(paramValue);
					beanClass.getDeclaredMethod(setterMethodName, String.class).invoke(obj, escapeValue);
				}
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return obj;
	}
	
}
