package com.winterwell.utils.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.junit.Test;

import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;

import junit.framework.TestCase;


public class FileUtilsTest extends TestCase {

	public void testGetWWDir() throws IOException {
		File wwd = FileUtils.getWinterwellDir();
		System.out.println(wwd);
		assert new File(wwd, "open-code").isDirectory();
	}

	/**
	 * TODO add a chunky file
	 * @throws IOException
	 */
	public void offtestChunk() throws IOException {
		final int CHUNK_SIZE = 1000;
		File f = new File("");
		BufferedInputStream stream = new BufferedInputStream(new FileInputStream(f));
		int offset = 0;
		while(stream.available() > 0) {
			offset += CHUNK_SIZE;
			byte[] b = new byte[CHUNK_SIZE];
			int r = stream.read(b, offset, CHUNK_SIZE);
			System.out.println(r);
		}
	}
	
	public void testToFile() {
		File f = FileUtils.toFile("c:\\foo\\bar.txt");
		assert f.getName().equals("bar.txt");
		assert f.getParentFile().getName().equals("foo");
		File c = f.getParentFile().getParentFile();
		assert c.toString().equals("c:") : c;
	}
	/**
	 * This test fails due to a weird file
	 * 
	 * @throws IOException
	 */
	public void devtestReadWeirdFile() throws IOException {
		// Note: This needs some file-sniffing capability to work out the
		// format
		// Currently we fail this test
		FileInputStream in = new FileInputStream(new File(
				"test/format-test.sql"));
		InputStreamReader r = new InputStreamReader(in, FileUtils.ASCII);
		String ugly = FileUtils.read(r);
		assert ugly.contains("SELECT") : ugly.substring(0, 255)
				+ " (this test currently fails) ";
	}

	public void init() {
		File f = new File("testFiles");
		f.mkdir();
	}

	public void testAppendFile1() {
		init();
		File f = new File("testFiles/newFile.txt");
		FileUtils.append("string", f);
		assert FileUtils.read(f).equals("string");
		f.delete();
	}

	public void testAppendFile2() {
		init();
		File f = new File("testFiles/newFile.txt");
		FileUtils.append("string", f);
		FileUtils.append("STRING", f);
		assert FileUtils.read(f).equals("stringSTRING");
		f.delete();
	}

	@Test
	public void testBadSymLink() {
		File f = new File(
				"/home/daniel/winterwell/code/creole/web/WEB-INF/lib/bonecp.jar");
		boolean ok = f.exists();
		boolean sl = FileUtils.isSymLink(f);
		Printer.out(ok + "\t" + sl);
	}

	public void testGetMD5HashString(){
		init();
		File f = new File("testFiles/newMD5File.txt");
		File f2 = new File("testFiles/newMD5File2.txt");
		File f3 = new File("testFiles/newMD5File3.txt");
		
		f.delete();
		f2.delete();
		f3.delete();

		FileUtils.append("string", f);
		FileUtils.append("STRING", f);
		String md5 = FileUtils.getMD5HashString(f);
		Printer.out("md5 " + md5);
		
		FileUtils.append("string", f2);
		FileUtils.append("STRING", f2);
		
		String md5two = FileUtils.getMD5HashString(f2);
		Printer.out("md5two " + md5two);
		assert md5two.toString().equals(md5.toString());
		
		FileUtils.append("rabbit", f3);
		FileUtils.append("SQUID", f3);

		String md5three = FileUtils.getMD5HashString(f3);
		Printer.out("md5three " + md5three);

		assert !md5.toString().equals(md5three.toString());

		
		f.delete();
		f2.delete();
		
	}
	
	public void testChangeType() {
		{
			File f = new File("whatever/dummy.txt");
			File f2 = FileUtils.changeType(f, "html");
			assert f2.equals(new File("whatever/dummy.html")) : f2;
		}
		{
			File f = new File("whatever/dummy.txt");
			File f2 = FileUtils.changeType(f, ".html");
			assert f2.equals(new File("whatever/dummy.html")) : f2;
		}
		{
			File f = new File("whatever/dummy");
			File f2 = FileUtils.changeType(f, "html");
			assert f2.equals(new File("whatever/dummy.html")) : f2;
		}
		{
			File f = new File("whatever/dummy.");
			File f2 = FileUtils.changeType(f, "html");
			assert f2.equals(new File("whatever/dummy.html")) : f2;
		}
	}

	public void testCopy1() {
		init();
		File origin = new File("testFiles/origin.txt");
		FileUtils.write(origin, "content in the original file");
		File destiny = new File("testFiles/destiny.txt");
		FileUtils.copy(origin, destiny);
		assert FileUtils.read(destiny).equals("content in the original file");
		FileUtils.delete(origin);
		FileUtils.delete(destiny);
	}

	public void testCopy2() {
		init();
		File origin = new File("testFiles/origin.txt");
		FileUtils.write(origin, "content in the original file");
		File folder = new File("testFiles/folder");
		folder.mkdir();
		FileUtils.copy(origin, folder);
		File destiny = new File("testFiles/folder/origin.txt");
		assert FileUtils.read(destiny).equals("content in the original file");
		origin.delete();
		destiny.delete();
		folder.delete();
	}

	public void testCopy3() {
		init();
		File origin = new File("testFiles/origin.txt");
		FileUtils.write(origin, "content in the original file");
		File destiny = new File("testFiles/destiny.txt");
		FileUtils.write(destiny, "old content");
		FileUtils.copy(origin, destiny);
		assert FileUtils.read(destiny).equals("content in the original file");
		origin.delete();
		destiny.delete();
	}

	// copying directories
	public void testCopy4() {
		init();
		File dirOrigin = new File("testFiles/dirOrigin");
		dirOrigin.mkdir();
		File origin = new File("testFiles/dirOrigin/origin.txt");
		FileUtils.write(origin, "content in the original file");
		File dirDestiny = new File("testFiles/dirDestiny");
		dirDestiny.mkdir();
		FileUtils.copy(dirOrigin, dirDestiny);
		File destiny = new File("testFiles/dirDestiny/origin.txt");
		assert FileUtils.read(destiny).equals("content in the original file");
		FileUtils.deleteDir(dirOrigin);
		FileUtils.deleteDir(dirDestiny);
	}

	public void testFilenameEncode() {
		for (String s : new String[] {
				"foo/bar.txt",
				"foo//bar.txt",
				"bar.txt/",
				"\foo///bar/txt//",
				" foo/bar.txt ",
				"10%",
				"a_b__c___",
				"Holy Smokes! \n\t What're we going to do Batman?.txt.old   \n \t \r\n ",
				"../../root-dir;rm -rf *;",
				// test a LONG name
				Utils.getRandomString(260) }) {
			String x = FileUtils.filenameEncode(s);
			assert !x.contains(" ");
			String o = FileUtils.filenameDecode(x);
			System.out.println(x);
			assert o.equals(s) : x + "\t" + o;
		}
	}

	public void testFind() {
		{
			List<File> java = FileUtils.find(new File("src"), ".*\\.java");
			assert java.size() > 10;
			for (File file : java) {
				assert file.getName().endsWith(".java");
			}
		}
		{ // Test relativity of paths
			List<File> java = FileUtils.find(new File("src"), ".*\\.java");
			assert java.size() > 10;
			for (File file : java) {
				// Is this really what we want??
				assert file.getPath().startsWith("src") : file;
			}
		}
		{ // Test hidden files
			List<File> files = FileUtils.find(new File("test"),
					FileUtils.TRUE_FILTER, true);
			boolean ok = false;
			for (File file : files) {
				if (file.getName().equals(".hidden-file")) {
					ok = true;
				}
			}
			assert ok : files;
		}
		{ // Test excluding hidden files
			List<File> files = FileUtils.find(new File("src"),
					FileUtils.TRUE_FILTER, false);
			for (File file : files) {
				assert !file.getName().contains("svn") : file;
			}
		}
	}


	public void testGetBasenameCautious() {
		{
			String b = FileUtils.getBasenameCautious("mybase.txt");
			assertEquals("mybase", b);
		}
		{
			String b = FileUtils.getBasenameCautious("mybase.html");
			assertEquals("mybase", b);
		}
		{
			String b = FileUtils.getBasenameCautious("mybase.fubar");
			assertEquals("mybase.fubar", b);
		}
		{
			String b = FileUtils.getBasenameCautious("<mybase.com>");
			assertEquals("<mybase.com>", b);
		}
		{
			String b = FileUtils.getBasenameCautious("mybase.1");
			assertEquals("mybase.1", b);
		}
		{
			String b = FileUtils.getBasenameCautious("mybase.JPG");
			assertEquals("mybase", b);
		}
		{
			String b = FileUtils.getBasenameCautious("mybase.");
			assertEquals("mybase.", b);
		}
	}

	public void testGetExtension() {
		assertEquals("", FileUtils.getExtension("baz"));
		assertEquals(".tar", FileUtils.getExtension("foo.tar"));
		assertEquals(".gz", FileUtils.getExtension("foo.tar.gz"));
		assertEquals(".gz", FileUtils.getExtension("/foo/.bar/baz.tar.gz"));
		assertEquals(".gz", FileUtils.getExtension("FOO.TAR.GZ"));
		// what should this return??
//		assertEquals(".", FileUtils.getExtension("bar."));
	}

	public void testGetNewFile() throws IOException {
		File file = new File("test/dummy1.txt");
		FileUtils.write(file, "hello");
		File f2 = FileUtils.getNewFile(file);
		FileUtils.write(f2, "hello again");
		File f3 = FileUtils.getNewFile(file);
		assert !f2.equals(file);
		assert !f2.equals(f3);
		assert !f3.equals(file);
		FileUtils.delete(file);
		FileUtils.delete(f2);
		FileUtils.delete(f3);
	}

	public void testGetRelativePath() {
		{
			File f = new File("/a/b/c.txt");
			File base = new File("/a");
			String p = FileUtils.getRelativePath(f, base);
			assert p.equals("b/c.txt") || p.equals("b\\c.txt") : p;
		}
		{
			File f = new File("C:\\a\\b\\c.txt");
			File base = new File("C:\\a");
			String p = FileUtils.getRelativePath(f, base);
			assert p.equals("b\\c.txt") : p;
		}
		{
			File f = new File("/b/b/c.txt");
			File base = new File("/a");
			try {
				String p = FileUtils.getRelativePath(f, base);
				assert false : p;
			} catch (IllegalArgumentException e) {
				assert true;
			}
		}
	}

	public void testGetType() {
		String t = FileUtils.getType(new File("dummy.txt"));
		assert t.equals("txt");
		t = FileUtils.getType(new File("whatever.stuff/dummy.old.txt"));
		assert t.equals("txt");
		t = FileUtils.getType(new File("whatever.stuff/dummy"));
		assert t.equals("");
		t = FileUtils.getType(new File("C:\\whatever.stuff\\dummy"));
		assert t.equals("");
		t = FileUtils.getType(new File("C:\\whatever.stuff\\txt.pdf"));
		assert t.equals("pdf");
	}

	public void testIsSymLink() throws IOException {
		// normal file
		File original = File.createTempFile("test", ".txt");
		FileUtils.write(original, "hello world");
		assert !FileUtils.isSymLink(original);
		FileUtils.delete(original);

		{ // normal directory
			File dir = FileUtils.getWorkingDirectory();
			assert dir.isDirectory();
			assert !FileUtils.isSymLink(dir);
		}

		{ // file with a sym link in the chain
			File dir = new File("test-dir" + Utils.getRandomString(4));
			dir.mkdirs();
			assert !FileUtils.isSymLink(dir);
			File f = new File(dir, "test1.txt");
			FileUtils.write(f, "fubar");
			File ldir = new File("test-linked-dir" + Utils.getRandomString(4));
			FileUtils.makeSymLink(dir, ldir);
			assert FileUtils.isSymLink(ldir);
			File lf = new File(ldir, "test1.txt");
			String s = FileUtils.read(lf);
			assert s.equals("fubar");
			assert !FileUtils.isSymLink(lf);
			FileUtils.deleteDir(ldir);
			FileUtils.deleteDir(dir);
		}
		{ // root
			File rt = new File("/");
			assert !FileUtils.isSymLink(rt);
		}
	}

	public void testMakeSymLink() throws IOException {
		File original = File.createTempFile("test", ".txt");
		File linked = FileUtils.changeType(original, "link");
		FileUtils.write(original, "hello world");
		FileUtils.makeSymLink(original, linked);
		Printer.out(FileUtils.read(linked));
		assert FileUtils.isSymLink(linked);
	}

	public void testPrepend() {
		init();
		File f = new File("testFiles/newFile.txt");
		FileUtils.write(f, "hello\nworld");
		assertEquals("hello\nworld", FileUtils.read(f));
		FileUtils.prepend(f, "Cowabunga");
		String txt = FileUtils.read(f);
		assertEquals(txt, "Cowabungahello\nworld");
		f.delete();
	}

	public void testReadFile() throws IOException {
		{
			String alice = FileUtils.read(new File("test/test.txt"));
			String shortAlice = alice.substring(0, 255);
			assert alice.contains("Alice");
			assert alice
					.contains("So she set to work, and very soon finished off the cake.");
		}
	}

	public void testResolveDotDot() throws IOException {
		File f = new File("../whatever.txt");
		String p = FileUtils.resolveDotDot(f.getAbsolutePath());
		assert !p.contains("..") : p;
	}

	public void testSafeFileName() {
		{
			String x = FileUtils.safeFilename(" foo/bar.txt ");
			assert x.equals("foo/bar.txt") : x;
		}
		{
			String x = FileUtils
					.safeFilename("Holy Smokes! What're we going to do Batman?.txt.old   \n \t \r\n ");
			assert x.equals("Holy_Smokes_Whatre_we_going_to_do_Batman.txt.old") : x;
			x = FileUtils.safeFilename("../../root-dir;rm -rf *;");
			assert x.equals("_./_./root-dirrm_-rf") : x;
			x = FileUtils.safeFilename("../../root-dir; \n\n \t rm -rf *;");
			assert x.equals("_./_./root-dir_rm_-rf") : x;
		}
	}

	public void testSetType() {
		File f = FileUtils.changeType(new File("dummy.txt"), "gif");
		assert f.getPath().equals("dummy.gif");
		f = FileUtils.changeType(new File("whatever.stuff/dummy.old.txt"),
				"gif");
		String fp = f.getPath();
		fp = fp.replace('\\', '/'); // make windows same as unix
		assert fp.equals("whatever.stuff/dummy.old.gif") : fp;
	}
}
