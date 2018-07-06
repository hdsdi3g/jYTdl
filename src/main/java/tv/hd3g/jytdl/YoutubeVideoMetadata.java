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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Ignore here like_count, view_count, dislike_count, average_rating, subtitles, age_limit, automatic_captions, thumbnails.
 */
class YoutubeVideoMetadata {
	
	public String toString() {
		return "\"" + fulltitle + "\" by " + uploader + " [" + id + "]";
	}
	
	String id;
	
	int age_limit;
	
	/**
	 * Like 20180330, YYYYMMDD
	 */
	String upload_date;
	
	/**
	 * Like https://www.youtube.com/watch?v=deVeHV3cnnbI
	 */
	String webpage_url;
	
	/**
	 * In seconds
	 */
	int duration;
	
	String fulltitle;
	
	/**
	 * Like http://www.youtube.com/user/BaptetGael
	 */
	String uploader_url;
	
	String description;
	
	ArrayList<String> tags;
	
	/**
	 * Like "Bapt&Gael"
	 */
	String uploader;
	
	/**
	 * Like "BaptetGael" or "UCb1UI8X57325r8zS1fI7uUw"
	 */
	String uploader_id;
	
	ArrayList<String> categories;
	
	/**
	 * Like "Standard YouTube License"
	 */
	String license;
	
	/**
	 * Like "Youtube"
	 */
	String extractor_key;
	
	/**
	 * URL like https://i.ytimg.com/vi/eVeHV3cnnbI/maxresdefault.jpg
	 */
	String thumbnail;
	
	/**
	 * Like "youtube"
	 */
	String extractor;
	
	ArrayList<Format> formats;
	
	/**
	 * Ignore http_headers, downloader_options (http_chunk_size), container, format_note, url
	 */
	static class Format {
		
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
	
	Stream<Format> getAllAudioOnlyStreams() {
		return formats.stream().filter(f -> {
			if (f.acodec == null) {
				return false;
			} else if (f.vcodec != null) {
				if (f.vcodec.equalsIgnoreCase("none") == false) {
					return false;
				}
			}
			return f.acodec.equalsIgnoreCase("none") == false;
		});
	}
	
	Stream<Format> getAllVideoOnlyStreams() {
		return formats.stream().filter(f -> {
			if (f.vcodec == null) {
				return false;
			} else if (f.acodec != null) {
				if (f.acodec.equalsIgnoreCase("none") == false) {
					return false;
				}
			}
			return f.vcodec.equalsIgnoreCase("none") == false;
		});
	}
	
	static Stream<Format> keepOnlyThisCodec(Stream<Format> a_v_formats, String base_codec_name) {
		return a_v_formats.filter(f -> {
			if (f.acodec != null) {
				if (f.acodec.equalsIgnoreCase("none") == false) {
					return f.acodec.toLowerCase().startsWith(base_codec_name.toLowerCase());
				}
			}
			if (f.vcodec != null) {
				if (f.vcodec.equalsIgnoreCase("none") == false) {
					return f.vcodec.toLowerCase().startsWith(base_codec_name.toLowerCase());
				}
			}
			return false;
		});
	}
	
	/**
	 * @return bigger to smaller
	 */
	static Stream<Format> orderByVideoResolution(Stream<Format> video_formats) {
		return video_formats.filter(f -> {
			if (f.vcodec == null) {
				return false;
			}
			return f.vcodec.equalsIgnoreCase("none") == false;
		}).sorted((l, r) -> {
			return (r.height * r.width) - (l.height * l.width);
		});
	}
	
	/**
	 * @return bigger to smaller
	 */
	static Stream<Format> orderByBitrate(Stream<Format> formats) {
		return formats.sorted((l, r) -> {
			return Math.round(r.tbr - l.tbr);
		});
	}
	
	public static String readableFileSize(long size) {
		if (size <= 0) return "0";
		final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
	
	static String computeTotalSizeToDownload(Format f1, Format f2) {
		return readableFileSize(f1.filesize + f2.filesize) + "ytes";
	}
	
	static Stream<Format> allAudioVideoMux(Stream<Format> formats) {
		return formats.filter(f -> {
			if (f.acodec == null | f.vcodec == null) {
				return false;
			}
			return f.acodec.equalsIgnoreCase("none") == false & f.vcodec.equalsIgnoreCase("none") == false;
		});
	}
	
}
