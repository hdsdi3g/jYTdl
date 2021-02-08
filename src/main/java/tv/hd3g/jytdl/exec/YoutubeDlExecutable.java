/*
 * This file is part of jytdl.
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
package tv.hd3g.jytdl.exec;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import tv.hd3g.commons.IORuntimeException;
import tv.hd3g.processlauncher.ExecutableTool;
import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.cmdline.Parameters;

public class YoutubeDlExecutable implements ExecutableTool {

	private final Parameters parameters;
	private ScheduledExecutorService maxExecTimeScheduler;
	private File workingDirectory;
	private long maxExecTime;

	public YoutubeDlExecutable(final Parameters parameters) {
		this.parameters = Objects.requireNonNull(parameters);
	}

	@Override
	public void beforeRun(final ProcesslauncherBuilder processBuilder) {
		if (maxExecTimeScheduler != null) {
			processBuilder.setExecutionTimeLimiter(maxExecTime, TimeUnit.MILLISECONDS, maxExecTimeScheduler);
		}
		if (workingDirectory != null) {
			try {
				processBuilder.setWorkingDirectory(workingDirectory);
			} catch (final IOException e) {
				throw new IORuntimeException(e);
			}
		}
	}

	public void setMaxExecTime(final long maxExecTime,
	                           final TimeUnit unit,
	                           final ScheduledExecutorService maxExecTimeScheduler) {
		this.maxExecTimeScheduler = Objects.requireNonNull(maxExecTimeScheduler);
		this.maxExecTime = unit.toMillis(maxExecTime);
	}

	public void setWorkingDirectory(final File workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	public void setLimitRate(final int limitRate) {
		if (limitRate < 1) {
			return;
		}
		parameters.prependParameters("--limit-rate", String.valueOf(limitRate));
	}

	@Override
	public Parameters getReadyToRunParameters() {
		return parameters;
	}

	@Override
	public String getExecutableName() {
		return "youtube-dl";
	}
}
