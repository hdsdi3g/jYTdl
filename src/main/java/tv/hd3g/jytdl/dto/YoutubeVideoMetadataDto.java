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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ignore here like_count, view_count, dislike_count, average_rating, subtitles, ageLimit, automatic_captions, thumbnails.
 */
public class YoutubeVideoMetadataDto {

	@Override
	public String toString() {
		return "\"" + fulltitle.trim() + "\" by " + uploader + " [" + id + "]";
	}

	private final String id;
	private final int ageLimit;
	/**
	 * Like 20180330, YYYYMMDD
	 */
	private final String uploadDate;
	/**
	 * Like https://www.youtube.com/watch?v=L0xK-S9O9VI
	 */
	private final String webpageUrl;
	/**
	 * In seconds
	 */
	private final int duration;
	private final String fulltitle;
	/**
	 * Like https://www.youtube.com/channel/UCrbn3TbA-oraG8nHuurE1_A
	 */
	private final String uploaderUrl;
	private final String description;
	private final List<String> tags;
	/**
	 * Like "hdsdi3g"
	 */
	private final String uploader;
	/**
	 * Like "hdsdi3g" or "UCrbn3TbA-oraG8nHuurE1_A"
	 */
	private final String uploaderId;
	private final List<String> categories;
	/**
	 * Like "Standard YouTube License"
	 */
	private final String license;
	/**
	 * Like "Youtube"
	 */
	private final String extractorKey;
	/**
	 * URL like https://i.ytimg.com/vi/L0xK-S9O9VI/maxresdefault.jpg
	 */
	private final String thumbnail;
	/**
	 * Like "youtube"
	 */
	private final String extractor;
	private final List<YoutubeVideoMetadataFormatDto> formats;

	public YoutubeVideoMetadataDto(@JsonProperty("id") final String id, // NOSONAR S107
	                               @JsonProperty("age_limit") final int ageLimit,
	                               @JsonProperty("upload_date") final String uploadDate,
	                               @JsonProperty("webpage_url") final String webpageUrl,
	                               @JsonProperty("duration") final int duration,
	                               @JsonProperty("fulltitle") final String fulltitle,
	                               @JsonProperty("uploader_url") final String uploaderUrl,
	                               @JsonProperty("description") final String description,
	                               @JsonProperty("tags") final List<String> tags,
	                               @JsonProperty("uploader") final String uploader,
	                               @JsonProperty("uploader_id") final String uploaderId,
	                               @JsonProperty("categories") final List<String> categories,
	                               @JsonProperty("license") final String license,
	                               @JsonProperty("extractor_key") final String extractorKey,
	                               @JsonProperty("thumbnail") final String thumbnail,
	                               @JsonProperty("extractor") final String extractor,
	                               @JsonProperty("formats") final List<YoutubeVideoMetadataFormatDto> formats) {
		this.id = id;
		this.ageLimit = ageLimit;
		this.uploadDate = uploadDate;
		this.webpageUrl = webpageUrl;
		this.duration = duration;
		this.fulltitle = fulltitle;
		this.uploaderUrl = uploaderUrl;
		this.description = description;
		this.tags = tags;
		this.uploader = uploader;
		this.uploaderId = uploaderId;
		this.categories = categories;
		this.license = license;
		this.extractorKey = extractorKey;
		this.thumbnail = thumbnail;
		this.extractor = extractor;
		this.formats = formats;
	}

	public String getId() {
		return id;
	}

	public int getAgeLimit() {
		return ageLimit;
	}

	public String getUploadDate() {
		return uploadDate;
	}

	public String getWebpageUrl() {
		return webpageUrl;
	}

	public int getDuration() {
		return duration;
	}

	public String getFulltitle() {
		return fulltitle;
	}

	public String getUploaderUrl() {
		return uploaderUrl;
	}

	public String getDescription() {
		return description;
	}

	public List<String> getTags() {
		return tags;
	}

	public String getUploader() {
		return uploader;
	}

	public String getUploaderId() {
		return uploaderId;
	}

	public List<String> getCategories() {
		return categories;
	}

	public String getLicense() {
		return license;
	}

	public String getExtractorKey() {
		return extractorKey;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	public String getExtractor() {
		return extractor;
	}

	public List<YoutubeVideoMetadataFormatDto> getFormats() {
		return formats;
	}

}
