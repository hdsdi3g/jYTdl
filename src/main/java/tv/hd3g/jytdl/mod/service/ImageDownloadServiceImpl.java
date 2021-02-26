/*
 * This file is part of jYTdl.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2021
 *
 */
package tv.hd3g.jytdl.mod.service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import tv.hd3g.commons.IORuntimeException;

@Service
public class ImageDownloadServiceImpl implements ImageDownloadService {
	private static final Logger log = LogManager.getLogger();

	@Override
	public File getImageFromMedia(final String thumbnailURL, final String referer, final File tempDir) {
		if (thumbnailURL == null) {
			return null;
		}

		try {
			final var url = new URL(thumbnailURL);
			final var thumbnailFile = new File(tempDir.getAbsolutePath(), "thumbnail");

			log.trace("Wait 0.5 sec before try to download");
			Thread.sleep(500);
			log.info("Download thumbnail image {} to {}", url, thumbnailFile.getName());
			final var maxTry = 5;
			for (var tryPos = 0; tryPos < maxTry; tryPos++) {
				if (downloadTry(referer, url, thumbnailFile, maxTry, tryPos)) {
					break;
				}
			}
			if (thumbnailFile.exists() && thumbnailFile.length() > 0) {
				return thumbnailFile;
			} else {
				FileUtils.deleteQuietly(thumbnailFile);
			}
		} catch (final MalformedURLException mue) {
			log.warn("Can't download thumbnail image, invalid URL: {}", thumbnailURL, mue);
		} catch (final IOException ioe) {
			throw new IORuntimeException(ioe);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
		return null;
	}

	private boolean downloadTry(final String referer,
	                            final URL thumbnailURL,
	                            final File thumbnailFile,
	                            final int maxTry,
	                            final int tryPos) throws IOException, InterruptedException {
		try {
			final var connection = thumbnailURL.openConnection();
			connection.addRequestProperty("Referer", referer);
			connection.addRequestProperty("User-Agent",
			        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_10) AppleWebKit/543.21 (KHTML, like Gecko) Chrome/65.4.3210.987 Safari/543.21");
			connection.addRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7,de;q=0.6");
			connection.addRequestProperty("Accept",
			        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
			connection.addRequestProperty("Cache-Control", "max-age=0");
			connection.setConnectTimeout(30000);
			connection.setReadTimeout(30000);

			FileUtils.copyInputStreamToFile(connection.getInputStream(), thumbnailFile);
			return true;
		} catch (final SocketTimeoutException ste) {
			log.warn("Can't thumbnail download image (try {}/{}) from URL: {}; {}",
			        tryPos + 1, maxTry, thumbnailURL, ste.getMessage());
			if (tryPos + 1 != maxTry) {
				log.debug("Wait {} sec for the next try...", tryPos);
				Thread.sleep(1000L + tryPos * 1000L);
			}
		}
		return false;
	}

}
