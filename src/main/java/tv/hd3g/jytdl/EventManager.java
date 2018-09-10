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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.Execprocess;
import hd3gtv.tools.VideoConst;

public class EventManager {
	
	static {
		System.setProperty("javax.xml.bind.JAXBContextFactory", "org.eclipse.persistence.jaxb.JAXBContextFactory");
	}
	
	private static Logger log = Logger.getLogger(EventManager.class);
	
	// private final ExecutorService order_executor;
	// private final ExecutorService process_executor;
	
	private final ConcurrentHashMap<File, Operation> operations_by_order_file;
	private final ExecBinaryPath ebp;
	private final File out_directory;
	private boolean only_audio;
	
	public EventManager(ExecBinaryPath ebp, File out_directory) throws JAXBException {
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
	}
	
	public void setOnlyAudio(boolean only_audio) {
		this.only_audio = only_audio;
		// return this;
	}
	
	public void onFoundURLFile(File new_file) {
		operations_by_order_file.computeIfAbsent(new_file, f -> {
			try {
				log.info("Start scan file " + f);
				
				return new Operation(f, new WindowsURLParser(new_file).getURL());
			} catch (Exception e) {
				throw new RuntimeException("Can't process url file " + new_file.getName(), e);
			}
		});
		
	}
	
	class Operation {
		
		private final YoutubedlWrapper cf_yt_wrapper;
		
		Operation(File order_file, URL target) throws IOException, InterruptedException {
			if (order_file == null) {
				throw new NullPointerException("\"order_file\" can't to be null");
			}
			cf_yt_wrapper = new YoutubedlWrapper(target, ebp, out_directory);
			cf_yt_wrapper.download(VideoConst.Resolution.valueOf(System.getProperty("getresolution", VideoConst.Resolution.HD_1080.name())), only_audio);// XXX externalize maxres && out codec while list
			
			if (Desktop.isDesktopSupported()) {
				if (SystemUtils.IS_OS_WINDOWS) {
					try {
						Execprocess ep = new Execprocess(ebp.get("recycle.exe"), Arrays.asList("-f", order_file.getAbsolutePath()));
						ep.run();
						if (ep.getExitvalue() != 0) {
							log.error("Can't exec recycle.exe");
							throw new FileNotFoundException();
						}
					} catch (FileNotFoundException e) {
						log.warn("Can't found recycle.exe in current PATH");
						
						if (order_file.delete() == false) {
							log.warn("Can't remove " + order_file.getAbsolutePath());
						}
					}
				} else {
					if (Desktop.getDesktop().moveToTrash(order_file) == false) {
						log.warn("Can't move to trash " + order_file.getAbsolutePath());
					}
				}
			} else {
				if (order_file.delete() == false) {
					log.warn("Can't remove " + order_file.getAbsolutePath());
				}
			}
		}
		
		/*void cancel() {
			if (cf_yt_wrapper.isDone() == false) {
				log.info("Remove tasks for " + order_file);
				cf_yt_wrapper.cancel(false);
				cf_yt_wrapper.completeExceptionally(new InterruptedException("Canceled task"));
			} else {
				try {
					CompletableFuture<?> p_chain = cf_yt_wrapper.get().getProcess_chain();
					
					if (p_chain.isDone() == false) {
						log.info("Remove tasks for " + order_file);
						p_chain.cancel(false);
						p_chain.completeExceptionally(new InterruptedException("Canceled task"));
					}
				} catch (InterruptedException | ExecutionException e) {
					log.warn("Can't get task", e);
				}
			}
		}*/
		
	}
	
	/*void waitToClose() {
		order_executor.shutdown();
		try {
			order_executor.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
	}*/
	
}
