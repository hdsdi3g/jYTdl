/*
 * This file is part of MyDMAM.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2015-2018
 * 
*/
package hd3gtv.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.BinaryOperator;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

public class ExecBinaryPath {
	
	public final static Logger Log = Logger.getLogger(ExecBinaryPath.class);
	
	private final ArrayList<String> paths;
	private final static String[] WINDOWS_EXEC_EXTENTIONS = { "exe", "com", "bat", "cmd" };
	
	private final HashMap<String, File> declared_in_configuration;
	
	public ExecBinaryPath() {
		String[] PATH = System.getenv("PATH").split(File.pathSeparator);
		paths = new ArrayList<>(Arrays.asList(PATH));
		
		/*String user_home = System.getProperty("user.home");
		
		ArrayList<String> all_configured_execpath = Configuration.global.getValues("execpath", "sets", null);
		if (all_configured_execpath != null) {
			all_configured_execpath.forEach(entry -> {
				File test = new File(entry);
				if (test.exists() == false) {
					test = new File(user_home + File.separator + entry);
				}
				
				if (test.exists() && test.isDirectory() && test.canRead()) {
					PATHS.add(test.getAbsolutePath());
				}
			});
		}*/
		
		declared_in_configuration = new HashMap<String, File>();
		
		System.getProperties().forEach((k, v) -> {
			if (((String) k).startsWith("exec.")) {
				File exec = new File((String) v);
				if (validExec(exec)) {
					declared_in_configuration.put(((String) k).substring("exec.".length()), exec);
				} else if (exec.isDirectory() && exec.exists() && exec.canRead()) {
					paths.add(exec.getAbsolutePath());
				}
			} else if (((String) k).startsWith("executable.")) {
				File exec = new File((String) v);
				if (validExec(exec)) {
					declared_in_configuration.put(((String) k).substring("executable.".length()), exec);
				} else if (exec.isDirectory() && exec.exists() && exec.canRead()) {
					paths.add(exec.getAbsolutePath());
				}
			}
		});
		
		/*Map<String, String> values = Configuration.global.getValues("executables");
		
		if (values != null) {
			File exec = null;
			for (Map.Entry<String, String> entry : values.entrySet()) {
				exec = new File(entry.getValue());
				if (validExec(exec)) {
					declared_in_configuration.put(entry.getKey(), exec);
					continue;
				}
				Log.error("Invalid declared_in_configuration executable: " + entry.getKey() + " can't be correctly found in " + exec.getPath());
			}
		}*/
	}
	
	public String getFullPath() {
		return paths.stream().reduce((BinaryOperator<String>) (left, right) -> {
			return left + File.pathSeparator + right;
		}).get();
	}
	
	private static boolean validExec(File exec) {
		if (exec.exists() == false) {
			return false;
		}
		if (exec.isFile() == false) {
			return false;
		}
		if (exec.canRead() == false) {
			return false;
		}
		if (SystemUtils.IS_OS_WINDOWS) {
			for (int pos_w_exe = 0; pos_w_exe < WINDOWS_EXEC_EXTENTIONS.length; pos_w_exe++) {
				if (exec.getName().toLowerCase().endsWith("." + WINDOWS_EXEC_EXTENTIONS[pos_w_exe])) {
					return true;
				}
			}
			return false;
		} else {
			return exec.canExecute();
		}
	}
	
	/**
	 * Can add .exe to name if OS == Windows and if missing.
	 * @throws FileNotFoundException if exec don't exists or is not correctly declared_in_configuration.
	 */
	public File get(String name) throws IOException {
		if (declared_in_configuration.containsKey(name)) {
			return declared_in_configuration.get(name);
		}
		
		File exec = new File(name);
		if (validExec(exec)) {
			return exec;
		}
		
		for (int pos_path = 0; pos_path < paths.size(); pos_path++) {
			exec = new File(paths.get(pos_path) + File.separator + name);
			if (validExec(exec)) {
				return exec;
			}
			if (SystemUtils.IS_OS_WINDOWS) {
				for (int pos_w_exe = 0; pos_w_exe < WINDOWS_EXEC_EXTENTIONS.length; pos_w_exe++) {
					exec = new File(paths.get(pos_path) + File.separator + name + "." + WINDOWS_EXEC_EXTENTIONS[pos_w_exe]);
					if (validExec(exec)) {
						return exec;
					}
				}
			}
		}
		
		throw new IOException("Can't found executable \"" + name + "\"");
	}
	
}
