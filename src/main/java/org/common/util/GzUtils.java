package org.common.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.Files;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Van
 * @version 1.0
 */
public class GzUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GzUtils.class);
	
	/**
	 * 預設串流下載的byte數
	 */
	private static int defaultBuffer = 8192;

	private GzUtils() {
	}

	/**
	 * 變更預設串流下載的byte數
	 *
	 * @param buffer
	 *            串流的byte數, int型別
	 */
	public static void setBuffer(int buffer) {
		defaultBuffer = buffer;
	}

	/**
	 * 將GzTar格式的壓縮檔內容假壓縮至指定目錄下
	 *
	 * @see BufferedInputStream
	 * @see FileInputStream
	 * @see BufferedOutputStream
	 * @see FileOutputStream
	 * @see GZIPInputStream
	 * @param gzFile
	 *            欲解壓縮的GzTar格式檔案完整路徑, String型別
	 * @param destination
	 *            解壓縮的輸出路徑, String型別
	 * @param deleteResource
	 *            是否於解壓縮完畢後刪除原始檔案, boolean型別
	 *            
	 * @return 輸出的檔案位置, File型別
	 */
	public static File decompressTarGz(String gzFile, String destination, boolean deleteResource) {
		File outDir = null;
//		Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");
//		archiver.extract(archive, destination);
		try (GZIPInputStream gzIn = new GZIPInputStream(new BufferedInputStream(new FileInputStream(gzFile)));
				TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn, defaultBuffer)) {
			TarArchiveEntry tatEntry;
			while ((tatEntry = tarIn.getNextTarEntry()) != null) {
				outDir = new File(destination + File.separator + tatEntry.getName());
				if (tatEntry.isDirectory()) {
					outDir.mkdirs();
				} else {
					try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outDir))) {
						byte[] bytes = new byte[defaultBuffer];
						int size;
						while ((size = tarIn.read(bytes)) != -1) {
							out.write(bytes, 0, size);
						}
						out.flush();
					} catch (IOException e) {
						LOGGER.info(e.getMessage(), e);
					}
				}
			}
		} catch (IOException e) {
			LOGGER.info(e.getMessage(), e);
		}
		if (deleteResource) FileOperationUtils.remove(gzFile);
		
		return new File(destination);
	}

	/**
	 * 將Gz格式的壓縮檔內容假壓縮至指定目錄下
	 *
	 * @see BufferedInputStream
	 * @see FileInputStream
	 * @see BufferedOutputStream
	 * @see FileOutputStream
	 * @see GZIPInputStream
	 * @param gzFile
	 *            欲解壓縮的Gz格式檔案完整路徑, File型別
	 * @param deleteResource
	 *            是否於解壓縮完畢後刪除原始檔案, boolean型別
	 * @return 輸出的檔案位置, File型別
	 */
	public static File decompressTarGz(File gzFile, boolean deleteResource) {
		try {
			if (gzFile != null && gzFile.length() > 0) return decompressTarGz(gzFile.getCanonicalPath(), gzFile.getParent(), deleteResource);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return gzFile;
	}

	/**
	 * 	將Gz格式的壓縮檔內容假壓縮至指定目錄下
	 *
	 * @see BufferedInputStream
	 * @see FileInputStream
	 * @see BufferedOutputStream
	 * @see FileOutputStream
	 * @see GZIPInputStream
	 * @param gzFile
	 *            欲解壓縮的Gz格式檔案完整路徑, File型別
	 * @param destination
	 *            欲解壓縮至的目的地路徑, String型別
	 * @return 輸出的檔案位置, File型別
	 */
	public static File decompressGzOnly(File gzFile, String destination) {
		if (Objects.isNull(gzFile) || gzFile.length() <= 0) return null;
		String originFileName = gzFile.getName();
		String outputFileName = originFileName.substring(0, originFileName.lastIndexOf('.'));
		File destPath = new File(destination);
		if (!destPath.exists()) destPath.mkdirs();
		
		File outputFile = new File(destPath, outputFileName);
		try (FileInputStream in = new FileInputStream(gzFile);
				GZIPInputStream gzIn = new GZIPInputStream(in);
				FileOutputStream fout = new FileOutputStream(outputFile)) {

			int size;
			byte[] bytes = new byte[defaultBuffer];
			while ((size = gzIn.read(bytes, 0, bytes.length)) != -1) {
				fout.write(bytes, 0, size);
			}
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return outputFile.exists() ? outputFile : null;
	}
	
	/**
	 *  將Gz格式的壓縮檔內容假壓縮至當前目錄下
	 *
	 * @see BufferedInputStream
	 * @see FileInputStream
	 * @see BufferedOutputStream
	 * @see FileOutputStream
	 * @see GZIPInputStream
	 * @param gzFile
	 *            欲解壓縮的Gz格式檔案完整路徑, File型別
	 * @return 輸出的檔案位置, File型別
	 */
	public static File decompressGzOnly(File gzFile) {
		return decompressGzOnly(gzFile, gzFile.getParent());
	}
	
	/**
	 * [方法多載] 支援字串路徑; 將Gz格式的壓縮檔內容假壓縮至當前目錄下
	 *
	 * @see BufferedInputStream
	 * @see FileInputStream
	 * @see BufferedOutputStream
	 * @see FileOutputStream
	 * @see GZIPInputStream
	 *            欲解壓縮的Gz格式檔案完整路徑, File型別
	 * @return 輸出的檔案位置, File型別
	 */
	public static File decompressGzOnly(String file) {
		return decompressGzOnly(new File(file));
	}

	/**
	 * 將檔案壓縮成Gz格式的壓縮檔
	 *
	 * @see BufferedInputStream
	 * @see FileInputStream
	 * @see BufferedOutputStream
	 * @see FileOutputStream
	 * @see GZIPInputStream
	 * @param file
	 *            欲壓縮的來源檔, File型別
	 *            輸出的Gz格式壓縮檔名稱, String型別
	 * @return 壓縮完成的Gz壓縮檔, File型別
	 */
	public static File compressToGzFile(File file, boolean removeOrigin) {
		File gZipFile = new File(file.getAbsolutePath() + ".gz");
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
				GZIPOutputStream gzOut = new GZIPOutputStream(new FileOutputStream(gZipFile)) {
					{
						def.setLevel(Deflater.BEST_COMPRESSION);
					}
				}) {
			byte[] buffer = new byte[defaultBuffer];
			int size;
			while ((size = in.read(buffer)) != -1) {
				gzOut.write(buffer, 0, size);
			}
			gzOut.finish();
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
		boolean validGzFile = gZipFile.exists() && gZipFile.length() > 0;
		if (validGzFile && removeOrigin) FileOperationUtils.remove(file);
		
		return validGzFile ? gZipFile : null;
	}

	/**
	 * [方法多載]<br>
	 * 採用原始檔案名稱作為新檔案名稱, 將檔案壓縮成Gz格式的壓縮檔
	 *
	 * @see BufferedInputStream
	 * @see FileInputStream
	 * @see BufferedOutputStream
	 * @see FileOutputStream
	 * @see GZIPInputStream
	 * @param file
	 *            欲壓縮的來源檔, File型別
	 *            輸出的Gz格式壓縮檔名稱, String型別
	 * @return 壓縮完成的Gz壓縮檔, File型別
	 */
	public static File compressToGzFile(File file) {
		return compressToGzFile(file, false);
	}

	/**
	 * [方法多載]<br>
	 * 採用原始檔案名稱作為新檔案名稱, 將檔案壓縮成Gz格式的壓縮檔
	 *
	 * @see BufferedInputStream
	 * @see FileInputStream
	 * @see BufferedOutputStream
	 * @see FileOutputStream
	 * @see GZIPInputStream
	 * @param file
	 *            欲壓縮的來源檔, String型別
	 *            輸出的Gz格式壓縮檔名稱, String型別
	 * @return 壓縮完成的Gz壓縮檔, File型別
	 */
	public static File compressToGzFile(String file) {
		return compressToGzFile(file, false);
	}

	/**
	 * [方法多載]<br>
	 * 支援傳入檔案路徑字串; 將檔案壓縮成Gz格式的壓縮檔
	 *
	 * @see BufferedInputStream
	 * @see FileInputStream
	 * @see BufferedOutputStream
	 * @see FileOutputStream
	 * @see GZIPInputStream
	 * @param file
	 *            欲壓縮的來源檔, String型別
	 *            輸出的Gz格式壓縮檔名稱, String型別
	 * @return 壓縮完成的Gz壓縮檔, File型別
	 */
	public static File compressToGzFile(String file, boolean removeOrigin) {
		return compressToGzFile(new File(file), removeOrigin);
	}
	
	/**
	 * 將檔案壓縮成Tar.Gz格式的壓縮檔
	 *
	 * @see BufferedInputStream
	 * @see FileInputStream
	 * @see BufferedOutputStream
	 * @see FileOutputStream
	 * @see org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
	 * @see org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
	 * @param file
	 *            欲壓縮的來源檔, String型別
	 * @return 壓縮完成的Gz壓縮檔, File型別
	 */
	public static File compressToTarGzFile(File file) {
		File outputFile = new File(file.getAbsolutePath() + ".tar.gz");
		try (BufferedOutputStream buffOut = new BufferedOutputStream(new FileOutputStream(outputFile));
			 GzipCompressorOutputStream gzipOut = new GzipCompressorOutputStream(buffOut);
			 TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzipOut)) {
			addFileToTarGz(tarOut, file, "");
			tarOut.finish();
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return outputFile;
	}
	
	/**
	 * [方法多載]<br>
	 * 將檔案壓縮成Tar.Gz格式的壓縮檔 (另可額外決定是否保留壓縮前對象)
	 *
	 * @see BufferedInputStream
	 * @see FileInputStream
	 * @see BufferedOutputStream
	 * @see FileOutputStream
	 * @see GZIPOutputStream
	 * @see org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
	 * @param file
	 *            欲壓縮的來源檔, String型別
	 * @return 壓縮完成的Gz壓縮檔, File型別
	 */
	public static File compressToTarGzFile(File file, boolean removeOrigin) {
		File tarGzFile = compressToTarGzFile(file);
		if (tarGzFile.exists() && tarGzFile.length() > 0 && removeOrigin) {
			LOGGER.warn("`{}` remove result: {}", file, FileOperationUtils.remove(file));
		}
		
		return tarGzFile;
	}
	
	private static void addFileToTarGz(TarArchiveOutputStream tarOut, File file, String entryBase) {
		try {
			String entryName = entryBase + file.getName();
			TarArchiveEntry tarEntry = new TarArchiveEntry(file, entryName);
			tarOut.putArchiveEntry(tarEntry);
			if (file.isFile()) {
				Files.copy(file.toPath(), tarOut);
				tarOut.closeArchiveEntry();
			} else if (file.isDirectory()) {
				String nextEntryBase = entryName + "/";
				tarOut.closeArchiveEntry();
				Stream.of(file.listFiles())
					.forEach(subFile -> addFileToTarGz(tarOut, subFile, nextEntryBase));
			}
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
	}
}
