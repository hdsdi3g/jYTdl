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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class Config {
	
	public int max_width_res;
	public int max_height_res;
	public String best_aformat;
	public String best_vformat;
	public boolean only_audio;
	public String scan_dir;
	public String out_dir;
	public String audiovideo_extension;
	public String audioonly_extension;
	
	public static Config loadYml(File yml_file) throws IOException {
		Yaml yaml = new Yaml(new Constructor(Config.class));
		FileReader fr = new FileReader(yml_file);
		return yaml.load(fr);
	}
	
	/**
	 * No checks made be here
	 */
	public File getScanDir() {
		if (scan_dir == null) {
			return new File(System.getProperty("user.home") + File.separator + "Desktop");
		} else if (scan_dir.isEmpty()) {
			return new File(System.getProperty("user.home") + File.separator + "Desktop");
		} else {
			return new File(System.getProperty("user.home") + File.separator + scan_dir);
		}
	}
	
	/**
	 * No checks made be here
	 */
	public File getOutDir() {
		if (out_dir == null) {
			return new File(System.getProperty("user.home") + File.separator + "Downloads");
		} else if (out_dir.isEmpty()) {
			return new File(System.getProperty("user.home") + File.separator + "Downloads");
		} else {
			return new File(System.getProperty("user.home") + File.separator + out_dir);
		}
	}
	
}
