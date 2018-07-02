/*
 * This file is part of MyDMAM.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2 déc. 2016
 * 
*/
package hd3gtv.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.apache.log4j.Logger;

public class InteractiveConsole implements InteractiveConsoleOrder {
	
	private static Logger log = Logger.getLogger(InteractiveConsole.class);
	
	private LinkedHashMap<String, Action> controller;
	
	public InteractiveConsole() throws NullPointerException {
		controller = new LinkedHashMap<>();
		
		Action a = new Action("quit", "Exit application.", InteractiveConsole.class, (line, out) -> {
			out.println("Exit now");
			System.exit(0);
		});
		controller.put("q", a);
		controller.put("quit", a);
		controller.put("exit", a);
		controller.put("bye", a);
		
		a = new Action("help", "Show help.", InteractiveConsole.class, (line, out) -> {
			out.println("Show help:");
			
			HashSet<Action> actual_actions = new HashSet<>(controller.size());
			
			TableList table = new TableList();
			controller.forEach((order, action) -> {
				if (actual_actions.contains(action) == false) {
					actual_actions.add(action);
					table.addRow("- " + order, action.name, action.description, action.creator.getSimpleName());
				}
			});
			table.print();
			out.println();
			out.println("If you prefix the action by \"l\" like \"l action\", it will be execute in loop.");
			out.println();
		});
		controller.put("h", a);
		controller.put("?", a);
		controller.put("help", a);
	}
	
	private class Action {
		private BiConsumer<String, PrintStream> procedure;
		private String name;
		private String description;
		private Class<?> creator;
		
		Action(String name, String description, Class<?> creator, BiConsumer<String, PrintStream> procedure) {
			this.procedure = procedure;
			if (procedure == null) {
				throw new NullPointerException("\"procedure\" can't to be null");
			}
			this.name = name;
			if (name == null) {
				throw new NullPointerException("\"name\" can't to be null");
			}
			this.description = description;
			if (description == null) {
				throw new NullPointerException("\"description\" can't to be null");
			}
			this.creator = creator;
			if (creator == null) {
				throw new NullPointerException("\"creator\" can't to be null");
			}
		}
		
	}
	
	/**
	 * Blocking !
	 */
	public void waitActions() {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		String line;
		String order;
		String param;
		boolean want_loop = false;
		AtomicBoolean in_loop = new AtomicBoolean(false);
		
		System.out.println(" ========== Start interactive mode =========== ");
		System.out.println(" == Use q for quit, h for help, l for loop  ==");
		System.out.println();
		try {
			while (true) {
				System.out.print("> ");
				line = bufferedReader.readLine().trim();
				if (line.equals("")) {
					continue;
				}
				
				if (line.startsWith("l ")) {
					want_loop = true;
					line = line.substring(2);
				} else if (line.equals("q") && in_loop.get()) {
					in_loop.set(false);
					continue;
				} else {
					want_loop = false;
				}
				
				if (line.indexOf(" ") > -1) {
					order = line.split(" ")[0].toLowerCase();
					param = line.substring(line.indexOf(" ") + 1);
				} else {
					order = line.toLowerCase();
					param = null;
				}
				
				if (controller.containsKey(order) == false) {
					System.out.println("Unknow action \"" + order + "\"");
					continue;
				}
				
				if (want_loop) {
					in_loop.set(true);
					final Action f_order = controller.get(order);
					final String parameter = param;
					Thread t = new Thread(() -> {
						try {
							System.out.println("Enter \"q\" for end loop.");
							Thread.sleep(200);
							while (in_loop.get()) {
								f_order.procedure.accept(parameter, System.out);
								Thread.sleep(1000);
								System.out.println();
							}
						} catch (Exception e) {
							System.out.println("Error during " + f_order);
							e.printStackTrace(System.out);
						}
					}, "ConsolePrintLoop");
					t.setDaemon(true);
					t.start();
					continue;
				}
				
				try {
					controller.get(order).procedure.accept(param, System.out);
				} catch (Exception e) {
					System.out.println("Error during " + order);
					e.printStackTrace(System.out);
				}
				System.out.println();
			}
		} catch (IOException e) {
			log.error("Exit Console mode", e);
		}
	}
	
	public void addConsoleOrder(String order, String name, String description, Class<?> creator, BiConsumer<String, PrintStream> procedure) {
		synchronized (controller) {
			if (controller.containsKey(order)) {
				log.warn("Action " + order + " already exists. Added by " + controller.get(order).creator + " and in conflict with " + creator);
				return;
			}
			controller.put(order.toLowerCase(), new Action(name, description, creator, procedure));
		}
	}
	
}
