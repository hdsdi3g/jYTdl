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
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 * 
*/
package tv.hd3g.jytdl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.ExecutableFinder;
import tv.hd3g.jytdl.tools.FFmpegMuxer;
import tv.hd3g.jytdl.tools.ImageDownload;
import tv.hd3g.jytdl.tools.AtomicParsley;
import tv.hd3g.jytdl.tools.YoutubeDl;

public class Downloader {
	
	private static final Logger log = LogManager.getLogger();
	
	private final Config config;
	private final File out_directory;
	
	private final ImageDownload image_download;
	private final FFmpegMuxer ffmpeg_muxer;
	private final AtomicParsley mp4tagger;
	private final YoutubeDl youtubedl;
	
	public Downloader(ExecutableFinder exec_binary_path, Config config, ExecutorService message_out_executor) throws IOException {
		this.config = config;
		if (config == null) {
			throw new NullPointerException("\"config\" can't to be null");
		}
		
		image_download = new ImageDownload();
		
		ScheduledExecutorService max_exec_time_scheduler = Executors.newScheduledThreadPool(1);
		youtubedl = new YoutubeDl(exec_binary_path, config, message_out_executor);
		youtubedl.autoTestExecutable(max_exec_time_scheduler);
		
		ffmpeg_muxer = new FFmpegMuxer(exec_binary_path, config, message_out_executor);
		ffmpeg_muxer.autoTestExecutable(max_exec_time_scheduler);
		
		mp4tagger = new AtomicParsley(exec_binary_path, config, message_out_executor);
		mp4tagger.autoTestExecutable(max_exec_time_scheduler);
		
		out_directory = config.getOutDir();
		if (out_directory == null) {
			throw new NullPointerException("\"out_directory\" can't to be null");
		} else if (out_directory.exists() == false) {
			throw new RuntimeException("Invalid out_directory: " + out_directory.getPath() + ", don't exists");
		} else if (out_directory.canWrite() == false) {
			throw new RuntimeException("Invalid out_directory: " + out_directory.getPath() + ", can't write");
		} else if (out_directory.isDirectory() == false) {
			throw new RuntimeException("Invalid out_directory: " + out_directory.getPath() + ", is not au directory");
		}
	}
	
	void download(URL source_url) throws IOException, InterruptedException {
		if (source_url == null) {
			throw new NullPointerException("\"source\" can't to be null");
		}
		
		log.info("Prepare download for " + source_url);
		
		/**
		 * Test if URL is valid.
		 */
		String youtube_id = youtubedl.getId(source_url);
		log.debug("Id is " + youtube_id);
		
		MediaAsset media = new MediaAsset(source_url, config);
		media.setJsonMetadata(youtubedl.getRawJsonMetadata(media.getSourceURL()));
		
		YoutubeVideoMetadataFormat best_aformat = media.getBestAudioStream();
		YoutubeVideoMetadataFormat best_vformat = media.getBestVideoStream();
		
		if (best_aformat == null | best_vformat == null) {
			if (media.isOnlyAudio() && best_aformat == null) {
				throw new IOException("Can't found best audio codec for " + media.getMtd() + " in only-audio mode");
			}
			
			if (best_aformat != null) {
				log.info("Can't found best video codec for " + media.getMtd());
			} else if (best_vformat != null) {
				log.info("Can't found best audio codec for " + media.getMtd());
			} else {
				log.info("Can't found best audio and video codec for " + media.getMtd());
			}
			
			Optional<YoutubeVideoMetadataFormat> o_best_avformat = YoutubeVideoMetadata.orderByVideoResolution(YoutubeVideoMetadata.allAudioVideoMux(media.getMtd().formats.stream())).findFirst();
			
			YoutubeVideoMetadataFormat to_download = o_best_avformat.orElseThrow(() -> new RuntimeException("Can't found valid downloadable format for " + media.getMtd()));
			
			File output_file = new File(out_directory.getCanonicalPath() + File.separator + media.getBaseOutFileName() + "." + to_download.ext);
			
			if (output_file.exists()) {
				log.warn("Output file exists: \"" + output_file + "\", ignore download operation");
				return;
			}
			
			youtubedl.downloadSpecificFormat(output_file, to_download, media.getMtd());
			
			boolean modified = output_file.setLastModified(System.currentTimeMillis());
			if (modified == false) {
				log.warn("Can't change file date " + output_file);
			}
		} else {
			File temp_dir = new File(out_directory.getCanonicalPath() + File.separator + media.getMtd().extractor_key + " " + media.getMtd().id + " - " + media.getNormalizedTitle());
			
			log.debug("Create dest temp dir " + temp_dir);
			FileUtils.forceMkdir(temp_dir);
			FileUtils.cleanDirectory(temp_dir);
			
			log.debug("Select best audio format: " + best_aformat);
			
			File v_outfile = null;
			if (media.isOnlyAudio() == false) {
				log.debug("Select best video format: " + best_vformat);
				log.info("Download " + media.getMtd() + "; " + YoutubeVideoMetadata.computeTotalSizeToDownload(best_aformat, best_vformat));
				
				v_outfile = new File(temp_dir.getAbsolutePath() + File.separator + "v-" + best_vformat.format_id + "." + best_vformat.ext);
				youtubedl.downloadSpecificFormat(v_outfile, best_vformat, media.getMtd());
			} else {
				log.info("Download audio only " + media.getMtd() + "; " + YoutubeVideoMetadata.readableFileSize(best_aformat.filesize) + "ytes");
			}
			
			File a_outfile = new File(temp_dir.getAbsolutePath() + File.separator + "a-" + best_aformat.format_id + "." + best_aformat.ext);
			youtubedl.downloadSpecificFormat(a_outfile, best_aformat, media.getMtd());
			
			File final_output_file = new File(out_directory.getCanonicalPath() + File.separator + media.getBaseOutFileName() + "." + media.getExtension());
			
			if (media.isProcessMp4()) {
				File mux_outfile = new File(temp_dir.getAbsolutePath() + File.separator + "mux." + media.getExtension());
				ffmpeg_muxer.muxStreams(media, mux_outfile, v_outfile, a_outfile);
				media.downloadImage(image_download, temp_dir);
				mp4tagger.addTagsToFile(media, mux_outfile, final_output_file);
			} else {
				ffmpeg_muxer.muxStreams(media, final_output_file, v_outfile, a_outfile);
				log.info("Output format is not suitable for image/metadata injection, skip it.");
			}
			
			log.debug("Remove " + temp_dir);
			FileUtils.forceDelete(temp_dir);
		}
	}
	
}
