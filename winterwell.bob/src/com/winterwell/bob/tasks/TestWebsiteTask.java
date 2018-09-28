package com.winterwell.bob.tasks;
import java.io.File;
import java.net.URI;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.web.WebUtils;

/**
 * @deprecated Use puppeteer!
 * 
 * Status - fine in principle, but fails in chrome headless for our React pages :(
 * @author daniel
 *
 */
public class TestWebsiteTask extends BuildTask {

	private String baseUrl;
	private List<String> testUrls;

	File outputDir = new File("test/screenshots");
	
	@Override
	protected void doTask() throws Exception {
		outputDir.mkdirs();
		for(String u : testUrls) {
			URI ru = WebUtils.resolveUri(baseUrl, u);
			File file = File.createTempFile(FileUtils.safeFilename("test_"+ru.getHost()), ".pdf");
			WebUtils.renderUrlToPdf(ru.toString(), file, false, null, TUnit.MINUTE.dt);
			File pngOut = new File(outputDir, FileUtils.safeFilename(ru.toString()+".png"));			
			WebUtils.pngFromPdf(file, pngOut);
		}
	}

	public void setBaseUrl(String urlBase) {
		baseUrl = urlBase;
	}

	public void setTestUrls(List<String> asList) {
		testUrls = asList;
	}

}
