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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2018
 * 
*/
package tv.hd3g.jytdl.tools;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.jytdl.MediaAsset;

public class ImageDownload {
	private static Logger log = LogManager.getLogger();
	
	public File download(MediaAsset media, File temp_dir) throws IOException {
		File thumbnail_image = null;
		
		if (media.getMtd().thumbnail != null) {
			try {
				URL thumbnail = new URL(media.getMtd().thumbnail);
				thumbnail_image = new File(temp_dir.getAbsolutePath() + File.separator + "thumbnail." + FilenameUtils.getExtension(media.getMtd().thumbnail));
				
				log.trace("Wait 0.5 sec before try to download");
				Thread.sleep(500);
				
				log.info("Download thumbnail image " + thumbnail + " to " + thumbnail_image.getName());
				int max_try = 5;
				for (int pos = 0; pos < max_try; pos++) {
					try {
						URLConnection connection = thumbnail.openConnection();
						connection.addRequestProperty("Referer", media.getMtd().webpage_url.toString());
						connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_10) AppleWebKit/543.21 (KHTML, like Gecko) Chrome/65.4.3210.987 Safari/543.21");
						connection.addRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7,de;q=0.6");
						connection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
						connection.addRequestProperty("Cache-Control", "max-age=0");
						connection.setConnectTimeout(30000);
						connection.setReadTimeout(30000);
						
						FileUtils.copyInputStreamToFile(connection.getInputStream(), thumbnail_image);
						break;
					} catch (SocketTimeoutException ste) {
						log.warn("Can't thumbnail download image (try " + (pos + 1) + "/" + max_try + ") from URL: " + media.getMtd().thumbnail + "; " + ste.getMessage());
						if (pos + 1 != max_try) {
							log.debug("Wait " + pos + " sec for the next try...");
							Thread.sleep(1000 + pos * 1000);
						}
					}
				}
				
				if (thumbnail_image.exists() == false) {
					thumbnail_image = null;
				} else if (thumbnail_image.length() == 0) {
					thumbnail_image = null;
				}
			} catch (MalformedURLException mue) {
				log.warn("Can't thumbnail download image, invalid URL: " + media.getMtd().thumbnail, mue);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		return thumbnail_image;
	}
	
}
