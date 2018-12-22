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
	private final File out_directory;
	private final Properties prefs;
	
	public EventManager(ExecutableFinder ebp, File out_directory) throws IOException {
		this.ebp = ebp;
		if (ebp == null) {
			throw new NullPointerException("\"ebp\" can't to be null");
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
		
		operations_by_order_file = new ConcurrentHashMap<>();
		
		prefs = new Properties();
		prefs.load(new FileReader(new File("prefs.properties")));
	}
	
	private final WindowsURLParser windows_url_parser = new WindowsURLParser();
	
	public void onFoundURLFile(File new_file) {
		operations_by_order_file.computeIfAbsent(new_file, f -> {
			try {
				log.info("Start scan file " + f);
				
				return new Operation(f, windows_url_parser.getURL(new_file));
			} catch (Exception e) {
				throw new RuntimeException("Can't process url file " + new_file.getName(), e);
			}
		});
		
	}
	
	private final DesktopEntryParser desktop_entry_parser = new DesktopEntryParser();
	
	public void onFoundDesktopFile(File new_file) {
		operations_by_order_file.computeIfAbsent(new_file, f -> {
			try {
				log.info("Start scan file " + f);
				
				return new Operation(f, desktop_entry_parser.getURL(new_file));
			} catch (Exception e) {
				throw new RuntimeException("Can't process desktop file " + new_file.getName(), e);
			}
		});
		
	}
	
	class Operation {
		
		Operation(File order_file, URL target) throws IOException, InterruptedException {
			if (order_file == null) {
				throw new NullPointerException("\"order_file\" can't to be null");
			}
			new YoutubedlWrapper(target, ebp, out_directory, prefs).download();
			
			if (Desktop.isDesktopSupported()) {
				if (SystemUtils.IS_OS_WINDOWS) {
					try {
						ExecProcessText ept = new ExecProcessText("recycle", ebp);
						ept.addParameters("-f", order_file.getAbsolutePath());
						ept.run().checkExecution();
					} catch (FileNotFoundException e) {
						log.warn("Can't found recycle.exe in current PATH");
						
						if (order_file.delete() == false) {
							log.warn("Can't remove " + order_file.getAbsolutePath());
						}
					}
				} else {
					if (Desktop.getDesktop().isSupported(Action.MOVE_TO_TRASH)) {
						if (Desktop.getDesktop().moveToTrash(order_file) == false) {
							log.warn("Can't move to trash " + order_file.getAbsolutePath());
						}
					} else {
						log.warn("Move-to-trash is not supported here. File to delete: " + order_file.getAbsolutePath());
					}
				}
			} else {
				if (order_file.delete() == false) {
					log.warn("Can't remove " + order_file.getAbsolutePath());
				}
			}
		}
	}
	
}
