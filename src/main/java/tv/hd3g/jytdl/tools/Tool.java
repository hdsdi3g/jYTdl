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

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import tv.hd3g.execprocess.ExecutableFinder;
import tv.hd3g.jytdl.Config;

public abstract class Tool {
	
	protected final ExecutableFinder exec_binary_path;
	protected final Config config;
	protected final Executor message_out_executor;
	
	public Tool(ExecutableFinder eb_path, Config config, Executor message_out_executor) {
		exec_binary_path = eb_path;
		if (eb_path == null) {
			throw new NullPointerException("\"eb_path\" can't to be null");
		}
		this.config = config;
		if (config == null) {
			throw new NullPointerException("\"config\" can't to be null");
		}
		this.message_out_executor = message_out_executor;
		if (message_out_executor == null) {
			throw new NullPointerException("\"message_out_executor\" can't to be null");
		}
	}
	
	public abstract void autoTestExecutable(ScheduledExecutorService max_exec_time_scheduler) throws IOException;
	
}
