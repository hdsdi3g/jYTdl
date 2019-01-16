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
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import tv.hd3g.jytdl.Config.DownloadPolicyBestFormat;
import tv.hd3g.jytdl.tools.ImageDownload;

public class MediaAsset {
	
	private static final Logger log = LogManager.getLogger();
	
	/**
	 * Transform accents to non accented (ascii) version.
	 */
	public static final Pattern PATTERN_Combining_Diacritical_Marks = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	
	private final URL source_url;
	private final Config config;
	
	private YoutubeVideoMetadata mtd;
	private String normalized_title;
	private String normalized_uploader;
	private YoutubeVideoMetadataFormat best_vstream;
	private YoutubeVideoMetadataFormat best_astream;
	private DownloadPolicyBestFormat best_format;
	
	MediaAsset(URL source_url, Config config) {
		this.source_url = source_url;
		this.config = config;
	}
	
	public URL getSourceURL() {
		return source_url;
	}
	
	public static String removeFilenameForbiddenChars(String txt) {
		String l1 = txt.replaceAll("<", "[").replaceAll(">", "]").replaceAll(":", "-").replaceAll("/", "-").replaceAll("\\\\", "-").replaceAll("\\|", " ").replaceAll("\\?", "").replaceAll("\\*", " ").replaceAll("\\\"", "”");
		l1 = l1.replaceAll("&", "-").replaceAll("%", "");
		l1 = l1.replaceAll("        ", " ").replaceAll("       ", " ").replaceAll("      ", " ").replaceAll("     ", " ").replaceAll("    ", " ").replaceAll("   ", " ").replaceAll("  ", " ").replaceAll("---", "-").replaceAll("--", "-");
		return l1;
	}
	
	public void setJsonMetadata(String asset_metadatas_json) {
		Gson g = new GsonBuilder().setPrettyPrinting().create();
		mtd = g.fromJson(asset_metadatas_json, YoutubeVideoMetadata.class);
		
		normalized_title = removeFilenameForbiddenChars(PATTERN_Combining_Diacritical_Marks.matcher(Normalizer.normalize(mtd.fulltitle.trim(), Normalizer.Form.NFD)).replaceAll("").trim()).trim();
		normalized_uploader = removeFilenameForbiddenChars(PATTERN_Combining_Diacritical_Marks.matcher(Normalizer.normalize(mtd.uploader, Normalizer.Form.NFD)).replaceAll("").trim()).trim();
		
		if (config.only_audio) {
			best_format = config.audio_only_format;
			best_astream = YoutubeVideoMetadata.orderByBitrate(YoutubeVideoMetadata.selectBestCodec(mtd.getAllAudioOnlyStreams(), best_format.a)).findFirst().orElse(null);
		} else {
			List<YoutubeVideoMetadataFormat> all_best_vstream = config.max_res.stream().flatMap(config_res -> {
				return config.best_format.stream().map(best_format -> best_format.v).flatMap(config_best_vformat -> {
					Stream<YoutubeVideoMetadataFormat> stream = mtd.getAllVideoOnlyStreams();
					stream = YoutubeVideoMetadata.selectBestCodec(stream, config_best_vformat);
					/*stream = YoutubeVideoMetadata.orderByVideoResolution(stream).dropWhile(f -> {
						return f.width > config_res.w | f.height > config_res.h;
					});
					stream = YoutubeVideoMetadata.orderByBitrate(stream);*/
					return stream;
				});
			}).collect(Collectors.toUnmodifiableList());
			
			all_best_vstream = YoutubeVideoMetadata.orderByBitrate(YoutubeVideoMetadata.orderByVideoResolution(all_best_vstream.stream())).collect(Collectors.toUnmodifiableList());
			
			best_vstream = all_best_vstream.stream().findFirst().orElse(null);
			
			best_astream = config.best_format.stream().map(best_format -> best_format.a).flatMap(config_best_aformat -> {
				return YoutubeVideoMetadata.orderByBitrate(YoutubeVideoMetadata.selectBestCodec(mtd.getAllAudioOnlyStreams(), config_best_aformat));
			}).findFirst().orElse(null);
			
			best_format = config.best_format.stream().filter(format -> {
				return best_vstream.vcodec.startsWith(format.v);
			}).findFirst().orElse(config.best_format.get(0));
			
			/*best_format = config.max_res.stream().flatMap(config_res -> {
				return config.best_format.stream().flatMap(config_best_format -> {
					Stream<YoutubeVideoMetadataFormat> stream = YoutubeVideoMetadata.selectBestCodec(mtd.getAllVideoOnlyStreams(), config_best_format.v);
					stream = YoutubeVideoMetadata.orderByBitrate(YoutubeVideoMetadata.orderByVideoResolution(stream));
					
					if (stream.findFirst().isPresent()) {
						return Stream.of(config_best_format);
					} else {
						return Stream.empty();
					}
				});
			}).findFirst().orElse(config.best_format.get(0));*/
			
		}
		
		log.info("Wanted format: {}, selected: {}, {}", best_format, best_vstream, best_astream);
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
	
	public String getExtension() {
		return best_format.ext;
	}
	
	public boolean isProcessMp4() {
		return best_format.process_mp4;
	}
	
	/**
	 * @return can be null
	 */
	public YoutubeVideoMetadataFormat getBestVideoStream() {
		return best_vstream;
	}
	
	/**
	 * @return can be null
	 */
	public YoutubeVideoMetadataFormat getBestAudioStream() {
		return best_astream;
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
