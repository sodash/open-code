package jobs;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.web.WebUtils;

public class BuildDansWebsite extends BuildTask {

	Map placeLinks = new ArrayMap("writing", "/writing", "misc",
			"/writing/misc.htm", "dad", "/writing/dad", "scripts", "",
			"reviews", "/reviews", "research", "/abstracts.htm", "recipes",
			"/recipes", "silly", "/images/silly.htm", "images", "/gallery.html"

	);

	private String template;

	private void doPhotos() {
		// TODO Auto-generated method stub
	}

	@Override
	public void doTask() throws Exception {
		File base = new File("/home/daniel/public_html");
		template = FileUtils.read(new File(base, "template.html"));
		// Collect Links?
		// Build!
		List<File> srcFiles = FileUtils.find(base, ".*\\.src.htm");
		for (File src : srcFiles) {
			String name = src.getName();
			name = name.substring(0, name.length() - ".src.htm".length());
			if (name.equals("index"))
				name += ".html";
			else
				name += ".htm";
			File out = new File(src.getParentFile(), name);
			// apply template
			try {
				write(src, out);
			} catch (Exception e) {
				System.err.println("ERROR: " + src + ": " + e.getMessage());
			}
		}
		// Done with pages
		doPhotos();
	}

	private void write(File src, File out) {
		// TODO Auto-generated method stub
		String in = FileUtils.read(src);
		List<String> bodies = WebUtils.extractXmlTags("body", in, false);
		String[] title = StrUtils.find(Pattern.compile(
				"<title>(.*)</title>", Pattern.DOTALL), in);
		if (bodies.size() == 0) {
			System.err.println(src + " had no body!");
			bodies.add(in);
		}
		String body = bodies.get(0);
		String[] links = StrUtils.find(Pattern.compile(
				"<links>(.*)</links>", Pattern.DOTALL), in);
		String[] placeBits = StrUtils.find(Pattern.compile(
				"<place>(.*)</place>", Pattern.DOTALL), in);
		String[] copyrightBits = StrUtils.find(Pattern.compile(
				"<copyright>(.*)</copyright>", Pattern.DOTALL), in);
		String[] keywordBits = StrUtils.find(Pattern.compile(
				"<keywords>(.*)</keywords>", Pattern.DOTALL), in);
		String page = template;
		if (title == null) {
			title = new String[] { "", "untitled" };
			System.err.println(src + " had no title!");
		}
		page = page.replace("$TITLE", title[1]);
		page = page.replace("$BODY", body);
		String link = links == null ? "" : links[1];
		page = page.replace("$LINKS", link);
		String places = "<a href='/'>home</a> / ";
		if (placeBits != null) {
			String[] ps = placeBits[1].split(",");
			for (String p : ps) {
				p = p.toLowerCase().trim();
				String plink = (String) placeLinks.get(p);
				if (plink == null) {
					System.err.println(p);
					places += p + " /";
					continue;
				}
				places += "<a href='" + plink + "'>" + p + "</a> /";
			}
		}
		page = page.replace("$PLACE", places);
		String keywords = keywordBits == null ? "mathematics, science, writing, AI, artificial intelligence, games, consultancy"
				: keywordBits[1];
		page = page.replace("$KEYWORDS", keywords);
		String copyright = copyrightBits == null ? "&copy; Daniel Winterstein 1998-2008"
				: copyrightBits[1];
		page = page.replace("$COPYRIGHT", copyright);
		// Write
		FileUtils.write(out, page);
	}

}
