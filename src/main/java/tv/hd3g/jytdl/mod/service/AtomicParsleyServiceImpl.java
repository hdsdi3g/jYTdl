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

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tv.hd3g.jytdl.dto.YoutubeVideoMetadataDto;
import tv.hd3g.jytdl.exec.AtomicParsleyExecutable;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

@Service
public class AtomicParsleyServiceImpl implements AtomicParsleyService {
	private static final Logger log = LogManager.getLogger();
	private static final Logger logProcess = LogManager.getLogger("atomicparsley-process");

	@Autowired
	private ExecutableFinder executableFinder;
	@Autowired
	private ScheduledExecutorService maxExecTimeScheduler;
	@Autowired
	private FFmpegMuxerService ffmpegMuxerService;

	@PostConstruct
	public void init() {
		final var output = new AtomicParsleyExecutable(Parameters.of("-version"), maxExecTimeScheduler)
		        .execute(executableFinder)
		        .checkExecutionGetText();
		log.info("Use {}", () -> output.getStdout(false, "; "));
	}

	@Override
	public void addTagsToFile(final YoutubeVideoMetadataDto mtd,
	                          final File thumbnailImage,
	                          final File muxOutfile,
	                          final File finalOutputFile) {
		log.info("Add tags from {} to {}", muxOutfile.getName(), finalOutputFile.getName());
		final var ept = new Parameters();

		ept.addParameters(muxOutfile.getAbsolutePath());
		ept.addParameters("--artist", mtd.getUploader());
		ept.addParameters("--title", mtd.getFulltitle().trim());
		ept.addParameters("--album", mtd.getExtractorKey());
		ept.addParameters("--grouping", mtd.getExtractorKey());
		var description = mtd.getDescription();
		description = description.substring(0, Math.min(255, description.length()));
		ept.addParameters("--comment", description);
		ept.addParameters("--description", description);
		ept.addParameters("--year", mtd.getUploadDate().substring(0, 4));
		ept.addParameters("--compilation", "true");

		if (thumbnailImage != null) {
			final var convertedThumbnailImage = new File(thumbnailImage.getParentFile(), "thumbnail.png");
			ffmpegMuxerService.simpleConvert(thumbnailImage, convertedThumbnailImage);
			ept.addParameters("--artwork", convertedThumbnailImage.getPath());
		}
		ept.addParameters("--encodingTool", "AtomicParsley");
		ept.addParameters("--podcastURL", mtd.getUploaderUrl());
		ept.addParameters("--podcastGUID", mtd.getWebpageUrl());

		mtd.getCategories().stream()
		        .findFirst()
		        .ifPresent(category -> ept.addParameters("--category", category));

		if (mtd.getLicense() != null &&
		    mtd.getLicense().trim().isEmpty() == false) {
			ept.addParameters("--copyright", mtd.getLicense());
		}

		if (mtd.getAgeLimit() > 13) {
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
		ept.addParameters("--TVNetwork", mtd.getExtractorKey());
		ept.addParameters("--TVShowName", mtd.getUploader());

		if (mtd.getTags() != null &&
		    mtd.getTags().isEmpty() == false) {
			ept.addParameters("--keyword", mtd.getTags().stream().collect(Collectors.joining(",")));
		}
		ept.addParameters("--output", finalOutputFile.getAbsolutePath());

		final var exec = new AtomicParsleyExecutable(ept, maxExecTimeScheduler);
		exec.execute(executableFinder, logProcess,
		        line -> {
			        if (line.getLine().equalsIgnoreCase("")) {
				        return null;
			        } else if (line.isStdErr()) {
				        return Level.ERROR;
			        } else if (line.getLine().trim().startsWith("Progress:")) {
				        return Level.DEBUG;
			        } else {
				        return Level.INFO;
			        }
		        })
		        .waitForEndAndCheckExecution();
	}
}
