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
package tv.hd3g.jytdl.mod.service;

import java.io.File;
import java.net.URL;

import tv.hd3g.jytdl.dto.YoutubeVideoMetadataDto;
import tv.hd3g.jytdl.dto.YoutubeVideoMetadataFormatDto;

public interface YoutubeDlService {

	String getId(URL sourceUrl);

	String getRawJsonMetadata(URL sourceURL);

	void downloadSpecificFormat(File outputFile, YoutubeVideoMetadataFormatDto toDownload, YoutubeVideoMetadataDto mtd);

}
