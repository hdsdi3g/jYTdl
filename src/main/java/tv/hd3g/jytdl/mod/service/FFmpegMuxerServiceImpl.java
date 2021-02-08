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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2021
 *
 */
package tv.hd3g.jytdl.mod.service;

import static java.lang.System.lineSeparator;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tv.hd3g.fflauncher.FFbase;
import tv.hd3g.fflauncher.FFmpeg;
import tv.hd3g.fflauncher.enums.FFLogLevel;
import tv.hd3g.jytdl.Config;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

@Service
public class FFmpegMuxerServiceImpl implements FFmpegMuxerService {
	private static final String FFMPEG = "ffmpeg";

	private static final Logger log = LogManager.getLogger();

	@Autowired
	private ExecutableFinder executableFinder;
	@Autowired
	private ScheduledExecutorService maxExecTimeScheduler;
	@Autowired
	private Config config;

	@PostConstruct
	public void init() {
		final var base = new FFbase(FFMPEG, new Parameters());
		base.setMaxExecTimeScheduler(maxExecTimeScheduler);
		final var version = base.getAbout(executableFinder).getVersion().headerVersion;
		log.info("Use ffmpeg about {}", version);
	}

	@Override
	public void muxStreams(final boolean processMp4, final File muxOutFile, final File vOutFile, final File aOutFile) {
		log.info("Assemble media files to {}", muxOutFile.getName());

		final var ffmpeg = new FFmpeg(FFMPEG, new Parameters());
		ffmpeg.setLogLevel(FFLogLevel.INFO, false, false);
		ffmpeg.setHidebanner();
		ffmpeg.setOnErrorDeleteOutFiles(true);
		ffmpeg.setOverwriteOutputFiles();

		if (config.isOnlyAudio() == false) {
			ffmpeg.addSimpleInputSource(vOutFile);
		}
		ffmpeg.addSimpleInputSource(aOutFile);
		ffmpeg.checkSources();

		if (config.isOnlyAudio()) {
			ffmpeg.addAudioCodecName("copy", -1);
		} else {
			ffmpeg.addMap(0, 0);
			ffmpeg.addMap(1, 0);
			ffmpeg.addVideoCodecName("copy", -1);
			ffmpeg.addAudioCodecName("copy", -1);
		}

		if (processMp4) {
			ffmpeg.addFastStartMovMp4File();
		}
		ffmpeg.addSimpleOutputDestination(muxOutFile);

		execFFmpeg(muxOutFile, ffmpeg);
	}

	private void execFFmpeg(final File outFile, final FFmpeg ffmpeg) {
		if (log.isDebugEnabled()) {
			final var sysOut = ffmpeg.execute(executableFinder)
			        .waitForEnd().checkExecutionGetText()
			        .getStdout(false, lineSeparator() + " \\ ");
			log.debug("ffmpeg sysout for {}: {}", outFile, sysOut);
		} else {
			ffmpeg.execute(executableFinder).waitForEndAndCheckExecution();
		}
	}

	@Override
	public void simpleConvert(final File thumbnailImage, final File convertedThumbnailImage) {
		final var ffmpeg = new FFmpeg(FFMPEG, new Parameters());
		ffmpeg.setLogLevel(FFLogLevel.INFO, false, false);
		ffmpeg.setHidebanner();
		ffmpeg.setOnErrorDeleteOutFiles(true);
		ffmpeg.setOverwriteOutputFiles();

		ffmpeg.addSimpleInputSource(thumbnailImage);
		ffmpeg.checkSources();
		ffmpeg.addSimpleOutputDestination(convertedThumbnailImage);
		execFFmpeg(convertedThumbnailImage, ffmpeg);
	}
}
