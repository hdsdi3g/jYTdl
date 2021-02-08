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
 * Copyright (C) hdsdi3g for hd3g.tv 2021
 *
 */
package tv.hd3g.jytdl.mod.component;

import java.time.Duration;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.watchfolder.FolderActivity;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFilesInMemoryDb;
import tv.hd3g.jobkit.watchfolder.Watchfolders;
import tv.hd3g.jytdl.Config;
import tv.hd3g.jytdl.mod.service.DownloadServiceImpl;

@Component
public class WatchFolderSetup {
	@Autowired
	private JobKitEngine jobKitEngine;
	@Autowired
	private FolderActivity folderActivity;
	@Valid
	@Autowired
	private Config config;

	@Value("${watchfolder.enabled:true}")
	private boolean enabled;
	@Value("${watchfolder.spoolScans:process}")
	private String spoolScans;
	@Value("${watchfolder.spoolEvents:process}")
	private String spoolEvents;
	@Value("${watchfolder.scanTime:1s}")
	private Duration scanTime;

	@PostConstruct
	public void init() {
		final var oF = new ObservedFolder();
		oF.setTargetFolder("file://localhost/" + config.getScanDir().getAbsolutePath());
		oF.setLabel("videolink");
		oF.setAllowedExtentions(DownloadServiceImpl.managedExtensions);
		oF.setRecursive(false);

		final var watchfolders = new Watchfolders(List.of(oF),
		        folderActivity, scanTime, jobKitEngine,
		        spoolScans, spoolEvents, WatchedFilesInMemoryDb::new);
		watchfolders.startScans();
	}
}
