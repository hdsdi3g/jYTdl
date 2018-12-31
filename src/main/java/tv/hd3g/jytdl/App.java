package tv.hd3g.jytdl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.ExecutableFinder;

public class App {
	
	private static final Logger log = LogManager.getLogger();
	
	public static void main(String[] args) throws Exception {
		
		File config_file = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator)).map(p -> {
			return new File(p);
		}).filter(f -> {
			return f.exists() && f.isDirectory() && f.canRead();
		}).flatMap(f -> Arrays.stream(f.listFiles((dir, name) -> {
			return name.equals("config.yml");
		}))).findFirst().orElseThrow(() -> new FileNotFoundException("Can't found config.yml in classpath"));
		
		Config config = Config.loadYml(config_file);
		
		/**
		 * Async test YoutubedlWrapper
		 */
		ExecutableFinder ex_finder = new ExecutableFinder();
		Thread t = new Thread(() -> {
			try {
				YoutubedlWrapper.doChecks(ex_finder);
			} catch (IOException e) {
				log.fatal("Checking error", e);
				System.exit(1);
			}
		}, "ChkExternalTools");
		t.setDaemon(true);
		t.setPriority(Thread.MAX_PRIORITY);
		t.start();
		
		// TODO 4k problem: mp4 don't have 4k, but webm, yes.
		
		File scan_dir = config.getScanDir();
		if (scan_dir.exists() == false) {
			throw new FileNotFoundException("Can't found " + scan_dir + " directory");
		} else if (scan_dir.canRead() == false) {
			throw new IOException("Can't read " + scan_dir + " directory");
		} else if (scan_dir.isDirectory() == false) {
			throw new IOException(scan_dir + " is not a directory");
		}
		
		WatchService watcher = FileSystems.getDefault().newWatchService();
		scan_dir.toPath().register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.OVERFLOW);
		
		FileParser file_parser = new FileParser();
		EventManager event_manager = new EventManager(ex_finder, file_parser, config);
		
		/**
		 * Process actual files
		 */
		FileUtils.iterateFiles(scan_dir, FileParser.ALL_MANAGED_EXTENSIONS, false).forEachRemaining(file_parser.validateExtension(f -> {
			event_manager.onFoundFile(f.getAbsoluteFile());
		}));
		
		while (true) {
			WatchKey key = watcher.take();
			
			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
				
				if (kind == StandardWatchEventKinds.OVERFLOW) {
					continue;
				}
				
				@SuppressWarnings("unchecked")
				Path filename = ((WatchEvent<Path>) event).context();
				File event_from_file = scan_dir.toPath().resolve(filename).toFile();
				String ext = FilenameUtils.getExtension(event_from_file.getPath());
				
				if (kind == StandardWatchEventKinds.ENTRY_CREATE | kind == StandardWatchEventKinds.ENTRY_MODIFY) {
					if (event_from_file.exists() == false) {
						log.trace("Can't process non-exists file, like {}", () -> event_from_file.getPath());
						continue;
					} else if (event_from_file.isDirectory()) {
						log.trace("Can't process directories, like {}", () -> event_from_file.getPath());
						continue;
					} else if (event_from_file.canRead() == false) {
						log.trace("Can't read file {}", () -> event_from_file.getPath());
						continue;
					} else if (event_from_file.length() == 0) {
						log.trace("Can't process empty file {}", () -> event_from_file.getPath());
						continue;
					}
					
					if (ext.equalsIgnoreCase("url") | ext.equalsIgnoreCase("desktop")) {
						event_manager.onFoundFile(event_from_file.getAbsoluteFile());
					}
				} else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
					if (event_from_file.exists() == false) {
						event_manager.afterLostFile(event_from_file.getAbsoluteFile());
					}
				}
			}
			
			if (key.reset() == false) {
				break;
			}
		}
		
	}
	
}
