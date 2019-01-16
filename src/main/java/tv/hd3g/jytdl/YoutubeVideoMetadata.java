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
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Ignore here like_count, view_count, dislike_count, average_rating, subtitles, age_limit, automatic_captions, thumbnails.
 */
public class YoutubeVideoMetadata {
	
	public String toString() {
		return "\"" + fulltitle.trim() + "\" by " + uploader + " [" + id + "]";
	}
	
	public String id;
	
	public int age_limit;
	
	/**
	 * Like 20180330, YYYYMMDD
	 */
	public String upload_date;
	
	/**
	 * Like https://www.youtube.com/watch?v=deVeHV3cnnbI
	 */
	public String webpage_url;
	
	/**
	 * In seconds
	 */
	public int duration;
	
	public String fulltitle;
	
	/**
	 * Like http://www.youtube.com/user/BaptetGael
	 */
	public String uploader_url;
	
	public String description;
	
	public ArrayList<String> tags;
	
	/**
	 * Like "Bapt&Gael"
	 */
	public String uploader;
	
	/**
	 * Like "BaptetGael" or "UCb1UI8X57325r8zS1fI7uUw"
	 */
	public String uploader_id;
	
	public ArrayList<String> categories;
	
	/**
	 * Like "Standard YouTube License"
	 */
	public String license;
	
	/**
	 * Like "Youtube"
	 */
	public String extractor_key;
	
	/**
	 * URL like https://i.ytimg.com/vi/eVeHV3cnnbI/maxresdefault.jpg
	 */
	public String thumbnail;
	
	/**
	 * Like "youtube"
	 */
	public String extractor;
	
	public ArrayList<YoutubeVideoMetadataFormat> formats;
	
	static final Predicate<YoutubeVideoMetadataFormat> audioOnlyFilter = f -> {
		if (f.acodec == null) {
			return false;
		} else if (f.vcodec != null) {
			if (f.vcodec.equalsIgnoreCase("none") == false) {
				return false;
			}
		}
		return f.acodec.equalsIgnoreCase("none") == false;
	};
	
	static final Predicate<YoutubeVideoMetadataFormat> videoOnlyFilter = f -> {
		if (f.vcodec == null) {
			return false;
		} else if (f.acodec != null) {
			if (f.acodec.equalsIgnoreCase("none") == false) {
				return false;
			}
		}
		return f.vcodec.equalsIgnoreCase("none") == false;
	};
	
	Stream<YoutubeVideoMetadataFormat> getAllAudioOnlyStreams() {
		return formats.stream().filter(audioOnlyFilter);
	}
	
	Stream<YoutubeVideoMetadataFormat> getAllVideoOnlyStreams() {
		return formats.stream().filter(videoOnlyFilter);
	}
	
	static Stream<YoutubeVideoMetadataFormat> selectBestCodec(Stream<YoutubeVideoMetadataFormat> a_v_formats, String base_codec_name) {
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
	static Stream<YoutubeVideoMetadataFormat> orderByVideoResolution(Stream<YoutubeVideoMetadataFormat> video_formats) {
		return video_formats.filter(f -> {
			if (f.vcodec == null) {
				return false;
			}
			return f.vcodec.equalsIgnoreCase("none") == false;
		}).sorted((l, r) -> {
			return r.height * r.width - l.height * l.width;
		});
	}
	
	/**
	 * @return bigger to smaller
	 */
	static Stream<YoutubeVideoMetadataFormat> orderByBitrate(Stream<YoutubeVideoMetadataFormat> formats) {
		return formats.sorted((l, r) -> {
			return Math.round(r.tbr - l.tbr);
		});
	}
	
	public static String readableFileSize(long size) {
		if (size <= 0) {
			return "0";
		}
		final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
	
	static String computeTotalSizeToDownload(YoutubeVideoMetadataFormat f1, YoutubeVideoMetadataFormat f2) {
		return readableFileSize(f1.filesize + f2.filesize) + "ytes";
	}
	
	static Stream<YoutubeVideoMetadataFormat> allAudioVideoMux(Stream<YoutubeVideoMetadataFormat> formats) {
		return formats.filter(f -> {
			if (f.acodec == null | f.vcodec == null) {
				return false;
			}
			return f.acodec.equalsIgnoreCase("none") == false & f.vcodec.equalsIgnoreCase("none") == false;
		});
	}
	
}
