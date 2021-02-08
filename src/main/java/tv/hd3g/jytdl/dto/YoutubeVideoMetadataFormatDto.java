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
 * Copyright (C) hdsdi3g for hd3g.tv 2018
 *
 */
package tv.hd3g.jytdl.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ignore http_headers, downloader_options (http_chunk_size), container, format_note, url
 */
public class YoutubeVideoMetadataFormatDto {

	/**
	 * Like "249 - audio only (DASH audio)", "140 - audio only (DASH audio)", "248 - 1920x1080 (1080p)"
	 */
	private final String format;
	/**
	 * Like "vp9", "avc1.640028", "none"
	 */
	private final String vcodec;
	/**
	 * Like "opus", "mp4a.40.2", "none"
	 */
	private final String acodec;
	/**
	 * In kbps
	 * like 56.559, 127.907, 1729.513
	 */
	private final float tbr;
	/**
	 * Like "webm", "m4a"
	 */
	private final String ext;
	private final String formatId;
	/**
	 * In bytes
	 */
	private final long filesize;
	/**
	 * Like 1080
	 */
	private final int height;
	/**
	 * Like 1920
	 */
	private final int width;
	/**
	 * Like 24
	 */
	private final int fps;

	public YoutubeVideoMetadataFormatDto(@JsonProperty("format") final String format, // NOSONAR S107
	                                  @JsonProperty("vcodec") final String vcodec,
	                                  @JsonProperty("acodec") final String acodec,
	                                  @JsonProperty("tbr") final float tbr,
	                                  @JsonProperty("ext") final String ext,
	                                  @JsonProperty("format_id") final String formatId,
	                                  @JsonProperty("filesize") final long filesize,
	                                  @JsonProperty("height") final int height,
	                                  @JsonProperty("width") final int width,
	                                  @JsonProperty("fps") final int fps) {
		this.format = format;
		this.vcodec = vcodec;
		this.acodec = acodec;
		this.tbr = tbr;
		this.ext = ext;
		this.formatId = formatId;
		this.filesize = filesize;
		this.height = height;
		this.width = width;
		this.fps = fps;
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder();
		sb.append(format);
		sb.append("\t");

		if ("none".equals(vcodec) == false) {
			sb.append(vcodec);
			if ("none".equals(acodec) == false) {
				sb.append("+");
			}
		}
		if ("none".equals(acodec) == false) {
			sb.append(acodec);
		}
		sb.append(", ");
		sb.append(tbr);
		sb.append(" kbps (");
		sb.append(filesize);
		sb.append(" bytes)");

		return sb.toString();
	}

	public String getFormat() {
		return format;
	}

	public String getVcodec() {
		return vcodec;
	}

	public String getAcodec() {
		return acodec;
	}

	public float getTbr() {
		return tbr;
	}

	public String getExt() {
		return ext;
	}

	/**
	 * 171, 140, 248
	 */
	public String getFormatId() {
		return formatId;
	}

	public long getFilesize() {
		return filesize;
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	public int getFps() {
		return fps;
	}

}
