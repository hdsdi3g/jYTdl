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
package tv.hd3g.jytdl;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.io.File;
import java.net.URL;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tv.hd3g.jytdl.Config.DownloadPolicyBestFormat;
import tv.hd3g.jytdl.Config.DwlBestFormat;
import tv.hd3g.jytdl.dto.YoutubeVideoMetadataDto;
import tv.hd3g.jytdl.dto.YoutubeVideoMetadataFormatDto;

public class MediaAsset {

	private static final Logger log = LogManager.getLogger();
	private static final ObjectMapper jsonDeserializer = new ObjectMapper()
	        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

	/**
	 * Transform accents to non accented (ascii) version.
	 */
	public static final Pattern PATTERN_Combining_Diacritical_Marks = Pattern.compile(
	        "\\p{InCombiningDiacriticalMarks}+");

	private final URL sourceUrl;
	private final Config config;

	private YoutubeVideoMetadataDto mtd;
	private String normalizedTitle;
	private String normalizedUploader;
	private YoutubeVideoMetadataFormatDto bestVstream;
	private YoutubeVideoMetadataFormatDto bestAstream;
	private DwlBestFormat bestFormat;
	private File thumbnailImage;

	public MediaAsset(final URL sourceUrl, final Config config) {
		this.sourceUrl = sourceUrl;
		this.config = config;
	}

	public URL getSourceURL() {
		return sourceUrl;
	}

	public static String removeFilenameForbiddenChars(final String txt) {
		return txt.replace("<", "[")
		        .replace(">", "]")
		        .replace(":", "-")
		        .replace("/", "-")
		        .replace("\\", "-")
		        .replace("|", " ")
		        .replace("?", "")
		        .replace("*", " ")
		        .replace("\"", "”")
		        .replace("&", "-")
		        .replace("%", "")
		        .replace("        ", " ")
		        .replace("       ", " ")
		        .replace("      ", " ")
		        .replace("     ", " ")
		        .replace("    ", " ")
		        .replace("   ", " ")
		        .replace("  ", " ")
		        .replace("---", "-")
		        .replace("--", "-");
	}

	private static Stream<YoutubeVideoMetadataFormatDto> selectBestCodec(final Stream<YoutubeVideoMetadataFormatDto> aVFormats,
	                                                                     final String base_codec_name) {
		return aVFormats.filter(f -> {
			if (f.getAcodec() != null && f.getAcodec().equalsIgnoreCase("none") == false) {
				return f.getAcodec().toLowerCase().startsWith(base_codec_name.toLowerCase());
			}
			if (f.getVcodec() != null && f.getVcodec().equalsIgnoreCase("none") == false) {
				return f.getVcodec().toLowerCase().startsWith(base_codec_name.toLowerCase());
			}
			return false;
		});
	}

	/**
	 * @return bigger to smaller
	 */
	public static Stream<YoutubeVideoMetadataFormatDto> orderByVideoResolution(final Stream<YoutubeVideoMetadataFormatDto> videoFormats) {
		return videoFormats.filter(f -> {
			if (f.getVcodec() == null) {
				return false;
			}
			return f.getVcodec().equalsIgnoreCase("none") == false;
		}).sorted((l, r) -> (r.getHeight() * r.getWidth() - l.getHeight() * l.getWidth()));
	}

	public void setJsonMetadata(final String asset_metadatas_json) throws JsonProcessingException {
		mtd = jsonDeserializer.readValue(asset_metadatas_json, YoutubeVideoMetadataDto.class);

		normalizedTitle = removeFilenameForbiddenChars(PATTERN_Combining_Diacritical_Marks.matcher(Normalizer
		        .normalize(mtd.getFulltitle().trim(), Normalizer.Form.NFD)).replaceAll("").trim()).trim();
		normalizedUploader = removeFilenameForbiddenChars(PATTERN_Combining_Diacritical_Marks.matcher(Normalizer
		        .normalize(mtd.getUploader(), Normalizer.Form.NFD)).replaceAll("").trim()).trim();

		/**
		 * bigger to smaller
		 */
		final Comparator<YoutubeVideoMetadataFormatDto> orderByBitrate = (l, r) -> Math.round(r.getTbr() - l.getTbr());

		final var allAudioOnlyStreams = mtd.getFormats().stream()
		        .filter(f -> {
			        if (f.getAcodec() == null
			            || f.getVcodec() != null && f.getVcodec().equalsIgnoreCase("none") == false) {
				        return false;
			        }
			        return f.getAcodec().equalsIgnoreCase("none") == false;
		        }).collect(toUnmodifiableList());

		if (config.isOnlyAudio()) {
			final var bestAudioFormat = config.getAudioOnlyFormat();
			bestFormat = bestAudioFormat;
			bestAstream = selectBestCodec(allAudioOnlyStreams.stream(), bestAudioFormat.getA())
			        .sorted(orderByBitrate)
			        .findFirst()
			        .orElse(null);
		} else {
			List<YoutubeVideoMetadataFormatDto> allBestVstream = config.getMaxRes().stream().flatMap(
			        configRes -> config.getBestFormat().stream()
			                .map(DownloadPolicyBestFormat::getV)
			                .flatMap(configBestVformat -> selectBestCodec(mtd.getFormats().stream()
			                        .filter(f -> {
				                        if (f.getVcodec() == null
				                            || f.getAcodec() != null
				                               && f.getAcodec().equalsIgnoreCase("none") == false) {
					                        return false;
				                        }
				                        return f.getVcodec().equalsIgnoreCase("none") == false;
			                        }), configBestVformat)))
			        .collect(toUnmodifiableList());

			allBestVstream = orderByVideoResolution(allBestVstream.stream().sorted(orderByBitrate))
			        .collect(toUnmodifiableList());

			bestVstream = allBestVstream.stream().findFirst().orElse(null);

			bestAstream = config.getBestFormat().stream()
			        .map(DownloadPolicyBestFormat::getA)
			        .flatMap(configBestAformat -> selectBestCodec(allAudioOnlyStreams.stream(), configBestAformat)
			                .sorted(orderByBitrate))
			        .findFirst()
			        .orElse(null);

			bestFormat = config.getBestFormat().stream()
			        .filter(format -> bestVstream.getVcodec().startsWith(format.getV()))
			        .findFirst().orElse(config.getBestFormat().get(0));
		}

		log.info("Wanted format: {}, selected: {}, {}", bestFormat, bestVstream, bestAstream);
	}

	public YoutubeVideoMetadataDto getMtd() {
		return mtd;
	}

	public String getNormalizedTitle() {
		return normalizedTitle;
	}

	public String getNormalizedUploader() {
		return normalizedUploader;
	}

	public String getBaseOutFileName() {
		return normalizedTitle + " ➠ " + normalizedUploader + "  " + mtd.getId();
	}

	public String getExtension() {
		return bestFormat.getExt();
	}

	public boolean isProcessMp4() {
		return bestFormat.isProcessMp4();
	}

	/**
	 * @return can be null
	 */
	public YoutubeVideoMetadataFormatDto getBestVideoStream() {
		return bestVstream;
	}

	/**
	 * @return can be null
	 */
	public YoutubeVideoMetadataFormatDto getBestAudioStream() {
		return bestAstream;
	}

	public void setThumbnailImage(final File thumbnailImage) {
		this.thumbnailImage = thumbnailImage;
	}

	/**
	 * @return can be null.
	 */
	public File getThumbnailImage() {
		return thumbnailImage;
	}

}
