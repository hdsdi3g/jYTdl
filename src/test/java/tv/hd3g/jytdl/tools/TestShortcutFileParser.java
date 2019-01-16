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
package tv.hd3g.jytdl.tools;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;

public class TestShortcutFileParser extends TestCase {
	
	private final File f_regular_link_desktop;
	private final File f_video_link_desktop;
	private final URL url_regular_link_desktop;
	private final URL url_video_link_desktop;
	
	public TestShortcutFileParser() throws MalformedURLException {
		f_regular_link_desktop = new File(getClass().getClassLoader().getResource("regular-link.desktop").getFile());
		f_video_link_desktop = new File(getClass().getClassLoader().getResource("video-link.desktop").getFile());
		url_regular_link_desktop = new URL("https://www.youtube.com/user/ScottBradleeLovesYa");
		url_video_link_desktop = new URL("https://www.youtube.com/watch?v=aLnZ1NQm2uk");
	}
	
	public void testURLFile() {
		// XXX create windows files
	}
	
	public void testDesktopFile() throws IOException {
		ShortcutFileParser fp = new ShortcutFileParser();
		assertEquals(url_regular_link_desktop, fp.parseFreeDesktopFile(f_regular_link_desktop));
		assertEquals(url_video_link_desktop, fp.parseFreeDesktopFile(f_video_link_desktop));
	}
	
	public void testRoutingType() throws IOException {
		ShortcutFileParser fp = new ShortcutFileParser();
		assertEquals(url_regular_link_desktop, fp.getURL(f_regular_link_desktop));
		assertEquals(url_video_link_desktop, fp.getURL(f_video_link_desktop));
		// XXX add windows files
	}
	
}
