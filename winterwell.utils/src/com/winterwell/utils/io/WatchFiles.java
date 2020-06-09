package com.winterwell.utils.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.winterwell.utils.log.Log;

/**
 * @testedby WatchFilesTest
 * @author daniel
 *
 */
public class WatchFiles implements Runnable {
	
	public static interface IListenToFileEvents {

		void processEvent(FileEvent fileEvent);
		
	}

	
	WatchService watcher;
	private boolean pleaseStop;
	private int totalEventCount;

	public WatchFiles() throws IOException {
		watcher  = FileSystems.getDefault().newWatchService();
	}
	
	/**
	 * Request a stop (may take a few seconds to actually stop, and can be jammed by a listener)
	 */
	public void stop() {
		this.pleaseStop = true;
	}
	
	public void run() {
		while( ! pleaseStop) {
			try {
			    WatchKey watchKey = watcher.poll(1, TimeUnit.SECONDS);
			    if (watchKey == null) continue;
		        List<WatchEvent<?>> events = watchKey.pollEvents();
		        Path watched = (Path) watchKey.watchable();
		        List<FileEvent> fileEvents = new ArrayList();
		        for (WatchEvent event : events) {
		        	Path path = (Path) event.context();
		        	// OMG - toAbsolutePath does NOT give the right path!
		        	// see https://stackoverflow.com/questions/32691350/watchservice-incorrectly-resolved-absolute-path
		        	File file = path.toFile(); //toAbsolutePath().toFile();	            
					fileEvents.add(new FileEvent(file, event.kind()));
		            totalEventCount++;
		        }
		        watchKey.reset();
		        // tell people
		        for(IListenToFileEvents l : listeners) {
		        	for (FileEvent pair2 : fileEvents) {
		        		try {
		        			l.processEvent(pair2);
		        		} catch(Throwable ex) {
		        			Log.e("watcher", ex);
		        		}
					}
		        }
			} catch(Exception ex) {
				Log.e("watcher", ex);
			}
		}
	}
	
	public WatchFiles addDir(File projectDir) throws IOException {
		assert projectDir.exists();
		if (projectDir.isFile()) {
			throw new IllegalArgumentException("Not a directory "+projectDir);
		}
		Files.walkFileTree(projectDir.toPath(), new WatchServiceRegisteringVisitor());
		return this;
	}

	private class WatchServiceRegisteringVisitor extends SimpleFileVisitor<Path> {
	    @Override
	    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
	    	Log.d("watch", dir);
	         dir.register(watcher, 
	        		 StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
	         
	         return FileVisitResult.CONTINUE;
	    }
	}	

	public synchronized void addListener(IListenToFileEvents iListenToFileEvents) {
		listeners.add(iListenToFileEvents);
	}
	
	public synchronized void removeListener(IListenToFileEvents iListenToFileEvents) {
		listeners.add(iListenToFileEvents);
	}
	
	List<IListenToFileEvents> listeners = new ArrayList();
	
}

// Linux complains!
//final class DirWatcher implements WatchService {
//
//	final WatchService base;
//	final File dir;
//	
//	public DirWatcher(WatchService base, File dir) {
//		this.base = base;
//		this.dir = dir;
//	}
//
//	@Override
//	public void close() throws IOException {
//		base.close();
//	}
//
//	@Override
//	public WatchKey poll() {
//		WatchKey wk = base.poll();
//		return addContext(wk);
//	}
//
//	private WatchKey addContext(WatchKey wk) {
//		// TODO Auto-generated method stub
//		return wk;
//	}
//
//	@Override
//	public WatchKey poll(long arg0, TimeUnit arg1) throws InterruptedException {
//		WatchKey wk = base.poll(arg0, arg1);
//		return addContext(wk);
//	}
//
//	@Override
//	public WatchKey take() throws InterruptedException {
//		WatchKey wk = base.take();
//		return addContext(wk);
//	}	
//}
//
