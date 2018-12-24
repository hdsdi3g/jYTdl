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

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.ExecProcessText;
import tv.hd3g.execprocess.ExecutableFinder;

public class EventManager {
	
	private static final Logger log = LogManager.getLogger();
	
	private final ConcurrentHashMap<File, Operation> operations_by_order_file;
	private final ExecutableFinder ebp;
	private final Properties prefs;
	private final YoutubedlWrapper youtube_dl_wrapper;
	
	public EventManager(ExecutableFinder ebp, File out_directory) throws IOException {
		this.ebp = ebp;
		if (ebp == null) {
			throw new NullPointerException("\"ebp\" can't to be null");
		}
		// this.out_directory = out_directory;
		if (out_directory == null) {
			throw new NullPointerException("\"out_directory\" can't to be null");
		} else if (out_directory.exists() == false) {
			throw new RuntimeException("Invalid out_directory: " + out_directory.getPath() + ", don't exists");
		} else if (out_directory.canWrite() == false) {
			throw new RuntimeException("Invalid out_directory: " + out_directory.getPath() + ", can't write");
		} else if (out_directory.isDirectory() == false) {
			throw new RuntimeException("Invalid out_directory: " + out_directory.getPath() + ", is not au directory");
		}
		
		operations_by_order_file = new ConcurrentHashMap<>();
		
		prefs = new Properties();
		prefs.load(new FileReader(new File("prefs.properties")));
		
		youtube_dl_wrapper = new YoutubedlWrapper(ebp, out_directory, prefs);
	}
	
	private final FileParser file_parser = new FileParser();
	
	public void onFoundFile(File new_file) {
		operations_by_order_file.computeIfAbsent(new_file, f -> {
			try {
				log.info("Start scan file " + f);
				
				return new Operation(f, file_parser.getURL(new_file));
			} catch (Exception e) {
				throw new RuntimeException("Can't process url file " + new_file.getName(), e);
			}
		});
		
	}
	
	class Operation {
		
		Operation(File order_file, URL target) throws IOException, InterruptedException {
			if (order_file == null) {
				throw new NullPointerException("\"order_file\" can't to be null");
			}
			youtube_dl_wrapper.download(target);
			
			if (SystemUtils.IS_OS_WINDOWS) {
				try {
					log.debug("Use recycle to delete file " + order_file.getAbsolutePath());
					ExecProcessText ept = new ExecProcessText("recycle", ebp);
					ept.addParameters("-f", order_file.getAbsolutePath());
					ept.run().checkExecution();
					return;
				} catch (FileNotFoundException e) {
					log.warn("Can't found recycle.exe in current PATH");
				}
			}
			
			if (Desktop.isDesktopSupported()) {
				if (Desktop.getDesktop().isSupported(Action.MOVE_TO_TRASH)) {
					log.debug("Use Java Desktop.moveToTrash to delete file " + order_file.getAbsolutePath());
					if (Desktop.getDesktop().moveToTrash(order_file)) {
						return;
					} else {
						log.warn("Can't move to trash " + order_file.getAbsolutePath());
					}
				}
			}
			
			try {
				/**
				 * KDE utility
				 */
				ExecProcessText ept = new ExecProcessText("kioclient", ebp);
				ept.addParameters("move", order_file.getAbsolutePath(), "trash:/");
				log.debug("Use KDE kioclient to delete file " + order_file.getAbsolutePath());
				ept.run().checkExecution();
				return;
			} catch (FileNotFoundException e) {
			}
			
			if (order_file.delete() == false) {
				log.warn("Can't remove " + order_file.getAbsolutePath());
			}
		}
	}
	
}
