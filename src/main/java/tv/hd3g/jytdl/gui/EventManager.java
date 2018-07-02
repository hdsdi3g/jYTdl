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
package tv.hd3g.jytdl.gui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.apple.propertylist.Dict;
import com.apple.propertylist.Plist;

import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.VideoConst;

public class EventManager {
	
	static {
		System.setProperty("javax.xml.bind.JAXBContextFactory", "org.eclipse.persistence.jaxb.JAXBContextFactory");
	}
	
	private static Logger log = Logger.getLogger(EventManager.class);
	
	// private final ExecutorService order_executor;
	// private final ExecutorService process_executor;
	
	private final ConcurrentHashMap<File, Operation> operations_by_order_file;
	private final Unmarshaller plist_unmarshaller;
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
		
		/*order_executor = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "Order");
			t.setDaemon(true);
			t.setPriority(Thread.MIN_PRIORITY);
			return t;
		});
		
		process_executor = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "Process");
			t.setDaemon(false);
			t.setPriority(Thread.MIN_PRIORITY);
			return t;
		});*/
		
		operations_by_order_file = new ConcurrentHashMap<>();
		
		JAXBContext jc = JAXBContext.newInstance(Plist.class); // "com.apple.propertylist"
		plist_unmarshaller = jc.createUnmarshaller();
		
		plist_unmarshaller.setEventHandler((ValidationEventHandler) e -> {
			ValidationEventLocator localtor = e.getLocator();
			log.warn("XML validation: " + e.getMessage() + " [s" + e.getSeverity() + "] at line " + localtor.getLineNumber() + ", column " + localtor.getColumnNumber() + " offset " + localtor.getOffset() + " node: " + localtor.getNode() + ", object " + localtor.getObject());
			return true;
		});
	}
	
	public void setOnlyAudio(boolean only_audio) {
		this.only_audio = only_audio;
		// return this;
	}
	
	public void onFoundPlistFile(File new_file) {
		operations_by_order_file.computeIfAbsent(new_file, f -> {
			try {
				return new Operation(f);
			} catch (Exception e) {
				throw new RuntimeException("Can't process plist file " + new_file.getName(), e);
			}
		});
	}
	
	public void onLostFile(File old_file) {
		/*Operation old_operation =*/ operations_by_order_file.remove(old_file);
		/*if (old_operation != null) {
			old_operation.cancel();
		}*/
	}
	
	class Operation {
		
		private final URL target;
		private final YoutubedlWrapper cf_yt_wrapper;
		
		Operation(File order_file) throws IOException, JAXBException, ParserConfigurationException, SAXException, InterruptedException {
			if (order_file == null) {
				throw new NullPointerException("\"order_file\" can't to be null");
			}
			log.info("Start scan file " + order_file);
			
			/**
			 * Not the best method to parse a plist...
			 */
			DocumentBuilderFactory xmlDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder xmlDocumentBuilder = xmlDocumentBuilderFactory.newDocumentBuilder();
			xmlDocumentBuilder.setErrorHandler(null);
			Document document = xmlDocumentBuilder.parse(order_file);
			
			JAXBElement<Plist> result = plist_unmarshaller.unmarshal(document, Plist.class);
			Plist plist = result.getValue();
			
			target = plist.getArrayOrDataOrDateOrDictOrRealOrIntegerOrStringOrTrueOrFalse().stream().map(o -> {
				return (Dict) o;
			}).flatMap(dict -> {
				return dict.getKeyOrArrayOrDataOrDateOrDictOrRealOrIntegerOrStringOrTrueOrFalse().stream();
			}).filter(o -> {
				return o instanceof com.apple.propertylist.String;
			}).map(o -> {
				try {
					return new URL(((com.apple.propertylist.String) o).getvalue());
				} catch (MalformedURLException e) {
					throw new RuntimeException("Can't extract URL", e);
				}
			}).findFirst().orElseThrow(() -> new NullPointerException("Can't extract URL from plist file"));
			
			cf_yt_wrapper = new YoutubedlWrapper(target, ebp, out_directory);
			
			cf_yt_wrapper.download(VideoConst.Resolution.valueOf(System.getProperty("getresolution", VideoConst.Resolution.HD_1080.name())), only_audio);
			
			if (Desktop.isDesktopSupported()) {
				if (Desktop.getDesktop().moveToTrash(order_file) == false) {
					log.warn("Can't move to trash " + order_file.getAbsolutePath());
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
