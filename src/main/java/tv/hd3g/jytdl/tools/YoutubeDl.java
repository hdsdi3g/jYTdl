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
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.ExecProcessText;
import tv.hd3g.execprocess.ExecutableFinder;
import tv.hd3g.execprocess.InteractiveExecProcessHandler;
import tv.hd3g.jytdl.Config;
import tv.hd3g.jytdl.MediaAsset;
import tv.hd3g.jytdl.YoutubeVideoMetadata;
import tv.hd3g.jytdl.YoutubeVideoMetadataFormat;

public class YoutubeDl extends Tool {
	
	private static final Logger log = LogManager.getLogger();
	
	public YoutubeDl(ExecutableFinder eb_path, Config config, ExecutorService message_out_executor) {
		super(eb_path, config, message_out_executor);
	}
	
	public void autoTestExecutable(ScheduledExecutorService max_exec_time_scheduler) throws IOException {
		ExecProcessText exec = new ExecProcessText("youtube-dl", exec_binary_path).addParameters("--version").setMaxExecutionTime(10, TimeUnit.SECONDS, max_exec_time_scheduler);
		String version = exec.run().checkExecution().getStdouterr(false, "; ");
		log.info("Use youtube-dl version " + version);
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
	
	public String getId(URL source_url) throws IOException {
		return youtubedlExec("--get-id", source_url.toString());
	}
	
	public void downloadSpecificFormat(File output_file, YoutubeVideoMetadataFormat format, YoutubeVideoMetadata mtd) throws IOException {
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
	
	public String getRawJsonMetadata(URL source) throws IOException {
		return youtubedlExec("--dump-json", source.toString());
	}
	
	public List<String> getFormatList(MediaAsset media) throws IOException {
		List<String> format_list = Arrays.asList(youtubedlExec("--list-formats", media.getSourceURL().toString()).split("\\r?\\n"));
		format_list.removeIf(t -> {
			/**
			 * Header starts like:
			 * [youtube] eVeHV3cnnbI: Downloading webpage
			 * [youtube] eVeHV3cnnbI: Downloading video info webpage
			 * [youtube] eVeHV3cnnbI: Extracting video information
			 * [info] Available formats for eVeHV3cnnbI:
			 * format code extension resolution note
			 * ---
			 */
			return t.contains(media.getMtd().id) | t.startsWith("format");
		});
		return format_list;
	}
	
}
