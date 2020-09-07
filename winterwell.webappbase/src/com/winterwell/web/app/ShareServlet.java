package com.winterwell.web.app;

import java.io.File;
import java.io.IOException;

import com.winterwell.utils.Dep;
import com.winterwell.utils.Proc;
import com.winterwell.utils.Utils;
import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.WebPage;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.fields.Checkbox;

/**
 * Provide the header info for Facebook share (and a redirect to the actual page).
 * 
 * See https://moz.com/blog/meta-data-templates-123
 * 
 * https://dev.twitter.com/docs/cards/validation/validator
 * https://developers.facebook.com/tools/debug/
 * https://developers.pinterest.com/tools/url-debugger/
 * https://developers.google.com/+/web/snippet/
 * 
 * @author daniel
 *
 */
public class ShareServlet implements IServlet {
	
	String homepage;
	
	@Override
	public void process(WebRequest state) throws Exception {
		WebPage page = new WebPage();
		state.setPage(page);
		// the real page
		String link = state.getRedirect();		
		if (link==null) link = homepage;
		// build page info
		String title = state.get("title"); // Maximum length 60-70 characters
		
		String image = state.get("image");		
		if ("screenshot".equals(image)) {
			image = doScreenshot(link);
		}
		
		String desc = state.get("desc"); // 155 chars??
		String tweep = state.get("tweep");
		
		page.setTitle(title);		
//		<meta name="description" content="Page description. No longer than 155 characters." />
		page.appendToHeader("<meta property=\"og:title\" content=\""+title+"\" />\n" + 
				"<meta property=\"og:type\" content=\"website\" />\n" + 
				"<meta property=\"og:url\" content=\""+WebUtils2.attributeEncode(link)+"\" />\n" + 
				"<meta property=\"og:image\" content='"+WebUtils2.attributeEncode(image)+"' />"
				+"<meta property=\"og:site_name\" content=\"SoGive\" />"
				+"<meta property='og:description' content='"+WebUtils2.attributeEncode(desc)+"' />"
				);
//		<meta property="article:tag" content="Article Tag" />
//		   <meta property="article:published_time" content="2014-08-12T00:01:56+00:00" />
//		    <meta property="article:author" content="CNN Karla Cripps" />

		page.appendToHeader("<meta name='twitter:card' value='"+WebUtils2.attributeEncode(desc)+"'>");
		page.appendToHeader("<meta name='twitter:site' content='"+WebUtils2.attributeEncode(tweep)+"'>");
		page.appendToHeader("<meta name='twitter:title' content='"+WebUtils2.attributeEncode(title)+"'>");
		page.appendToHeader("<meta name='twitter:description' content='"+WebUtils2.attributeEncode(desc)+"'>");
		page.appendToHeader("<meta name='twitter:image' content='"+WebUtils2.attributeEncode(image)+"'>");
		page.appendToHeader("<meta name='twitter:creator' content='"+WebUtils2.attributeEncode(tweep)+"'>");
//		<meta name="twitter:title" content="Page Title">
//		<meta name="twitter:description" content="Page description less than 200 characters">
//		<meta name="twitter:creator" content="@author_handle">
//		<-- T)witter Summary card images must be at least 120x120px -->
//		<meta name="twitter:image" content="http://www.example.com/image.jpg">
//		// G+
//		<!-- Schema.org markup for Google+ -->
//		<meta itemprop="name" content="The Name or Title Here">
//		<meta itemprop="description" content="This is the page description">
//		<meta itemprop="image" content="http://www.example.com/image.jpg">
		
		// redirect
		if ( ! state.debug) {
			page.append("<script>window.location='"+link+"';</script>");			
		}
//		page.addStylesheet("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css");
		page.append("<div class='container'><a href='"+link+"'><p>Redirecting to your page...</p><div class='card card-body'>"+"<h2>"+title+"</h2><p>"+desc+"</p><img src='"+image+"'></div></a></div>");
		
		state.sendPage();
		state.close();
	}

	/**
	 * 
	 * @param link
	 * @return image link
	 * @throws IOException 
	 */
	String doScreenshot(String link) throws IOException {
		File html = new FakeBrowser().getFile(link);
		File png = FileUtils.changeType(html, "png");

		WebUtils.renderUrlToPng(link, png);

		Log.d("screenshot", "Made "+png+" from "+link);
		// upload & link
		Uploader uploader = Dep.get(Uploader.class);
		String pngLink = uploader.upload(png);
		return pngLink;
	}

}
