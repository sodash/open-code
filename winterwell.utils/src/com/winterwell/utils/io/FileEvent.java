package com.winterwell.utils.io;

import java.io.File;
import java.nio.file.WatchEvent.Kind;

public class FileEvent {
	public final Kind kind;
	public final File file;

	public File getFile() {
		return file;
	}
	
	public FileEvent(File file, Kind kind) {
		this.file = file;
		this.kind = kind;
	}

	@Override
	public String toString() {
		return "FileEvent[kind=" + kind + ", file=" + file + "]";
	}
	
}
