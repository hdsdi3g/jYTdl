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
import java.util.concurrent.Executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.CommandLineProcessor;
import tv.hd3g.execprocess.ExecProcessText;
import tv.hd3g.execprocess.ExecutableFinder;
import tv.hd3g.fflauncher.FFLogLevel;
import tv.hd3g.fflauncher.FFmpeg;

public class FFmpegMuxer {
	private static Logger log = LogManager.getLogger();
	
	private final ExecutableFinder exec_binary_path;
	private final Executor message_out_executor;
	
	FFmpegMuxer(ExecutableFinder exec_binary_path, Executor message_out_executor) {
		this.exec_binary_path = exec_binary_path;
		this.message_out_executor = message_out_executor;
	}
	
	public void muxStreams(DownloadMedia media, File mux_outfile, File v_outfile, File a_outfile) throws IOException {
		log.info("Assemble media files " + media.getMtd() + " to " + mux_outfile.getName());
		
		FFmpeg ffmpeg = new FFmpeg(exec_binary_path, new CommandLineProcessor().createEmptyCommandLine("ffmpeg"));
		
		ffmpeg.setHidebanner().setOverwriteOutputFiles().setLogLevel(FFLogLevel.warning, false, false);
		
		if (media.isOnlyAudio() == false) {
			ffmpeg.addSimpleInputSource(v_outfile.getAbsolutePath());
		}
		ffmpeg.addSimpleInputSource(a_outfile.getAbsolutePath());
		
		if (media.isOnlyAudio()) {
			ffmpeg.addAudioCodecName("copy", -1);
		} else {
			ffmpeg.addMap(0, 0);
			ffmpeg.addMap(1, 0);
			ffmpeg.addVsync(1);
			ffmpeg.addVideoCodecName("copy", -1);
			ffmpeg.addAudioCodecName("copy", -1);
		}
		
		if (media.isProcessMp4()) {
			ffmpeg.addFastStartMovMp4File();
		}
		
		ffmpeg.addSimpleOutputDestination(mux_outfile.getAbsolutePath());
		
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
	}
	
}
