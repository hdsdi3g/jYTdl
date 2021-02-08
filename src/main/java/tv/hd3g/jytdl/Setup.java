/*
 * This file is part of jtydl.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 *
 */
package tv.hd3g.jytdl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@Configuration
@ComponentScan(basePackages = { "tv.hd3g.jytdl", "tv.hd3g.jobkit.mod" })
public class Setup {

	@Bean
	public ExecutableFinder getExecutableFinder() {
		return new ExecutableFinder();
	}

}
