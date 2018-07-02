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
package tv.hd3g.jytdl.gui;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.Execprocess;
import hd3gtv.tools.ExecprocessEvent;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.VideoConst;
import tv.hd3g.jytdl.gui.YoutubeVideoMetadata.Format;

public class YoutubedlWrapper {
	
	/**
	 * Transform accents to non accented (ascii) version.
	 */
	public static final Pattern PATTERN_Combining_Diacritical_Marks = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	
	private static Logger log = Logger.getLogger(YoutubedlWrapper.class);
	
	static void doChecks(ExecBinaryPath eb_path) throws IOException {
		ExecprocessGettext exec = new ExecprocessGettext(eb_path.get("youtube-dl"), Arrays.asList("--version"), eb_path);
		exec.setMaxexectime(10);
		exec.start();
		log.info("Use youtube-dl version " + exec.getResultstdout().toString());
		
		exec = new ExecprocessGettext(eb_path.get("ffmpeg"), Arrays.asList("-version"), eb_path);
		exec.setMaxexectime(3);
		exec.setEndlinewidthnewline(true);
		exec.start();
		log.info("Use " + exec.getResultstdout().toString().split("\\r?\\n")[0]);
		
		exec = new ExecprocessGettext(eb_path.get("atomicparsley"), Arrays.asList("-version"), eb_path);
		exec.setMaxexectime(3);
		exec.start();
		log.info("Use " + exec.getResultstdout().toString());
	}
	
	private final ExecBinaryPath exec_binary_path;
	
	private final String youtube_id;
	private final YoutubeVideoMetadata mtd;
	private final File out_directory;
	// private final CompletableFuture<?> process_chain;
	
	public YoutubedlWrapper(URL source_url, ExecBinaryPath eb_path, File out_directory) {
		if (source_url == null) {
			throw new NullPointerException("\"source\" can't to be null");
		}
		this.exec_binary_path = eb_path;
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
		
		log.info("Prepare download for " + source_url);
		
		try {
			youtube_id = youtubedlExec("--get-id", source_url.toString());
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
			
			mtd = g.fromJson(asset_metadatas_json, YoutubeVideoMetadata.class);
			
		} catch (IOException e) {
			throw new RuntimeException("Youtubedl has some problems", e);
		}
	}
	
	private String youtubedlExec(String... user_params) throws IOException {
		return youtubedlExec(new File(System.getProperty("java.io.tmpdir")), user_params);
	}
	
	private String youtubedlExec(File working_dir, String... user_params) throws IOException {
		StringBuffer sb = new StringBuffer();
		
		youtubedlExec(out -> {
			sb.append(out);
			sb.append(System.getProperty("line.separator"));
		}, err -> {
			System.err.println("Youtube-dl | " + err);
		}, working_dir, user_params);
		
		return sb.toString();
	}
	
	private void youtubedlExec(Consumer<String> onStdOut, Consumer<String> onStdErr, File working_dir, String... user_params) throws IOException {
		ArrayList<String> params = new ArrayList<>();
		params = new ArrayList<>();
		params.add("--no-color");
		params.add("--no-playlist");
		params.add("--retries");
		params.add("3");
		String limit_rate = System.getProperty("limit_rate");
		if (limit_rate != null) {
			if (limit_rate.equals("") == false) {
				params.add("--limit-rate");
				params.add(limit_rate); // 30000k
			}
		}
		params.addAll(Arrays.asList(user_params));
		
		Execprocess runprocess = new Execprocess(exec_binary_path.get("youtube-dl"), params, new ExecprocessEvent() {
			
			public void onStdout(String message) {
				onStdOut.accept(message);
			}
			
			public void onStderr(String message) {
				onStdErr.accept(message);
			}
			
			public void onError(IOException ioe) {
				log.error("Error during Youtube-dl execution", ioe);
			}
			
		});
		runprocess.setExecBinaryPath(exec_binary_path);
		runprocess.setWorkingDirectory(working_dir);
		
		log.debug("Exec " + runprocess.getCommandline());
		runprocess.run();
		
		if (runprocess.getExitvalue() != 0) {
			throw new IOException("Error with Youtube-dl, return code is " + runprocess.getExitvalue() + ", command line: " + runprocess.getCommandline());
		}
	}
	
	public static String removeFilenameForbiddenChars(String txt) {
		String l1 = txt.replaceAll("<", "[").replaceAll(">", "]").replaceAll(":", "-").replaceAll("/", "-").replaceAll("\\\\", "-").replaceAll("\\|", " ").replaceAll("\\?", "").replaceAll("\\*", " ").replaceAll("\\\"", "”");
		// Advanced Damage & Decay FX Tutorial! 100% After Effects!
		l1 = l1.replaceAll("&", "-").replaceAll("%", "");
		l1 = l1.replaceAll("    ", " ").replaceAll("   ", " ").replaceAll("  ", " ").replaceAll("---", "-").replaceAll("--", "-");
		return l1;
	}
	
	/**
	 * @return this
	 */
	YoutubedlWrapper download(VideoConst.Resolution max_res, boolean only_audio) throws IOException, InterruptedException {
		String normalized_title = removeFilenameForbiddenChars(PATTERN_Combining_Diacritical_Marks.matcher(Normalizer.normalize(mtd.fulltitle, Normalizer.Form.NFD)).replaceAll("").trim());
		String normalized_uploader = removeFilenameForbiddenChars(PATTERN_Combining_Diacritical_Marks.matcher(Normalizer.normalize(mtd.uploader, Normalizer.Form.NFD)).replaceAll("").trim());
		
		String base_out_filename = normalized_title + " ➠ " + normalized_uploader + "  " + mtd.id;
		
		Optional<Format> o_best_aformat = YoutubeVideoMetadata.orderByBitrate(YoutubeVideoMetadata.keepOnlyThisCodec(mtd.getAllAudioOnlyStreams(), "mp4a")).findFirst();
		
		Optional<Format> o_best_vformat = YoutubeVideoMetadata.orderByBitrate(YoutubeVideoMetadata.orderByVideoResolution(YoutubeVideoMetadata.keepOnlyThisCodec(mtd.getAllVideoOnlyStreams(), "avc1")).dropWhile(f -> {
			return f.width > max_res.getWidth() | f.height > max_res.getHeight();
		})).findFirst();
		
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
			
			simpleYtDownload(output_file, to_download);
			
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
			simpleYtDownload(v_outfile, v_format);
		} else {
			log.info("Download audio only " + mtd + "; " + YoutubeVideoMetadata.readableFileSize(a_format.filesize) + "ytes");
		}
		
		File a_outfile = new File(temp_dir.getAbsolutePath() + File.separator + "a-" + a_format.format_id + "." + a_format.ext);
		simpleYtDownload(a_outfile, a_format);
		
		String ext = System.getProperty("outextension", "mp4");
		if (only_audio) {
			ext = System.getProperty("outextension-onlyaudio", "aac");
		}
		
		File mux_outfile = new File(temp_dir.getAbsolutePath() + File.separator + "mux." + ext);
		log.info("Assemble media files " + mtd + " to " + mux_outfile.getName());
		
		ArrayList<String> ff_params = new ArrayList<>();
		ff_params.addAll(Arrays.asList("-y", "-v", "0"));
		if (only_audio == false) {
			ff_params.addAll(Arrays.asList("-i", v_outfile.getAbsolutePath()));
		}
		ff_params.addAll(Arrays.asList("-i", a_outfile.getAbsolutePath()));
		
		if (only_audio) {
			ff_params.addAll(Arrays.asList("-codec:a", "copy"));
		} else {
			ff_params.addAll(Arrays.asList("-map", "0:0", "-map", "1:0", "-vsync", "1", "-codec:v", "copy", "-codec:a", "copy"));
		}
		ff_params.addAll(Arrays.asList("-movflags", "faststart", "-f", "mp4"));
		ff_params.add(mux_outfile.getAbsolutePath());
		
		Execprocess runprocess_ffmpeg = new Execprocess(exec_binary_path.get("ffmpeg"), ff_params, new ExecprocessEvent() {
			
			public void onStdout(String message) {
				System.out.println("ffmpeg | " + message);
			}
			
			public void onStderr(String message) {
				System.err.println("ffmpeg | " + message);
			}
			
			public void onError(IOException ioe) {
				log.error("Error during ffmpeg execution", ioe);
			}
			
		});
		runprocess_ffmpeg.setExecBinaryPath(exec_binary_path);
		
		log.debug("Exec " + runprocess_ffmpeg.getCommandline());
		runprocess_ffmpeg.run();
		
		if (runprocess_ffmpeg.getExitvalue() != 0) {
			throw new IOException("Error with ffmpeg, return code is " + runprocess_ffmpeg.getExitvalue());
		}
		
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
							Thread.sleep(1000 + (pos * 1000));
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
		ArrayList<String> ap_params = new ArrayList<>();
		ap_params.add(mux_outfile.getAbsolutePath());
		ap_params.add("--artist");
		ap_params.add(mtd.uploader);
		ap_params.add("--title");
		ap_params.add(mtd.fulltitle);
		ap_params.add("--album");
		ap_params.add(mtd.extractor_key);
		ap_params.add("--grouping");
		ap_params.add(mtd.extractor_key);
		ap_params.add("--comment");
		ap_params.add(mtd.description.substring(0, Math.min(255, mtd.description.length())));
		ap_params.add("--description");
		ap_params.add(mtd.description.substring(0, Math.min(255, mtd.description.length())));
		ap_params.add("--year");
		ap_params.add(mtd.upload_date.substring(0, 4));
		ap_params.add("--compilation");
		ap_params.add("true");
		if (thumbnail_image != null) {
			ap_params.add("--artwork");
			ap_params.add(thumbnail_image.getAbsolutePath());
		}
		ap_params.add("--encodingTool");
		ap_params.add("Atomicparsley");
		
		ap_params.add("--podcastURL");
		ap_params.add(mtd.uploader_url);
		ap_params.add("--podcastGUID");
		ap_params.add(mtd.webpage_url);
		
		mtd.categories.stream().findFirst().ifPresent(category -> {
			ap_params.add("--category");
			ap_params.add(category);
		});
		
		ap_params.add("--copyright");
		ap_params.add(mtd.license);
		
		if (mtd.age_limit > 13) {
			ap_params.add("--advisory");
			ap_params.add("explicit");
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
		ap_params.add("--stik");
		ap_params.add("Normal");
		
		ap_params.add("--TVNetwork");
		ap_params.add(mtd.extractor_key);
		
		ap_params.add("--TVShowName");
		ap_params.add(mtd.uploader);
		
		if (mtd.tags != null) {
			if (mtd.tags.isEmpty() == false) {
				ap_params.add("--keyword");
				ap_params.add(mtd.tags.stream().collect(Collectors.joining(",")));
			}
		}
		
		File output_file = new File(out_directory.getCanonicalPath() + File.separator + base_out_filename + "." + System.getProperty("outextension", "mp4"));
		if (only_audio) {
			output_file = new File(out_directory.getCanonicalPath() + File.separator + base_out_filename + "." + System.getProperty("outextension-onlyaudio", "m4a"));
		}
		
		ap_params.add("--output");
		ap_params.add(output_file.getAbsolutePath());
		
		log.info("Add tags from " + mtd + " to " + output_file.getName());
		
		Execprocess runprocess_atomicparsley = new Execprocess(exec_binary_path.get("atomicparsley"), ap_params, new ExecprocessEvent() {
			
			public void onStdout(String message) {
				if (message.trim().isEmpty()) {
					return;
				}
				if (message.trim().startsWith("Progress:") == false) {
					System.out.println("atomicparsley | " + message);
				}
			}
			
			public void onStderr(String message) {
				System.err.println("atomicparsley | " + message);
			}
			
			public void onError(IOException ioe) {
				log.error("Error during atomicparsley execution", ioe);
			}
			
		});
		runprocess_atomicparsley.setExecBinaryPath(exec_binary_path);
		
		log.debug("Exec " + runprocess_atomicparsley.getCommandline());
		runprocess_atomicparsley.run();
		
		if (runprocess_atomicparsley.getExitvalue() != 0) {
			throw new IOException("Error with atomicparsley, return code is " + runprocess_atomicparsley.getExitvalue());
		}
		
		log.debug("Remove " + temp_dir);
		FileUtils.forceDelete(temp_dir);
		
		return this;
	}
	
	private void simpleYtDownload(File output_file, Format format) throws IOException {
		log.info("Download " + mtd + " download format: \"" + format + "\" to \"" + output_file.getName() + "\"");
		
		youtubedlExec(out -> {
			System.out.println("Youtube-dl | " + out);
		}, err -> {
			System.err.println("Youtube-dl | " + err);
		}, output_file.getParentFile(), "--format", format.format_id, "--output", output_file.getPath(), mtd.webpage_url);
	}
	
}
