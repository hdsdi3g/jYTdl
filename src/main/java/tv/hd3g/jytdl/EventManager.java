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
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.ExecutableFinder;
import tv.hd3g.jytdl.tools.ShortcutFileParser;
import tv.hd3g.jytdl.tools.Trash;

public class EventManager {
	
	private static final Logger log = LogManager.getLogger();
	
	private final HashSet<File> order_files;
	private final Downloader downloader;
	private final ShortcutFileParser file_parser;
	private final Trash trash;
	
	public EventManager(ExecutableFinder exec_binary_path, ShortcutFileParser file_parser, Config config) throws IOException {
		if (exec_binary_path == null) {
			throw new NullPointerException("\"ebp\" can't to be null");
		}
		this.file_parser = file_parser;
		if (file_parser == null) {
			throw new NullPointerException("\"file_parser\" can't to be null");
		}
		if (config == null) {
			throw new NullPointerException("\"config\" can't to be null");
		}
		
		order_files = new HashSet<>();
		
		ExecutorService message_out_executor = Executors.newFixedThreadPool(1);
		downloader = new Downloader(exec_binary_path, config, message_out_executor);
		trash = new Trash(exec_binary_path, config, message_out_executor);
	}
	
	public void onFoundFile(File new_file) {
		if (new_file == null) {
			throw new NullPointerException("\"new_file\" can't to be null");
		}
		
		synchronized (order_files) {
			if (order_files.contains(new_file)) {
				return;
			}
			order_files.add(new_file);
		}
		
		log.info("Start scan file " + new_file);
		try {
			URL url = file_parser.getURL(new_file);
			if (url == null) {
				return;
			}
			
			try {
				downloader.download(url);
			} catch (IOException | InterruptedException e) {
				log.error("Can't process " + url + " from " + new_file, e);
			}
		} catch (IOException e) {
			log.error("Can't process " + new_file.getName(), e);
		}
		
		trash.moveToTrash(new_file);
		
		if (new_file.exists() == false) {
			synchronized (order_files) {
				order_files.remove(new_file);
			}
		}
	}
	
	public synchronized void afterLostFile(File file) {
		log.debug("Remove founded file from internal list");
		order_files.remove(file);
	}
	
}
