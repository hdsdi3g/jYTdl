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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "jytdl")
@Validated
public class Config {

	private static final String USER_HOME = System.getProperty("user.home");
	private static final String DIRECTORY = " directory";
	private boolean onlyAudio;
	private File scanDir;
	private File outDir;
	@NotNull
	private List<DownloadPolicyMaxRes> maxRes;
	@NotNull
	private List<DownloadPolicyBestFormat> bestFormat;
	@NotNull
	private DownloadOnlyAudioPolicyBestFormat audioOnlyFormat;
	private int limitRate;

	@PostConstruct
	public void init() throws IOException {
		if (scanDir == null) {
			scanDir = new File(USER_HOME, "Desktop");
		} else if (outDir == null) {
			outDir = new File(USER_HOME, "Downloads");
		}

		if (scanDir.exists() == false) {
			scanDir = new File(USER_HOME, scanDir.getPath());
		}

		if (scanDir.exists() == false) {
			throw new FileNotFoundException("Can't found " + scanDir + DIRECTORY);
		} else if (scanDir.canRead() == false) {
			throw new IOException("Can't read " + scanDir + DIRECTORY);
		} else if (scanDir.isDirectory() == false) {
			throw new IOException(scanDir + " is not a directory");
		}

		if (outDir.exists() == false) {
			outDir = new File(USER_HOME, outDir.getPath());
		}
		if (outDir.exists() == false) {
			throw new FileNotFoundException("Can't found " + outDir + DIRECTORY);
		} else if (scanDir.canRead() == false) {
			throw new IOException("Can't read " + outDir + DIRECTORY);
		} else if (scanDir.isDirectory() == false) {
			throw new IOException(outDir + " is not a directory");
		}
	}

	@Validated
	public static class DownloadPolicyMaxRes {
		@NotNull
		private int w;
		@NotNull
		private int h;

		public int getW() {
			return w;
		}

		public void setW(final int w) {
			this.w = w;
		}

		public int getH() {
			return h;
		}

		public void setH(final int h) {
			this.h = h;
		}
	}

	public interface DwlBestFormat {

		String getExt();

		boolean isProcessMp4();

	}

	@Validated
	public static class DownloadPolicyBestFormat implements DwlBestFormat {
		@NotNull
		private String v;
		@NotNull
		private String a;
		@NotNull
		private String ext;
		private boolean processMp4;

		@Override
		public String toString() {
			final var sb = new StringBuilder();
			if (v != null) {
				sb.append("v:" + v);
				if (a != null) {
					sb.append(", ");
				}
			}
			if (a != null) {
				sb.append("a:" + a);
			}
			sb.append(" ." + ext);
			if (processMp4) {
				sb.append(" MP4 Process");
			}
			return sb.toString();
		}

		public String getV() {
			return v;
		}

		public void setV(final String v) {
			this.v = v;
		}

		public String getA() {
			return a;
		}

		public void setA(final String a) {
			this.a = a;
		}

		@Override
		public String getExt() {
			return ext;
		}

		public void setExt(final String ext) {
			this.ext = ext;
		}

		@Override
		public boolean isProcessMp4() {
			return processMp4;
		}

		public void setProcessMp4(final boolean processMp4) {
			this.processMp4 = processMp4;
		}

	}

	@Validated
	public static class DownloadOnlyAudioPolicyBestFormat implements DwlBestFormat {
		@NotNull
		private String a;
		@NotNull
		private String ext;
		private boolean processMp4;

		@Override
		public String toString() {
			final var sb = new StringBuilder();
			if (a != null) {
				sb.append("a:" + a);
			}
			sb.append(" ." + ext);
			if (processMp4) {
				sb.append(" MP4 Process");
			}
			return sb.toString();
		}

		public String getA() {
			return a;
		}

		public void setA(final String a) {
			this.a = a;
		}

		@Override
		public String getExt() {
			return ext;
		}

		public void setExt(final String ext) {
			this.ext = ext;
		}

		@Override
		public boolean isProcessMp4() {
			return processMp4;
		}

		public void setProcessMp4(final boolean processMp4) {
			this.processMp4 = processMp4;
		}

	}

	public boolean isOnlyAudio() {
		return onlyAudio;
	}

	public void setOnlyAudio(final boolean onlyAudio) {
		this.onlyAudio = onlyAudio;
	}

	public File getScanDir() {
		return scanDir;
	}

	public void setScanDir(final File scanDir) {
		this.scanDir = scanDir;
	}

	public File getOutDir() {
		return outDir;
	}

	public void setOutDir(final File outDir) {
		this.outDir = outDir;
	}

	public List<DownloadPolicyMaxRes> getMaxRes() {
		return maxRes;
	}

	public void setMaxRes(final List<DownloadPolicyMaxRes> maxRes) {
		this.maxRes = maxRes;
	}

	public List<DownloadPolicyBestFormat> getBestFormat() {
		return bestFormat;
	}

	public void setBestFormat(final List<DownloadPolicyBestFormat> bestFormat) {
		this.bestFormat = bestFormat;
	}

	public DownloadOnlyAudioPolicyBestFormat getAudioOnlyFormat() {
		return audioOnlyFormat;
	}

	public void setAudioOnlyFormat(final DownloadOnlyAudioPolicyBestFormat audioOnlyFormat) {
		this.audioOnlyFormat = audioOnlyFormat;
	}

	public int getLimitRate() {
		return limitRate;
	}

	public void setLimitRate(final int limitRate) {
		this.limitRate = limitRate;
	}

}
