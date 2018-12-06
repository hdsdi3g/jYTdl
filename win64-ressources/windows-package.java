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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public class WindowsPackage {
	
	//TODO update ini file + create external ini + bake icon
	//TODO create zip if it can get ext. exec.
	
	static final String NAME = "jYtdl";
	static final String BASE_JAR_NAME = "jytdl"; // TODO replace by pom parsing...
	static final String[] EXT_DEPS = { "youtube-dl", "ffmpeg", "atomicparsley" };
	
	public static void main(String[] args) throws Exception {
		File get_wd = new File(".").getCanonicalFile();
		
		if (new File(get_wd.getPath() + File.separator + "WinRun4J64.exe").exists()) {
			get_wd = get_wd.getParentFile();
		}
		
		final File target_dir = new File(get_wd.getPath() + File.separator + "target");
		final File dependency_dir = new File(target_dir.getPath() + File.separator + "dependency");
		final File winrun_dir = new File(get_wd.getPath() + File.separator + "win64-ressources");
		final File winrun_dir_exe = new File(winrun_dir.getPath() + File.separator + "WinRun4J64.exe");
		final File winrun_dir_ini = new File(winrun_dir.getPath() + File.separator + "WinRun4J64.ini");
		final File config_dir = new File(get_wd.getPath() + File.separator + "src" + File.separator + "main" + File.separator + "config");
		
		if (target_dir.exists() == false) {
			throw new FileNotFoundException("Can't found dir " + target_dir + ", please invoke maven before");
		} else if (dependency_dir.exists() == false) {
			throw new FileNotFoundException("Can't found dir " + dependency_dir + ", please invoke \"maven dependency:copy-dependencies\" before");
		} else if (winrun_dir.exists() == false) {
			throw new FileNotFoundException("Can't found dir " + winrun_dir);
		} else if (winrun_dir_exe.exists() == false | winrun_dir_ini.exists() == false) {
			throw new FileNotFoundException("Can't found WinRun4J64 exe/ini files in " + winrun_dir);
		} else if (config_dir.exists() == false) {
			throw new FileNotFoundException("Can't found config dir " + config_dir);
		}
		
		final File win_deploy_dir = new File(target_dir.getPath() + File.separator + "win_deploy");
		final File win_deploy_bin_dir = new File(win_deploy_dir.getPath() + File.separator + "bin");
		final File win_deploy_lib_dir = new File(win_deploy_dir.getPath() + File.separator + "lib");
		final File win_deploy_config_dir = new File(win_deploy_dir.getPath() + File.separator + "config");
		
		if (win_deploy_bin_dir.mkdirs() == false) {
			throw new IOException("Can't create dir " + win_deploy_dir);
		}
		/**
		 * Move dependency to lib
		 */
		if (dependency_dir.renameTo(win_deploy_lib_dir) == false) {
			throw new IOException("Can't move dir " + dependency_dir + " to " + win_deploy_dir);
		}
		if (win_deploy_config_dir.mkdirs() == false) {
			throw new IOException("Can't create dir " + win_deploy_dir);
		}
		
		/**
		 * Copy WinRun4J64.exe
		 */
		Files.copy(winrun_dir_exe.toPath(), new File(win_deploy_dir.getPath() + File.separator + NAME + ".exe").toPath());
		Files.copy(winrun_dir_ini.toPath(), new File(win_deploy_dir.getPath() + File.separator + NAME + ".ini").toPath());
		
		/**
		 * Find and move main jar file
		 */
		File main_app_jar = Arrays.stream(target_dir.listFiles((dir, name) -> {
			return name.toLowerCase().startsWith(BASE_JAR_NAME) & name.toLowerCase().endsWith(".jar");
		})).findFirst().orElseThrow(() -> new IOException("Can't found main jar App"));
		
		Files.copy(main_app_jar.toPath(), new File(win_deploy_lib_dir.getAbsolutePath() + File.separator + main_app_jar.getName()).toPath());
		
		/**
		 * Copy conf dir
		 */
		Arrays.stream(config_dir.listFiles((dir, name) -> {
			return name.startsWith(".") == false & new File(dir.getAbsolutePath() + File.separator + name).isHidden() == false;
		})).forEach(config_f -> {
			try {
				Files.copy(config_f.toPath(), new File(win_deploy_config_dir + File.separator + config_f.getName()).toPath());
			} catch (IOException e) {
				throw new RuntimeException("Can't copy " + config_f + " to " + win_deploy_config_dir, e);
			}
		});
		
		System.out.println("Don't forget to add binaries dependencies in " + win_deploy_bin_dir + ":");
		System.out.println(Arrays.stream(EXT_DEPS).collect(Collectors.joining(", ")));
	}
	
}