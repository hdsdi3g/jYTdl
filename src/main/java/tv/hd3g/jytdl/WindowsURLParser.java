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
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WindowsURLParser {
	private static final Logger log = LogManager.getLogger();
	
	private final URL url;
	
	public WindowsURLParser(File input_file) throws IOException {
		List<String> lines = FileUtils.readLines(input_file, StandardCharsets.UTF_8);
		
		log.debug("Read " + input_file);
		
		if (lines.size() < 2) {
			throw new IOException("File " + input_file.getName() + " is not an URL file (not enough lines)");
		} else if (lines.get(0).equals("[InternetShortcut]") == false) {
			throw new IOException("File " + input_file.getName() + " is not an URL file (not valid header InternetShortcut)");
		} else if (lines.stream().noneMatch(l -> l.startsWith("URL="))) {
			throw new IOException("File " + input_file.getName() + " is not an URL file (not valid var URL)");
		}
		
		url = new URL(lines.stream().filter(l -> l.startsWith("URL=")).map(l -> l.substring("URL=".length())).findFirst().get());
	}
	
	public URL getURL() {
		return url;
	}
}
