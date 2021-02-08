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
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 *
 */
package tv.hd3g.jytdl.mod.component;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tv.hd3g.processlauncher.CapturedStdOutErrTextInteractive;
import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

@Component
public class Trash {

	private static final Logger log = LogManager.getLogger();
	private static final Logger logProcess = LogManager.getLogger("delete-process");

	@Autowired
	private ExecutableFinder executableFinder;
	@Autowired
	private ScheduledExecutorService maxExecTimeScheduler;

	public void moveToTrash(final File orderFile) {
		if (Desktop.isDesktopSupported() &&
		    Desktop.getDesktop().isSupported(Action.MOVE_TO_TRASH)) {
			log.debug("Use Java Desktop.moveToTrash to delete file {}", orderFile.getAbsolutePath());
			if (Desktop.getDesktop().moveToTrash(orderFile)) {
				return;
			} else {
				log.warn("Can't move to trash {}", orderFile.getAbsolutePath());
			}
		}

		if (SystemUtils.IS_OS_WINDOWS) {
			try {
				log.debug("Use recycle to delete file {}", orderFile.getAbsolutePath());
				runExec("recycle", Parameters.of("-f", orderFile.getAbsolutePath()));
				return;
			} catch (final FileNotFoundException e) {
				log.warn("Can't found recycle.exe in current PATH");
			} catch (final IOException e) {
				log.error("Can't execute recycle.exe", e);
			}
		}

		try {
			/**
			 * KDE utility
			 */
			runExec("kioclient", Parameters.of("move", orderFile.getAbsolutePath(), "trash:/"));
			log.debug("Use KDE kioclient to delete file {}", orderFile.getAbsolutePath());
			return;
		} catch (final IOException e) {
			log.error("Can't execute kioclient", e);
		}

		if (orderFile.delete() == false) {// NOSONAR S4042
			log.warn("Can't remove {}", orderFile.getAbsolutePath());
		}
	}

	public void runExec(final String execName, final Parameters params) throws IOException {
		final var cl = new CommandLine(execName, params, executableFinder);
		final var builder = new ProcesslauncherBuilder(cl);

		final var capture = builder.getSetCaptureStandardOutputAsOutputText();
		capture.addObserver(new CapturedStdOutErrTextInteractive(line -> {
			if (line.isStdErr()) {
				logProcess.error(line);
			} else {
				logProcess.info(line);
			}
			return null;
		}));
		builder.setExecutionTimeLimiter(20, SECONDS, maxExecTimeScheduler);
		builder.start().checkExecution();
	}

}
