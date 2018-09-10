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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.execprocess.ExecutableFinder;

public class App {
	
	private static final Logger log = LogManager.getLogger();
	
	public static void main(String[] args) throws Exception {
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
		
		File scan_dir = new File(System.getProperty("scan_dir", System.getProperty("user.home") + File.separator + "Desktop"));
		if (scan_dir.exists() == false) {
			throw new FileNotFoundException("Can't found " + scan_dir + " directory");
		} else if (scan_dir.canRead() == false) {
			throw new IOException("Can't read " + scan_dir + " directory");
		} else if (scan_dir.isDirectory() == false) {
			throw new IOException(scan_dir + " is not a directory");
		}
		
		WatchService watcher = FileSystems.getDefault().newWatchService();
		scan_dir.toPath().register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.OVERFLOW);
		
		EventManager event_manager = new EventManager(ex_finder, new File(System.getProperty("out_dir", System.getProperty("user.home") + File.separator + "Downloads")));
		
		FileUtils.iterateFiles(scan_dir, new String[] { "url", "URL" }, false).forEachRemaining(f -> {
			String ext = FilenameUtils.getExtension(f.getPath());
			if (ext.equalsIgnoreCase("url")) {
				event_manager.onFoundURLFile(f.getAbsoluteFile());
			}
		});
		
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
				
				if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
					if (ext.equalsIgnoreCase("url")) {
						event_manager.onFoundURLFile(event_from_file.getAbsoluteFile());
					}
					/*} else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {*/
				} else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
					if (event_from_file.exists() == false) {
						continue;
					}
					
					if (ext.equalsIgnoreCase("url")) {
						event_manager.onFoundURLFile(event_from_file.getAbsoluteFile());
					}
				}
			}
			
			if (key.reset() == false) {
				break;
			}
		}
		
	}
	
}
