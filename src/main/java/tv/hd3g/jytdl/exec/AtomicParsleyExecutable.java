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

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import tv.hd3g.processlauncher.ExecutableTool;
import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.cmdline.Parameters;

public class AtomicParsleyExecutable implements ExecutableTool {

	private final Parameters parameters;
	private final ScheduledExecutorService maxExecTimeScheduler;

	public AtomicParsleyExecutable(final Parameters parameters, final ScheduledExecutorService maxExecTimeScheduler) {
		this.parameters = Objects.requireNonNull(parameters);
		this.maxExecTimeScheduler = maxExecTimeScheduler;
	}

	@Override
	public void beforeRun(final ProcesslauncherBuilder processBuilder) {
		processBuilder.setExecutionTimeLimiter(10, TimeUnit.MINUTES, maxExecTimeScheduler);
	}

	@Override
	public Parameters getReadyToRunParameters() {
		return parameters;
	}

	@Override
	public String getExecutableName() {
		return "AtomicParsley";
	}
}
