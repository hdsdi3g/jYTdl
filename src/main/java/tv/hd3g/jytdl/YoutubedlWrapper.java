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
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.CommandLineProcessor;
import tv.hd3g.execprocess.ExecProcessText;
import tv.hd3g.execprocess.ExecutableFinder;
import tv.hd3g.execprocess.InteractiveExecProcessHandler;
import tv.hd3g.fflauncher.FFmpeg;

public class YoutubedlWrapper {
	
	private static final Logger log = LogManager.getLogger();
	
	static void doChecks(ExecutableFinder exec_finder) throws IOException {
		ScheduledExecutorService max_exec_time_scheduler = Executors.newScheduledThreadPool(1);
		
		ExecProcessText exec = new ExecProcessText("youtube-dl", exec_finder).addParameters("--version").setMaxExecutionTime(10, TimeUnit.SECONDS, max_exec_time_scheduler);
		String version = exec.run().checkExecution().getStdouterr(false, "; ");
		
		log.info("Use youtube-dl version " + version);
		
		FFmpeg ffmpeg = new FFmpeg(exec_finder, new CommandLineProcessor().createEmptyCommandLine("ffmpeg"));
		log.info("Use ffmpeg " + ffmpeg.getAbout().getVersion().header_version);
		
		exec = new ExecProcessText("AtomicParsley", exec_finder).addParameters("-version").setMaxExecutionTime(15, TimeUnit.SECONDS, max_exec_time_scheduler);
		version = exec.run().checkExecution().getStdouterr(false, "; ");
		log.info("Use " + version);
	}
	
	private final ExecutableFinder exec_binary_path;
	private final File out_directory;
	private final ExecutorService message_out_executor;
	private final Config config;
	private final ImageDownload image_download;
	private final FFmpegMuxer ffmpeg_muxer;
	private final MP4Tagger mp4tagger;
	
	public YoutubedlWrapper(ExecutableFinder eb_path, Config config) {
		exec_binary_path = eb_path;
		if (eb_path == null) {
			throw new NullPointerException("\"eb_path\" can't to be null");
		}
		this.config = config;
		if (config == null) {
			throw new NullPointerException("\"config\" can't to be null");
		}
		
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
		
		image_download = new ImageDownload();
		
		message_out_executor = Executors.newFixedThreadPool(1);
		ffmpeg_muxer = new FFmpegMuxer(exec_binary_path, message_out_executor);
		mp4tagger = new MP4Tagger(exec_binary_path, config, message_out_executor);
		
	}
	
	private void youtubedlExec(InteractiveExecProcessHandler interactive_handler, File working_dir, String... user_params) throws IOException {
		ExecProcessText ept = new ExecProcessText("youtube-dl", exec_binary_path);
		
		ept.addBulkParameters("--no-color --no-playlist --retries 3");
		
		String limit_rate = System.getProperty("limit_rate");
		if (limit_rate != null) {
			if (limit_rate.equals("") == false) {
				ept.addParameters("--limit-rate", String.valueOf(limit_rate));
			}
		}
		ept.addParameters(Arrays.asList(user_params));
		ept.setInteractiveHandler(interactive_handler, message_out_executor);
		ept.setWorkingDirectory(working_dir);
		
		ept.run().checkExecution();
	}
	
	private String youtubedlExec(String... user_params) throws IOException {
		return youtubedlExec(new File(System.getProperty("java.io.tmpdir")), user_params);
	}
	
	private String youtubedlExec(File working_dir, String... user_params) throws IOException {
		StringBuffer sb = new StringBuffer();
		
		youtubedlExec((source, line, is_std_err) -> {
			if (is_std_err) {
				System.err.println("Youtube-dl | " + line);
			} else {
				sb.append(line);
				sb.append(System.getProperty("line.separator"));
			}
			return null;
		}, working_dir, user_params);
		
		return sb.toString();
	}
	
	DownloadMedia prepareDownload(URL source_url) throws IOException {
		if (source_url == null) {
			throw new NullPointerException("\"source\" can't to be null");
		}
		
		log.info("Prepare download for " + source_url);
		
		/**
		 * Test if URL is valid.
		 */
		String youtube_id = youtubedlExec("--get-id", source_url.toString());
		log.debug("Id is " + youtube_id);
		
		/*List<String> format_list = Arrays.asList(youtubedlExec("--list-formats", source_url.toString()).split("\\r?\\n"));
		format_list.removeIf(t -> {
			
			 * Header starts like:
			 * [youtube] eVeHV3cnnbI: Downloading webpage
			 * [youtube] eVeHV3cnnbI: Downloading video info webpage
			 * [youtube] eVeHV3cnnbI: Extracting video information
			 * [info] Available formats for eVeHV3cnnbI:
			 * format code extension resolution note
			 * ---
			 
			return t.contains(youtube_id) | t.startsWith("format");
		});
		format_list.forEach(t -> System.out.println(t));
		*/
		return new DownloadMedia(source_url, config);
	}
	
	void download(DownloadMedia media) throws IOException, InterruptedException {
		if (media == null) {
			throw new NullPointerException("\"media\" can't to be null");
		}
		
		media.setJsonMetadata(youtubedlExec("--dump-json", media.getSourceURL().toString()));
		
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
			
			simpleYtDownload(output_file, to_download, media.getMtd());
			
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
				simpleYtDownload(v_outfile, best_vformat, media.getMtd());
			} else {
				log.info("Download audio only " + media.getMtd() + "; " + YoutubeVideoMetadata.readableFileSize(best_aformat.filesize) + "ytes");
			}
			
			File a_outfile = new File(temp_dir.getAbsolutePath() + File.separator + "a-" + best_aformat.format_id + "." + best_aformat.ext);
			simpleYtDownload(a_outfile, best_aformat, media.getMtd());
			
			String ext = config.audiovideo_extension;
			if (media.isOnlyAudio()) {
				ext = config.audioonly_extension;
			}
			
			File mux_outfile = new File(temp_dir.getAbsolutePath() + File.separator + "mux." + ext);
			
			ffmpeg_muxer.muxStreams(media, mux_outfile, v_outfile, a_outfile);
			media.downloadImage(image_download, temp_dir);
			mp4tagger.addTagsToFile(media, mux_outfile, out_directory);
			
			log.debug("Remove " + temp_dir);
			FileUtils.forceDelete(temp_dir);
		}
	}
	
	private void simpleYtDownload(File output_file, YoutubeVideoMetadataFormat format, YoutubeVideoMetadata mtd) throws IOException {
		log.info("Download " + mtd + " download format: \"" + format + "\" to \"" + output_file.getName() + "\"");
		
		youtubedlExec((source, line, is_std_err) -> {
			if (is_std_err) {
				System.err.println("Youtube-dl | " + line);
			} else {
				System.out.println("Youtube-dl | " + line);
			}
			return null;
		}, output_file.getParentFile(), "--format", format.format_id, "--output", output_file.getPath(), mtd.webpage_url);
	}
	
}
