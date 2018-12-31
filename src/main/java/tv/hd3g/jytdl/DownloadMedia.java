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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.Normalizer;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DownloadMedia {
	/**
	 * Transform accents to non accented (ascii) version.
	 */
	public static final Pattern PATTERN_Combining_Diacritical_Marks = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	
	private final URL source_url;
	private final Config config;
	
	private YoutubeVideoMetadata mtd;
	private String normalized_title;
	private String normalized_uploader;
	
	DownloadMedia(URL source_url, Config config) {
		this.source_url = source_url;
		this.config = config;
	}
	
	public URL getSourceURL() {
		return source_url;
	}
	
	public static String removeFilenameForbiddenChars(String txt) {
		String l1 = txt.replaceAll("<", "[").replaceAll(">", "]").replaceAll(":", "-").replaceAll("/", "-").replaceAll("\\\\", "-").replaceAll("\\|", " ").replaceAll("\\?", "").replaceAll("\\*", " ").replaceAll("\\\"", "”");
		// Advanced Damage & Decay FX Tutorial! 100% After Effects!
		l1 = l1.replaceAll("&", "-").replaceAll("%", "");
		l1 = l1.replaceAll("        ", " ").replaceAll("       ", " ").replaceAll("      ", " ").replaceAll("     ", " ").replaceAll("    ", " ").replaceAll("   ", " ").replaceAll("  ", " ").replaceAll("---", "-").replaceAll("--", "-");
		return l1;
	}
	
	public void setJsonMetadata(String asset_metadatas_json) {
		Gson g = new GsonBuilder().setPrettyPrinting().create();
		mtd = g.fromJson(asset_metadatas_json, YoutubeVideoMetadata.class);
		
		normalized_title = removeFilenameForbiddenChars(PATTERN_Combining_Diacritical_Marks.matcher(Normalizer.normalize(mtd.fulltitle, Normalizer.Form.NFD)).replaceAll("").trim());
		normalized_uploader = removeFilenameForbiddenChars(PATTERN_Combining_Diacritical_Marks.matcher(Normalizer.normalize(mtd.uploader, Normalizer.Form.NFD)).replaceAll("").trim());
	}
	
	public YoutubeVideoMetadata getMtd() {
		return mtd;
	}
	
	public String getNormalizedTitle() {
		return normalized_title;
	}
	
	public String getNormalizedUploader() {
		return normalized_uploader;
	}
	
	public String getBaseOutFileName() {
		return normalized_title + " ➠ " + normalized_uploader + "  " + mtd.id;
	}
	
	/**
	 * @return can be null
	 */
	public YoutubeVideoMetadataFormat getBestVideoStream() {
		return YoutubeVideoMetadata.orderByBitrate(YoutubeVideoMetadata.orderByVideoResolution(YoutubeVideoMetadata.keepOnlyThisCodec(mtd.getAllVideoOnlyStreams(), config.best_vformat)).dropWhile(f -> {
			return f.width > config.max_width_res | f.height > config.max_height_res;
		})).findFirst().orElse(null);
	}
	
	/**
	 * @return can be null
	 */
	public YoutubeVideoMetadataFormat getBestAudioStream() {
		return YoutubeVideoMetadata.orderByBitrate(YoutubeVideoMetadata.keepOnlyThisCodec(mtd.getAllAudioOnlyStreams(), config.best_aformat)).findFirst().orElse(null);
	}
	
	private File thumbnail_image;
	
	public void downloadImage(ImageDownload downloader, File temp_dir) throws IOException {
		thumbnail_image = downloader.download(this, temp_dir);
	}
	
	/**
	 * @return can be null, please call downloadImage before.
	 */
	public File getThumbnailImage() {
		return thumbnail_image;
	}
	
	public boolean isOnlyAudio() {
		return config.only_audio;
	}
}
