package com.hyp.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.hyp.constants.Constants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUtils {

	public static byte[] downloadFileFromUrl(String fileUrl) throws Exception {
		try {
			URL url = new URL(fileUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");

			try (InputStream inputStream = connection.getInputStream();
					ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

				byte[] buffer = new byte[4096];
				int bytesRead;

				while ((bytesRead = inputStream.read(buffer)) != -1) {
					byteArrayOutputStream.write(buffer, 0, bytesRead);
				}

				return byteArrayOutputStream.toByteArray();
			}
		} catch (Exception e) {
			log.error("Failed to download file from URL {} {} ", fileUrl, e);
		}
		return null;
	}

	public static String trimFileUrl(String fileUrl) {
		if (fileUrl == null || fileUrl.isEmpty()) {
			return fileUrl;
		}
		try {

			for (String extension : Constants.FILE_EXTENSIONS) {
				int index = fileUrl.indexOf(extension);
				if (index != -1) {
					return fileUrl.substring(0, index + extension.length());
				}
			}
		} catch (Exception e) {
			log.error("Error occurred while trimming file URL: " + e.getMessage());
			return fileUrl;
		}
		return fileUrl;
	}

	public static String getContentType(String fileUrl) {
		if (fileUrl == null || fileUrl.isEmpty()) {
			return null;
		}
		for (Map.Entry<String, String> entry : Constants.getContentTypes().entrySet()) {
			if (fileUrl.toLowerCase().endsWith(entry.getKey())) {
				return entry.getValue();
			}
		}
		return null;
	}

	public static String getFileExtension(String contentType) {

		for (Map.Entry<String, String> entry : Constants.getContentTypes().entrySet()) {
			if (contentType.toLowerCase().endsWith(entry.getValue())) {
				return entry.getKey();
			}
		}
		return null;
	}

	public static String formatFileName(String fileName) {
		String uploadDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
		String normalizedFileName = Normalizer.normalize(fileName, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
		String formatedFileName = normalizedFileName.replaceAll("[^a-zA-Z0-9\\s-]", "").replaceAll("[_\\s]+", "-")
				.toLowerCase();
		return String.join("-", formatedFileName, uploadDate);
	}

}
