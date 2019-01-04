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
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.ExecProcessText;
import tv.hd3g.execprocess.ExecutableFinder;

public class MP4Tagger {
	private static Logger log = LogManager.getLogger();
	
	private final ExecutableFinder exec_binary_path;
	private final Executor message_out_executor;
	
	MP4Tagger(ExecutableFinder exec_binary_path, Executor message_out_executor) {
		this.exec_binary_path = exec_binary_path;
		this.message_out_executor = message_out_executor;
	}
	
	public void addTagsToFile(DownloadMedia media, File mux_outfile, File output_file) throws IOException {
		final ExecProcessText ept = new ExecProcessText("AtomicParsley", exec_binary_path);
		
		ept.addParameters(mux_outfile.getAbsolutePath());
		ept.addParameters("--artist", media.getMtd().uploader);
		ept.addParameters("--title", media.getMtd().fulltitle);
		ept.addParameters("--album", media.getMtd().extractor_key);
		ept.addParameters("--grouping", media.getMtd().extractor_key);
		ept.addParameters("--comment", media.getMtd().description.substring(0, Math.min(255, media.getMtd().description.length())));
		ept.addParameters("--description", media.getMtd().description.substring(0, Math.min(255, media.getMtd().description.length())));
		ept.addParameters("--year", media.getMtd().upload_date.substring(0, 4));
		ept.addParameters("--compilation", "true");
		if (media.getThumbnailImage() != null) {
			ept.addParameters("--artwork", media.getThumbnailImage().getAbsolutePath());
		}
		ept.addParameters("--encodingTool", "AtomicParsley");
		ept.addParameters("--podcastURL", media.getMtd().uploader_url);
		ept.addParameters("--podcastGUID", media.getMtd().webpage_url);
		
		media.getMtd().categories.stream().findFirst().ifPresent(category -> {
			ept.addParameters("--category", category);
		});
		
		if (media.getMtd().license != null) {
			if (media.getMtd().license.trim().isEmpty() == false) {
				ept.addParameters("--copyright", media.getMtd().license);
			}
		}
		
		if (media.getMtd().age_limit > 13) {
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
		ept.addParameters("--TVNetwork", media.getMtd().extractor_key);
		ept.addParameters("--TVShowName", media.getMtd().uploader);
		
		if (media.getMtd().tags != null) {
			if (media.getMtd().tags.isEmpty() == false) {
				ept.addParameters("--keyword", media.getMtd().tags.stream().collect(Collectors.joining(",")));
			}
		}
		
		ept.addParameters("--output", output_file.getAbsolutePath());
		
		log.info("Add tags from " + media.getMtd() + " to " + output_file.getName());
		
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
	}
	
}
