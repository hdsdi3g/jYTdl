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
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import tv.hd3g.execprocess.CommandLineProcessor;
import tv.hd3g.execprocess.ExecProcessText;
import tv.hd3g.execprocess.ExecutableFinder;
import tv.hd3g.execprocess.InteractiveExecProcessHandler;
import tv.hd3g.fflauncher.FFLogLevel;
import tv.hd3g.fflauncher.FFmpeg;
import tv.hd3g.jytdl.YoutubeVideoMetadata.Format;

public class YoutubedlWrapper {
	
	private static final Logger log = LogManager.getLogger();
	private static final String atomicparsley_execname = "AtomicParsley";// XXX to conf...
	
	/**
	 * Transform accents to non accented (ascii) version.
	 */
	public static final Pattern PATTERN_Combining_Diacritical_Marks = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	
	static void doChecks(ExecutableFinder exec_finder) throws IOException {
		ScheduledExecutorService max_exec_time_scheduler = Executors.newScheduledThreadPool(1);
		
		ExecProcessText exec = new ExecProcessText("youtube-dl", exec_finder).addParameters("--version").setMaxExecutionTime(10, TimeUnit.SECONDS, max_exec_time_scheduler);
		String version = exec.run().checkExecution().getStdouterr(false, "; ");
		
		log.info("Use youtube-dl version " + version);
		
		FFmpeg ffmpeg = new FFmpeg(exec_finder, new CommandLineProcessor().createEmptyCommandLine("ffmpeg"));
		log.info("Use ffmpeg " + ffmpeg.getAbout().getVersion().header_version);
		
		exec = new ExecProcessText(atomicparsley_execname, exec_finder).addParameters("-version").setMaxExecutionTime(15, TimeUnit.SECONDS, max_exec_time_scheduler);
		version = exec.run().checkExecution().getStdouterr(false, "; ");
		log.info("Use " + version);
	}
	
	private final ExecutableFinder exec_binary_path;
	private final File out_directory;
	private final ExecutorService message_out_executor;
	private final Properties prefs;
	
	public YoutubedlWrapper(ExecutableFinder eb_path, File out_directory, Properties prefs) {
		exec_binary_path = eb_path;
		if (eb_path == null) {
			throw new NullPointerException("\"eb_path\" can't to be null");
		}
		this.out_directory = out_directory;
		if (out_directory == null) {
			throw new NullPointerException("\"out_directory\" can't to be null");
		} else if (out_directory.exists() == false) {
			throw new RuntimeException("Invalid out_directory: " + out_directory.getPath() + ", don't exists");
		} else if (out_directory.canWrite() == false) {
			throw new RuntimeException("Invalid out_directory: " + out_directory.getPath() + ", can't write");
		} else if (out_directory.isDirectory() == false) {
			throw new RuntimeException("Invalid out_directory: " + out_directory.getPath() + ", is not au directory");
		}
		this.prefs = prefs;
		if (prefs == null) {
			throw new NullPointerException("\"prefs\" can't to be null");
		}
		
		message_out_executor = Executors.newFixedThreadPool(1);
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
	
	public static String removeFilenameForbiddenChars(String txt) {
		String l1 = txt.replaceAll("<", "[").replaceAll(">", "]").replaceAll(":", "-").replaceAll("/", "-").replaceAll("\\\\", "-").replaceAll("\\|", " ").replaceAll("\\?", "").replaceAll("\\*", " ").replaceAll("\\\"", "”");
		// Advanced Damage & Decay FX Tutorial! 100% After Effects!
		l1 = l1.replaceAll("&", "-").replaceAll("%", "");
		l1 = l1.replaceAll("        ", " ").replaceAll("       ", " ").replaceAll("      ", " ").replaceAll("     ", " ").replaceAll("    ", " ").replaceAll("   ", " ").replaceAll("  ", " ").replaceAll("---", "-").replaceAll("--", "-");
		return l1;
	}
	
	/**
	 * @return this
	 */
	YoutubedlWrapper download(URL source_url) throws IOException, InterruptedException {
		if (source_url == null) {
			throw new NullPointerException("\"source\" can't to be null");
		}
		log.info("Prepare download for " + source_url);
		
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
		
		String asset_metadatas_json = youtubedlExec("--dump-json", source_url.toString());
		Gson g = new GsonBuilder().setPrettyPrinting().create();
		
		YoutubeVideoMetadata mtd = g.fromJson(asset_metadatas_json, YoutubeVideoMetadata.class);
		
		String normalized_title = removeFilenameForbiddenChars(PATTERN_Combining_Diacritical_Marks.matcher(Normalizer.normalize(mtd.fulltitle, Normalizer.Form.NFD)).replaceAll("").trim());
		String normalized_uploader = removeFilenameForbiddenChars(PATTERN_Combining_Diacritical_Marks.matcher(Normalizer.normalize(mtd.uploader, Normalizer.Form.NFD)).replaceAll("").trim());
		
		String base_out_filename = normalized_title + " ➠ " + normalized_uploader + "  " + mtd.id;
		
		Optional<Format> o_best_aformat = YoutubeVideoMetadata.orderByBitrate(YoutubeVideoMetadata.keepOnlyThisCodec(mtd.getAllAudioOnlyStreams(), prefs.getProperty("best_aformat", "mp4a"))).findFirst();
		
		Optional<Format> o_best_vformat = YoutubeVideoMetadata.orderByBitrate(YoutubeVideoMetadata.orderByVideoResolution(YoutubeVideoMetadata.keepOnlyThisCodec(mtd.getAllVideoOnlyStreams(), prefs.getProperty("best_vformat", "avc1"))).dropWhile(f -> {
			return f.width > Integer.parseInt(prefs.getProperty("max_width_res", "1920")) | f.height > Integer.parseInt(prefs.getProperty("max_height_res", "1080"));
		})).findFirst();
		
		boolean only_audio = Boolean.parseBoolean(prefs.getProperty("only_audio", "false"));
		if (o_best_aformat.isPresent() == false | o_best_vformat.isPresent() == false) {
			if (only_audio && o_best_aformat.isPresent() == false) {
				throw new IOException("Can't found best audio codec for " + mtd + " in only-audio mode");
			}
			
			if (o_best_aformat.isPresent()) {
				log.info("Can't found best video codec for " + mtd);
			} else if (o_best_vformat.isPresent()) {
				log.info("Can't found best audio codec for " + mtd);
			} else {
				log.info("Can't found best audio and video codec for " + mtd);
			}
			
			Optional<Format> o_best_avformat = YoutubeVideoMetadata.orderByVideoResolution(YoutubeVideoMetadata.allAudioVideoMux(mtd.formats.stream())).findFirst();
			
			Format to_download = o_best_avformat.orElseThrow(() -> new RuntimeException("Can't found valid downloadable format for " + mtd));
			
			File output_file = new File(out_directory.getCanonicalPath() + File.separator + base_out_filename + "." + to_download.ext);
			
			if (output_file.exists()) {
				log.warn("Output file exists: \"" + output_file + "\", ignore download operation");
				return this;
			}
			
			simpleYtDownload(output_file, to_download, mtd);
			
			boolean modified = output_file.setLastModified(System.currentTimeMillis());
			if (modified == false) {
				log.warn("Can't change file date " + output_file);
			}
			
			return this;
		}
		
		File temp_dir = new File(out_directory.getCanonicalPath() + File.separator + mtd.extractor_key + " " + mtd.id + " - " + normalized_title);
		
		log.debug("Create dest temp dir " + temp_dir);
		FileUtils.forceMkdir(temp_dir);
		FileUtils.cleanDirectory(temp_dir);
		
		Format a_format = o_best_aformat.get();
		log.debug("Select best audio format: " + a_format);
		Format v_format = null;
		
		File v_outfile = null;
		if (only_audio == false) {
			v_format = o_best_vformat.get();
			log.debug("Select best video format: " + v_format);
			log.info("Download " + mtd + "; " + YoutubeVideoMetadata.computeTotalSizeToDownload(a_format, v_format));
			
			v_outfile = new File(temp_dir.getAbsolutePath() + File.separator + "v-" + v_format.format_id + "." + v_format.ext);
			simpleYtDownload(v_outfile, v_format, mtd);
		} else {
			log.info("Download audio only " + mtd + "; " + YoutubeVideoMetadata.readableFileSize(a_format.filesize) + "ytes");
		}
		
		File a_outfile = new File(temp_dir.getAbsolutePath() + File.separator + "a-" + a_format.format_id + "." + a_format.ext);
		simpleYtDownload(a_outfile, a_format, mtd);
		
		String ext = System.getProperty("outextension", "mp4");
		if (only_audio) {
			ext = System.getProperty("outextension-onlyaudio", "aac");
		}
		
		File mux_outfile = new File(temp_dir.getAbsolutePath() + File.separator + "mux." + ext);
		log.info("Assemble media files " + mtd + " to " + mux_outfile.getName());
		
		FFmpeg ffmpeg = new FFmpeg(exec_binary_path, new CommandLineProcessor().createEmptyCommandLine("ffmpeg"));
		
		ffmpeg.setHidebanner().setOverwriteOutputFiles().setLogLevel(FFLogLevel.warning, false, false);
		
		if (only_audio == false) {
			ffmpeg.addSimpleInputSource(v_outfile.getAbsolutePath());
		}
		ffmpeg.addSimpleInputSource(a_outfile.getAbsolutePath());
		
		if (only_audio) {
			ffmpeg.addAudioCodecName("copy", -1);
		} else {
			ffmpeg.addMap(0, 0);
			ffmpeg.addMap(1, 0);
			ffmpeg.addVsync(1);
			ffmpeg.addVideoCodecName("copy", -1);
			ffmpeg.addAudioCodecName("copy", -1);
		}
		ffmpeg.addFastStartMovMp4File();
		ffmpeg.addSimpleOutputDestination(mux_outfile.getAbsolutePath(), "mp4");
		
		ExecProcessText ept_ffmpeg = ffmpeg.createExec();
		ept_ffmpeg.setInteractiveHandler((source, line, is_std_err) -> {
			if (is_std_err) {
				System.err.println("ffmpeg | " + line);
			} else {
				System.out.println("ffmpeg | " + line);
			}
			return null;
		}, message_out_executor);
		
		ept_ffmpeg.run().checkExecution();
		
		File thumbnail_image = null;
		if (mtd.thumbnail != null) {
			try {
				URL thumbnail = new URL(mtd.thumbnail);
				thumbnail_image = new File(temp_dir.getAbsolutePath() + File.separator + "thumbnail." + FilenameUtils.getExtension(mtd.thumbnail));
				
				log.trace("Wait 0.5 sec before try to download");
				Thread.sleep(500);
				
				log.info("Download thumbnail image " + thumbnail + " to " + thumbnail_image.getName());
				int max_try = 5;
				for (int pos = 0; pos < max_try; pos++) {
					try {
						URLConnection connection = thumbnail.openConnection();
						connection.addRequestProperty("Referer", mtd.webpage_url.toString());
						connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_10) AppleWebKit/543.21 (KHTML, like Gecko) Chrome/65.4.3210.987 Safari/543.21");
						connection.addRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7,de;q=0.6");
						connection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
						connection.addRequestProperty("Cache-Control", "max-age=0");
						connection.setConnectTimeout(30000);
						connection.setReadTimeout(30000);
						
						FileUtils.copyInputStreamToFile(connection.getInputStream(), thumbnail_image);
						break;
					} catch (SocketTimeoutException ste) {
						log.warn("Can't thumbnail download image (try " + (pos + 1) + "/" + max_try + ") from URL: " + mtd.thumbnail + "; " + ste.getMessage());
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
				log.warn("Can't thumbnail download image, invalid URL: " + mtd.thumbnail, mue);
			}
		}
		
		/**
		 * Add mp4 tags
		 */
		final ExecProcessText ept = new ExecProcessText(atomicparsley_execname, exec_binary_path);
		
		ept.addParameters(mux_outfile.getAbsolutePath());
		ept.addParameters("--artist", mtd.uploader);
		ept.addParameters("--title", mtd.fulltitle);
		ept.addParameters("--album", mtd.extractor_key);
		ept.addParameters("--grouping", mtd.extractor_key);
		ept.addParameters("--comment", mtd.description.substring(0, Math.min(255, mtd.description.length())));
		ept.addParameters("--description", mtd.description.substring(0, Math.min(255, mtd.description.length())));
		ept.addParameters("--year", mtd.upload_date.substring(0, 4));
		ept.addParameters("--compilation", "true");
		if (thumbnail_image != null) {
			ept.addParameters("--artwork", thumbnail_image.getAbsolutePath());
		}
		ept.addParameters("--encodingTool", atomicparsley_execname);
		ept.addParameters("--podcastURL", mtd.uploader_url);
		ept.addParameters("--podcastGUID", mtd.webpage_url);
		
		mtd.categories.stream().findFirst().ifPresent(category -> {
			ept.addParameters("--category", category);
		});
		
		if (mtd.license != null) {
			if (mtd.license.trim().isEmpty() == false) {
				ept.addParameters("--copyright", mtd.license);
			}
		}
		
		if (mtd.age_limit > 13) {
			ept.addParameters("--advisory", "explicit");
		}
		
		/**
		 * Available stik settings - case sensitive (number in parens shows the stik value).
		 * (0) Movie
		 * (1) Normal
		 * (2) Audiobook
		 * (5) Whacked Bookmark
		 * (6) Music Video
		 * (9) Short Film
		 * (10) TV Show
		 * (11) Booklet
		 */
		ept.addParameters("--stik", "Normal");
		ept.addParameters("--TVNetwork", mtd.extractor_key);
		ept.addParameters("--TVShowName", mtd.uploader);
		
		if (mtd.tags != null) {
			if (mtd.tags.isEmpty() == false) {
				ept.addParameters("--keyword", mtd.tags.stream().collect(Collectors.joining(",")));
			}
		}
		
		File output_file = new File(out_directory.getCanonicalPath() + File.separator + base_out_filename + "." + System.getProperty("outextension", "mp4"));
		if (only_audio) {
			output_file = new File(out_directory.getCanonicalPath() + File.separator + base_out_filename + "." + System.getProperty("outextension-onlyaudio", "m4a"));
		}
		
		ept.addParameters("--output", output_file.getAbsolutePath());
		
		log.info("Add tags from " + mtd + " to " + output_file.getName());
		
		ept.setInteractiveHandler((source, line, is_std_err) -> {
			if (line.trim().isEmpty()) {
				return null;
			}
			if (is_std_err) {
				System.err.println("atomicparsley | " + line);
			} else {
				if (line.trim().startsWith("Progress:") == false) {
					System.out.println("atomicparsley | " + line);
				}
			}
			return null;
		}, message_out_executor);
		
		ept.run().checkExecution();
		
		log.debug("Remove " + temp_dir);
		FileUtils.forceDelete(temp_dir);
		
		return this;
	}
	
	private void simpleYtDownload(File output_file, Format format, YoutubeVideoMetadata mtd) throws IOException {
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
