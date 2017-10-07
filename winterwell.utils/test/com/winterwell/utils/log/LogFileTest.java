package com.winterwell.utils.log;

import java.io.File;

import javax.management.RuntimeErrorException;

import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.time.TUnit;

import junit.framework.TestCase;

public class LogFileTest extends TestCase {

	public void testLogFile() {
		File f = new File("test-output/test1.txt");
		FileUtils.delete(f);
		LogFile lf = new LogFile(f);
		Log.report("Hello 1");
		Log.report("Hello 2");
		String log = FileUtils.read(f);
		Printer.out(log);
		Log.report("Hello 3");
		log = FileUtils.read(f);
		Printer.out(log);
		lf.close();
	}

	public void testLogError() {
		File f = new File("test-output/test-error.txt");
		FileUtils.delete(f);
		LogFile lf = new LogFile(f);
		Exception es = new Exception("base");
		RuntimeException ex2 = new RuntimeException(es);
		Log.e("test", ex2);		
		String log = FileUtils.read(f);
		Printer.out(log);
		lf.close();
	}


	public void testLogAutoTag() {
		File f = new File("test-output/test1.txt");
		FileUtils.delete(f);
		LogFile lf = new LogFile(f);
		Log.d("Hello 1");
		Log.d("Hello 2");
		String log = FileUtils.read(f);
		Printer.out(log);
		lf.close();
	}

	public void testRotation() {
		{
			File f = new File("test-output/rotate-test.txt");
			FileUtils.delete(f);
			LogFile lf = new LogFile(f);
			lf.setLogRotation(TUnit.SECOND.getDt(), 10);
			for (int i = 0; i < 5; i++) {
				Log.report("Hello " + i);
				Utils.sleep(750);
			}
			// check what happened
			String log = FileUtils.read(f);
			Printer.out(log);
			assert log.contains("Hello 4") : log;
			assert !log.contains("Hello 1") : log;
			Printer.out(f.getAbsoluteFile().getParentFile().list());
			lf.close();
		}
		{
			File f = new File("test-output/rotate-test.txt");
			FileUtils.delete(f);
			LogFile lf = new LogFile(f);
			lf.setLogRotation(TUnit.SECOND.getDt(), 10);
			for (int i = 0; i < 20; i++) {
				Log.report("Hello " + i);
				Utils.sleep(750);
			}
			// check what happened
			String log = FileUtils.read(f);
			Printer.out(log);
			assert log.contains("Hello 19") : log;
			assert !log.contains("Hello 11") : log;
			Printer.out(f.getAbsoluteFile().getParentFile().list());
			lf.close();
		}
	}
}
