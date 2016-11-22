package com.winterwell.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.web.WebUtils;

import com.winterwell.utils.Printer;

/**
 * TODO spider a website looking for broken links
 * 
 * @author Daniel
 * 
 */
public class WebSpider {

	static Pattern aLink = Pattern.compile("href=['\"]?([^ '\">]+)['\"]?");
	String baseSite;
	private ArrayList<URI> brokenLinks;
	private int maxDepth = 12;

	private boolean stayInsideSite = true;

	private HashSet<URI> tried;

	public ArrayList<URI> getBrokenLinks() {
		return brokenLinks;
	}

	/**
	 * Find all the web links in a page
	 * 
	 * @param base
	 *            This will be used to resolve partial URLs
	 * @param page
	 * @return
	 */
	public List<URI> getLinksFromPage(URI base, String page) {
		List<URI> list = new ArrayList<URI>();
		Matcher m = aLink.matcher(page);
		while (m.find()) {
			String link = m.group(1);
			try {
				URI uri = WebUtils.resolveUri(base.toString(), link);
				list.add(uri);
			} catch (Exception e) {
				// TODO: handle exception
				Printer.out(link);
			}
		}
		return list;
	}

	/**
	 * 
	 * @param uri
	 * @return Does not include
	 */
	public List<URI> harvestLinks(URI uri) {
		baseSite = uri.getHost();
		List<URI> links = new ArrayList<URI>();
		tried = new HashSet<URI>();
		brokenLinks = new ArrayList<URI>();
		harvestLinks2(uri, links, brokenLinks, 0);
		return links;
	}

	private void harvestLinks2(URI uri, List<URI> links, List<URI> brokenLinks,
			int depth) {
		if (tried.contains(uri))
			return;
		tried.add(uri);
		FakeBrowser b = new FakeBrowser();
		String page;
		try {
			page = b.getPage(uri.toString(), null);
		} catch (Exception e) {
			System.out.println(uri);
			brokenLinks.add(uri);
			return;
		}
		links.add(uri);
		// In too deep?
		if (depth == maxDepth)
			return;
		// Get links
		// TODO switch to parsed page
		List<URI> pagelinks = getLinksFromPage(uri, page);
		// TODO parallelise
		for (URI plink : pagelinks) {
			String phost = plink.getHost();
			if (stayInsideSite && !baseSite.equalsIgnoreCase(phost)) {
				continue;
			}
			// Remove #pageanchor if present
			String fragment = plink.getRawFragment();
			if (fragment != null) {
				String ps = plink.toString();
				int i = ps.indexOf("#");
				plink = WebUtils.URI(ps.substring(0, i));
			}
			// recurse
			harvestLinks2(plink, links, brokenLinks, depth + 1);
		}
	}

}
