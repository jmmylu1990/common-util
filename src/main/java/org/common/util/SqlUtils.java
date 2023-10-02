package org.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SqlUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SqlUtils.class);
	private static Map<String, String> sqlCacheMap = new HashMap<>();
	private static boolean cachePrefer = true;
	
	private SqlUtils() {
	}
	
	public static void setCachePrefer(boolean cachePrefer) {
		SqlUtils.cachePrefer = cachePrefer;
	}

	private static String pathNormailize(String path) {
		boolean hasExt = path.matches("^.+?(\\.\\w+)$"); 
		path = hasExt ? path : path + ".sql";
		return path.replaceAll("^/", "");
	}
	
	public static String clean(String str) {
		return Objects.isNull(str) ? null : str.replace("'", "'").replace("\"", "\"\"");
	}
	
	public static String fromFile(String path, boolean cachePrefer) {
		path = pathNormailize(path);
		if (cachePrefer && sqlCacheMap.containsKey(path)) return sqlCacheMap.get(path);
	
		String sql = FileOperationUtils.extractContent(Thread.currentThread().getContextClassLoader().getResourceAsStream(path))
				.replaceAll("\\s*\\-{2,}.+\r?\n", " ") // Remove the comment
				.replaceAll("\r?\n", " ") // Keep the sql in single line
				.replaceAll("\\s{2,}", " "); // Truncate the space that is unnecessary
		sqlCacheMap.putIfAbsent(path, sql);
		LOGGER.debug("Sql path: {}", path);
		
		return sql;
	}
	
	public static String fromFile(String path) {
		return fromFile(path, cachePrefer); 
	}
	
	public static boolean refreshCache() {
		sqlCacheMap = sqlCacheMap.keySet().stream()
				.collect(Collectors.toMap(Function.identity(), p -> fromFile(p, false)));
		return !sqlCacheMap.isEmpty();
	}
	
	public static String refreshCache(String path) {
		path = pathNormailize(path);
		String sql = fromFile(path, false);
		sqlCacheMap.put(path, sql);
		return sql;
	}
	
	public static boolean releaseCache(String path) {
		return Objects.nonNull(sqlCacheMap.remove(pathNormailize(path)));
	}
	
	public static boolean releaseCache() {
		sqlCacheMap.clear();
		return sqlCacheMap.isEmpty();
	}

}
