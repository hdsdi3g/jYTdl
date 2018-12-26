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
package tv.hd3g.jytdl;

/**
 * Ignore http_headers, downloader_options (http_chunk_size), container, format_note, url
 */
public class YoutubeVideoMetadataFormat {
	
	/**
	 * Like "249 - audio only (DASH audio)", "140 - audio only (DASH audio)", "248 - 1920x1080 (1080p)"
	 */
	String format;
	
	/**
	 * Like "vp9", "avc1.640028", "none"
	 */
	String vcodec;
	
	/**
	 * Like "opus", "mp4a.40.2", "none"
	 */
	String acodec;
	
	/**
	 * In kbps
	 * like 56.559, 127.907, 1729.513
	 */
	float tbr;
	
	/**
	 * Like "webm", "m4a"
	 */
	String ext;
	
	/**
	 * 171, 140, 248
	 */
	String format_id;
	
	/**
	 * In bytes
	 */
	long filesize;
	
	/**
	 * Like 1080
	 */
	int height;
	
	/**
	 * Like 1920
	 */
	int width;
	
	/**
	 * Like 24
	 */
	int fps;
	
	public String toString() {
		return format + ", " + tbr + " kbps (" + filesize + " bytes)";
	}
	
}
