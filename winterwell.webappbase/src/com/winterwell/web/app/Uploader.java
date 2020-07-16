package com.winterwell.web.app;

import java.io.File;

import com.winterwell.utils.Dep;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.io.FileUtils;

/**
 * Upload files to be web-visible
 * @author daniel
 *
 */
public class Uploader {

	File uploadsDir;
	String uploadsUrl;
	
	public Uploader(File uploadsDir, String uploadsUrl) {
		this.uploadsDir = uploadsDir;
		this.uploadsUrl = uploadsUrl;
	}

	public String upload(File file) {
		// put it in an uploads dir?
		if (uploadsDir!=null) {
			File out = new File(uploadsDir, file.getName());
			FileUtils.copy(file, out);
			ISiteConfig sc = Dep.get(ISiteConfig.class);			
			String url = uploadsUrl+"/"+out.getName();
			return url;
		}
		throw new TodoException("Upload to media.gl.com??");
	}

}
