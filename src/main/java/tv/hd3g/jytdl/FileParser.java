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
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileParser {
	
	private static final Logger log = LogManager.getLogger();
	
	private final HashMap<String, Function<File, URL>> map_file_parser_by_file_extension;
	
	public final String URL_EXT = "url";
	public final String DESKTOP_EXT = "desktop";
	
	public FileParser() {
		map_file_parser_by_file_extension = new HashMap<>();
		map_file_parser_by_file_extension.put(URL_EXT, f -> {
			try {
				return parseWindowsURLFile(f);
			} catch (IOException e) {
				throw new RuntimeException("Can't manage URL file " + f.getPath(), e);
			}
		});
		map_file_parser_by_file_extension.put(DESKTOP_EXT, f -> {
			try {
				return parseFreeDesktopFile(f);
			} catch (IOException e) {
				throw new RuntimeException("Can't manage Freedesktop file " + f.getPath(), e);
			}
		});
	}
	
	public URL getURL(File file) {
		Function<File, URL> parser = map_file_parser_by_file_extension.get(FilenameUtils.getExtension(file.getName()).toLowerCase());
		if (parser == null) {
			throw new RuntimeException("File extension for  " + file.getPath() + " is not managed");
		}
		return parser.apply(file);
	}
	
	URL parseFreeDesktopFile(File input_file) throws IOException {
		List<String> lines = FileUtils.readLines(input_file, StandardCharsets.UTF_8);
		
		log.debug("Read " + input_file);
		
		System.out.println(lines); // XXX
		
		if (lines.size() < 2) {
			throw new IOException("File " + input_file.getName() + " is not an Freedesktop file (not enough lines)");
		} else if (lines.get(0).equals("[Desktop Entry]") == false) {
			throw new IOException("File " + input_file.getName() + " is not an Freedesktop file (not valid header Desktop Entry)");
		} else if (lines.stream().noneMatch(l -> l.startsWith("URL"))) {
			throw new IOException("File " + input_file.getName() + " is not an Freedesktop file (not valid var URL)");
		}
		
		return new URL(lines.stream().filter(l -> l.startsWith("URL")).map(l -> l.substring(l.indexOf("=") + 1)).findFirst().get());
	}
	
	URL parseWindowsURLFile(File input_file) throws IOException {
		List<String> lines = FileUtils.readLines(input_file, StandardCharsets.UTF_8);
		
		log.debug("Read " + input_file);
		
		if (lines.size() < 2) {
			throw new IOException("File " + input_file.getName() + " is not an URL file (not enough lines)");
		} else if (lines.get(0).equals("[InternetShortcut]") == false) {
			throw new IOException("File " + input_file.getName() + " is not an URL file (not valid header InternetShortcut)");
		} else if (lines.stream().noneMatch(l -> l.startsWith("URL="))) {
			throw new IOException("File " + input_file.getName() + " is not an URL file (not valid var URL)");
		}
		
		return new URL(lines.stream().filter(l -> l.startsWith("URL=")).map(l -> l.substring("URL=".length())).findFirst().get());
	}
	
}
