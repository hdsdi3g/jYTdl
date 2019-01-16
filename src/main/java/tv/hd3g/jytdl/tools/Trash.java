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
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 * 
*/
package tv.hd3g.jytdl.tools;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.lang.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.ExecProcessText;
import tv.hd3g.execprocess.ExecutableFinder;
import tv.hd3g.jytdl.Config;

public class Trash extends Tool {
	
	private static Logger log = LogManager.getLogger();
	
	public Trash(ExecutableFinder eb_path, Config config, Executor message_out_executor) {
		super(eb_path, config, message_out_executor);
	}
	
	public void autoTestExecutable(ScheduledExecutorService max_exec_time_scheduler) throws IOException {
	}
	
	public void moveToTrash(File order_file) {
		if (SystemUtils.IS_OS_WINDOWS) {
			try {
				log.debug("Use recycle to delete file " + order_file.getAbsolutePath());
				ExecProcessText ept = new ExecProcessText("recycle", exec_binary_path);
				ept.addParameters("-f", order_file.getAbsolutePath());
				ept.run().checkExecution();
				return;
			} catch (FileNotFoundException e) {
				log.warn("Can't found recycle.exe in current PATH");
			} catch (IOException e) {
				log.error("Can't execute recycle.exe", e);
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
			ExecProcessText ept = new ExecProcessText("kioclient", exec_binary_path);
			ept.addParameters("move", order_file.getAbsolutePath(), "trash:/");
			log.debug("Use KDE kioclient to delete file " + order_file.getAbsolutePath());
			ept.run().checkExecution();
			return;
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			log.error("Can't execute kioclient", e);
		}
		
		if (order_file.delete() == false) {
			log.warn("Can't remove " + order_file.getAbsolutePath());
		}
	}
	
}
