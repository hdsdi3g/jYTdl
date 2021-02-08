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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.LongFunction;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import tv.hd3g.commons.IORuntimeException;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.jytdl.Config;
import tv.hd3g.jytdl.MediaAsset;
import tv.hd3g.jytdl.dto.YoutubeVideoMetadataFormatDto;
import tv.hd3g.jytdl.mod.component.Trash;
import tv.hd3g.transfertfiles.CachedFileAttributes;
import tv.hd3g.transfertfiles.local.LocalFile;

@Service
public class DownloadServiceImpl implements DownloadService {
	private static final Logger log = LogManager.getLogger();
	public static final Set<String> managedExtensions = Set.of("desktop", "url");

	@Autowired
	private Config config;
	@Autowired
	private YoutubeDlService youtubeDl;
	@Autowired
	private FFmpegMuxerService ffmpegMuxer;
	@Autowired
	private AtomicParsleyService atomicParsley;
	@Autowired
	private ImageDownloadService imageDownload;
	@Autowired
	private Trash trash;

	@Override
	public void onAfterScan(final ObservedFolder observedFolder,
	                        final Duration scanTime,
	                        final WatchedFiles scanResult) {
		if (scanResult.getFounded().isEmpty()) {
			return;
		}
		scanResult.getFounded().forEach(this::processFoundedFile);
	}

	public void processFoundedFile(final CachedFileAttributes cfa) {
		final var originalA = cfa.getAbstractFile();
		final var originalFS = originalA.getFileSystem();
		if (originalFS.isAvaliable() == false) {
			originalFS.connect();
		}

		final var buffer = (int) cfa.length();
		final var outputStream = new ByteArrayOutputStream(buffer);
		originalA.downloadAbstract(outputStream, buffer, s -> true);

		final var bufferedReader = new BufferedReader(
		        new InputStreamReader(new ByteArrayInputStream(outputStream.toByteArray()), UTF_8));
		final var lines = bufferedReader.lines().collect(toUnmodifiableList());

		final var ext = FilenameUtils.getExtension(cfa.getName()).toLowerCase();
		final boolean downloaded;
		if (ext.equalsIgnoreCase("desktop")) {
			downloaded = extractMediaFromDesktopLink(lines);
		} else if (ext.equalsIgnoreCase("url")) {
			downloaded = extractMediaFromURLLink(lines);
		} else {
			return;
		}

		if (downloaded == false) {
			return;
		}
		if (originalA instanceof LocalFile) {
			trash.moveToTrash(((LocalFile) originalA).getInternalFile());
		} else {
			originalA.delete();
		}
	}

	/**
	 * Parse Linux's (Open Desktop) desktop file
	 */
	public boolean extractMediaFromDesktopLink(final List<String> lines) {
		if (lines.size() < 2) {
			throw new UnsupportedOperationException("This is not an Freedesktop file (not enough lines)");
		} else if (lines.get(0).equals("[Desktop Entry]") == false) {
			throw new UnsupportedOperationException("This is not an Freedesktop file (not valid header Desktop Entry)");
		} else if (lines.stream().noneMatch(l -> l.startsWith("URL"))) {
			throw new UnsupportedOperationException("This is not an Freedesktop file (not valid var URL)");
		}
		final var url = lines.stream()
		        .filter(l -> l.startsWith("URL"))
		        .map(l -> l.substring(l.indexOf("=") + 1))
		        .findFirst()
		        .orElseThrow(() -> new UnsupportedOperationException("This is an invalid content"));
		try {
			return download(new URL(url));
		} catch (final MalformedURLException | JsonProcessingException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	/**
	 * Parse Windows's url files
	 */
	public boolean extractMediaFromURLLink(final List<String> lines) {
		if (lines.size() < 2) {
			throw new UnsupportedOperationException("This not an URL file (not enough lines)");
		} else if (lines.get(0).equals("[InternetShortcut]") == false) {
			throw new UnsupportedOperationException("This is not an URL file (not valid header InternetShortcut)");
		} else if (lines.stream().noneMatch(l -> l.startsWith("URL="))) {
			throw new UnsupportedOperationException("This is not an URL file (not valid var URL)");
		}

		final var url = lines.stream()
		        .filter(l -> l.startsWith("URL="))
		        .map(l -> l.substring("URL=".length()))
		        .findFirst()
		        .orElseThrow(() -> new UnsupportedOperationException("This is an invalid content"));
		try {
			return download(new URL(url));
		} catch (final MalformedURLException | JsonProcessingException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	public static final Set<String> allowedURL = Set.of("https://www.youtube.com/");

	public boolean download(final URL sourceUrl) throws JsonProcessingException {
		if (allowedURL.stream()
		        .noneMatch(allowed -> sourceUrl.toString().startsWith(allowed))) {
			log.debug("Ignore {}, URL not allowed", sourceUrl);
			return false;
		}

		log.info("Prepare download for {}", sourceUrl);

		final var youtubeId = youtubeDl.getId(sourceUrl);
		log.debug("Id is {}", youtubeId);

		final var media = new MediaAsset(sourceUrl, config);
		media.setJsonMetadata(youtubeDl.getRawJsonMetadata(media.getSourceURL()));

		final var bestAformat = media.getBestAudioStream();
		final var bestVformat = media.getBestVideoStream();

		try {
			if (bestAformat == null || bestVformat == null) {
				downloadNoBestFormat(media, bestAformat, bestVformat);
			} else {
				downloadBestFormat(media, bestAformat, bestVformat);
			}
		} catch (final IOException e) {
			throw new IORuntimeException(e);
		}
		return true;
	}

	private void downloadBestFormat(final MediaAsset media,
	                                final YoutubeVideoMetadataFormatDto bestAformat,
	                                final YoutubeVideoMetadataFormatDto bestVformat) throws IOException {
		final var tempDir = new File(config.getOutDir().getAbsoluteFile(),
		        media.getMtd().getExtractorKey() + "-" + media.getMtd().getId());

		log.debug("Create dest temp dir  {}", tempDir);
		FileUtils.forceMkdir(tempDir);
		FileUtils.cleanDirectory(tempDir);

		log.debug("Select best audio format:  {}", bestAformat);

		final LongFunction<String> readableFileSize = size -> {
			if (size <= 0) {
				return "0";
			}
			final var units = new String[] { "B", "kB", "MB", "GB", "TB" };
			final var digitGroups = (int) (Math.log10(size) / Math.log10(1024));
			return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
		};

		File vOutfile = null;
		if (config.isOnlyAudio() == false) {
			log.debug("Select best video format:  {}", bestVformat);
			if (log.isInfoEnabled()) {
				log.info("Download {}; {}", media.getMtd(),
				        readableFileSize.apply(bestAformat.getFilesize() + bestVformat.getFilesize())
				                                            + "ytes");
			}

			vOutfile = new File(tempDir.getAbsolutePath() + File.separator + "v-" + bestVformat.getFormatId() + "."
			                    + bestVformat.getExt());
			youtubeDl.downloadSpecificFormat(vOutfile, bestVformat, media.getMtd());
		} else if (log.isInfoEnabled()) {
			log.info("Download audio only {}; {}ytes", media.getMtd(),
			        readableFileSize.apply(bestAformat.getFilesize()));
		}

		final var aOutfile = new File(tempDir.getAbsolutePath() + File.separator + "a-" + bestAformat.getFormatId()
		                              + "." + bestAformat.getExt());
		youtubeDl.downloadSpecificFormat(aOutfile, bestAformat, media.getMtd());

		final var finalOutputFile = new File(config.getOutDir().getAbsoluteFile() + File.separator + media
		        .getBaseOutFileName() + "." + media.getExtension());

		if (media.isProcessMp4()) {
			final var muxOutfile = new File(tempDir.getAbsolutePath() + File.separator + "mux." + media
			        .getExtension());
			ffmpegMuxer.muxStreams(true, muxOutfile, vOutfile, aOutfile);
			media.setThumbnailImage(imageDownload.getImageFromMedia(media.getMtd().getThumbnail(),
			        media.getMtd().getWebpageUrl(), tempDir));
			atomicParsley.addTagsToFile(media.getMtd(), media.getThumbnailImage(), muxOutfile, finalOutputFile);
		} else {
			ffmpegMuxer.muxStreams(false, finalOutputFile, vOutfile, aOutfile);
			log.info("Output format is not suitable for image/metadata injection, skip it.");
		}

		log.debug("Remove {}", tempDir);
		FileUtils.forceDelete(tempDir);
	}

	private void downloadNoBestFormat(final MediaAsset media,
	                                  final YoutubeVideoMetadataFormatDto bestAformat,
	                                  final YoutubeVideoMetadataFormatDto bestVformat) throws IOException {
		if (config.isOnlyAudio() && bestAformat == null) {
			throw new IOException("Can't found best audio codec for " + media.getMtd()
			                      + " in only-audio mode");
		}

		if (bestAformat != null) {
			log.info("Can't found best video codec for {}", media.getMtd());
		} else if (bestVformat != null) {
			log.info("Can't found best audio codec for {}", media.getMtd());
		} else {
			log.info("Can't found best audio and video codec for {}", media.getMtd());
		}

		final var oBestAVformat = MediaAsset.orderByVideoResolution(media.getMtd().getFormats().stream())
		        .filter(f -> {
			        if (f.getAcodec() == null || f.getVcodec() == null) {
				        return false;
			        }
			        return f.getAcodec().equalsIgnoreCase("none") == false
			               && f.getVcodec().equalsIgnoreCase("none") == false;
		        })
		        .findFirst();

		final var toDownload = oBestAVformat.orElseThrow(() -> new RuntimeException(
		        "Can't found valid downloadable format for " + media.getMtd()));

		final var outputFile = new File(config.getOutDir().getAbsoluteFile() + File.separator + media
		        .getBaseOutFileName() + "." + toDownload.getExt());

		if (outputFile.exists()) {
			log.warn("Output file exists: \"{}\", ignore download operation", outputFile);
			return;
		}

		youtubeDl.downloadSpecificFormat(outputFile, toDownload, media.getMtd());

		final var modified = outputFile.setLastModified(System.currentTimeMillis());
		if (modified == false) {
			log.warn("Can't change file date {}", outputFile);
		}
	}

}
