package com.winterwell.utils.io;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.winterwell.utils.Mutable;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;

public class WatchFilesTest {

	@Test
	public void testExistingFileInDir() throws IOException {
		Mutable.Int editCnt = new Mutable.Int();
		File d = File.createTempFile("testwatche", "dir");
		FileUtils.delete(d);
		d.mkdirs();
		
		File f = new File(d, "test.txt");
		FileUtils.write(f, "hello");
		
		WatchFiles wf = new WatchFiles();
		wf.addDir(d);
		wf.addListener(e -> editCnt.value++);
		Thread thread = new Thread(wf);
		thread.setName("testFile");
		thread.start();
		
		Utils.sleep(50);
		assert editCnt.value == 0;
		
		FileUtils.write(f, "hello world");		
		Utils.sleep(50);
		assert editCnt.value == 1 : editCnt;
		
		FileUtils.write(f, "goodbye");		
		Utils.sleep(50);
		assert editCnt.value == 2;
		
		wf.stop();
		thread.stop();
		FileUtils.delete(f);
	}
		
	

	@Test
	public void testNewFileInDir() throws IOException {
		Mutable.Int editCnt = new Mutable.Int();
		File d = File.createTempFile("testwatchn", "dir");
		FileUtils.delete(d);
		d.mkdirs();
		
		File f = new File(d, "test.txt");
		
		WatchFiles wf = new WatchFiles();
		wf.addDir(d);
		wf.addListener(e -> {
			editCnt.value++;
			Printer.out(e);
		});
		Thread thread = new Thread(wf);
		thread.setName("testFile");
		thread.start();
		
		Utils.sleep(50);
		assert editCnt.value == 0;
		
		FileUtils.write(f, "hello world");		
		Utils.sleep(50);
		assert editCnt.value > 0 : editCnt; // NB - can get create+modify
		int old = editCnt.value;
		
		FileUtils.write(f, "goodbye");		
		Utils.sleep(50);
		assert editCnt.value > old : editCnt;
		
		wf.stop();
		thread.stop();
		FileUtils.deleteDir(d);
	}


	@Test
	public void testExistingFileInSubDir() throws IOException {
		Mutable.Int editCnt = new Mutable.Int();
		File d = File.createTempFile("testwatch", "dir");
		FileUtils.delete(d);
		d.mkdirs();
		
		File sd = new File(d, "subdir");
		sd.mkdirs();
		
		File f = new File(sd, "subtest.txt");
		FileUtils.write(f, "hello");
		
		WatchFiles wf = new WatchFiles();
		wf.addDir(d);
		wf.addListener(e -> editCnt.value++);
		Thread thread = new Thread(wf);
		thread.setName("testFile");
		thread.start();
		
		Utils.sleep(50);
		assert editCnt.value == 0;
		
		FileUtils.write(f, "hello world");		
		Utils.sleep(50);
		assert editCnt.value > 0 : editCnt;
		int old = editCnt.value;
		
		FileUtils.write(f, "goodbye");		
		Utils.sleep(50);
		assert editCnt.value > old;
		
		wf.stop();
		thread.stop();
		FileUtils.deleteDir(d);
	}
}
