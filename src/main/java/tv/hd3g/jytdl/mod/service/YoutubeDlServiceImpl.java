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

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.logging.log4j.Level.DEBUG;
import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.INFO;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tv.hd3g.jytdl.Config;
import tv.hd3g.jytdl.dto.YoutubeVideoMetadataDto;
import tv.hd3g.jytdl.dto.YoutubeVideoMetadataFormatDto;
import tv.hd3g.jytdl.exec.YoutubeDlExecutable;
import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

@Service
public class YoutubeDlServiceImpl implements YoutubeDlService {
	private static final String NO_COLOR_NO_PLAYLIST_RETRIES_3 = "--no-color --no-playlist --retries 3";
	private static final Logger log = LogManager.getLogger();
	private static final Logger logProcess = LogManager.getLogger("ytdl-process");

	@Autowired
	private ExecutableFinder executableFinder;
	@Autowired
	private ScheduledExecutorService maxExecTimeScheduler;
	@Autowired
	private Config config;

	public CapturedStdOutErrTextRetention runYtDl(final Parameters params,
	                                              final long maxExecTime,
	                                              final TimeUnit unit,
	                                              final File workingDirectory) {
		final var ytdl = new YoutubeDlExecutable(params);
		ytdl.setLimitRate(config.getLimitRate());
		ytdl.setWorkingDirectory(workingDirectory);
		ytdl.setMaxExecTime(maxExecTime, unit, maxExecTimeScheduler);

		return ytdl.execute(executableFinder, logProcess,
		        line -> {
			        if (line.getLine().equalsIgnoreCase("")) {
				        return null;
			        } else if (line.isStdErr()) {
				        return ERROR;
			        } else if (line.getLine().trim().startsWith("[download]")) {
				        return DEBUG;
			        } else {
				        return INFO;
			        }
		        })
		        .checkExecutionGetText();
	}

	public CapturedStdOutErrTextRetention runYtDl(final Parameters params,
	                                              final long maxExecTime,
	                                              final TimeUnit unit) {
		return runYtDl(params, maxExecTime, unit, null);
	}

	@PostConstruct
	public void init() {
		final var output = runYtDl(Parameters.of("--version"), 2, SECONDS);
		log.info("Use youtube-dl version {}", () -> output.getStdout(false, "; "));
	}

	@Override
	public String getId(final URL sourceUrl) {
		log.info("Get id for URL: {}", sourceUrl);
		final var p = Parameters.bulk(NO_COLOR_NO_PLAYLIST_RETRIES_3);
		p.addParameters("--get-id", sourceUrl.toString());
		return runYtDl(p, 10, SECONDS)
		        .getStdout(false, System.lineSeparator());
	}

	@Override
	public String getRawJsonMetadata(final URL sourceURL) {
		log.info("Get YT metadatas for URL: {}", sourceURL);
		final var p = Parameters.bulk(NO_COLOR_NO_PLAYLIST_RETRIES_3);
		p.addParameters("--dump-json", sourceURL.toString());
		return runYtDl(p, 10, SECONDS)
		        .getStdout(false, System.lineSeparator());
	}

	@Override
	public void downloadSpecificFormat(final File outputFile,
	                                   final YoutubeVideoMetadataFormatDto toDownload,
	                                   final YoutubeVideoMetadataDto mtd) {
		log.info("Download {} YT format: \"{}\" to \"{}\"", mtd, toDownload, outputFile.getName());
		final var p = Parameters.bulk(NO_COLOR_NO_PLAYLIST_RETRIES_3);
		p.addParameters("--format", toDownload.getFormatId(), "--output", outputFile.getPath(), mtd.getWebpageUrl());
		runYtDl(p, 8, HOURS);
	}

}
