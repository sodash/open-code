package com.winterwell.web.app;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.winterwell.utils.Printer;
import com.winterwell.web.ajax.JsonResponse;
import com.winterwell.web.data.XId;
import com.winterwell.utils.Dep;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.IFn;
import com.winterwell.utils.Key;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.FileUtils;

import com.winterwell.utils.io.SqlUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FileTooLargeException;
import com.winterwell.web.HtmlTable;
import com.winterwell.web.IWidget;
import com.winterwell.web.WebInputException;
import com.winterwell.web.fields.AField;
import com.winterwell.web.fields.FileUploadField;
import com.winterwell.web.fields.MissingFieldException;
import com.winterwell.web.fields.SafeString;
import com.winterwell.youagain.client.AuthToken;
import com.winterwell.youagain.client.NoAuthException;
import com.winterwell.youagain.client.YouAgainClient;

/**
 * View, upload, edit and delete assets.
 * 
 * <h3>Upload</h3>
 * Post using multipart form encoding.
 * Parameters:
 * 
 *  - upload: The file!
 *  - convert: Optional. Change the image size?
 * 
 * @author daniel
 *
 */
public final class UploadServlet implements IServlet {
		
	/**
	 * 10mb
	 */
	private static final long MAX_UPLOAD = 10 * 1024L * 1024L;
	public static final String ACTION_UPLOAD = "upload";

	public static final FileUploadField UPLOAD = new FileUploadField("upload");
	
	File webRoot = new File("web"); // = Dep.get(ISiteConfig.class).getWebRootDir();
	
	public void setWebRoot(File webRoot) {
		this.webRoot = webRoot;
	}
	
	IFn<File,String> urlFromFile = file -> {
		String p = FileUtils.getRelativePath(file, webRoot);
		return p;
	};
	
	public void setUrlFromFile(IFn<File, String> urlFromFile) {
		this.urlFromFile = urlFromFile;
	}

	public UploadServlet() {
	}
	
	/**
	 * Uploads, renames, (adjusts images), creates and publishes an asset.
	 * Also files it under an object property if set.
	 * 
	 * @param state
	 * @param cargo 
	 * @return asset for the uploaded file
	 * @throws WebInputException
	 */
	private File doUpload(WebRequest state, Map cargo) throws WebInputException {
		if (cargo==null) cargo = new ArrayMap(); // avoid NPEs
		state.processMultipartIncoming(new ArrayMap<String, AField>(
//			CONVERT.getName(), CONVERT
		));
		// name from original filename - accessed via the pseudo field made by FileUploadField
		String name = state.get(new Key<String>(UPLOAD.getFilenameField()), "");
		// Get the file
		File tempFile = state.get(UPLOAD);
		
		// Do the storage!
		File _asset = doUpload2(state.getUserId(), tempFile, name, cargo);
		
		// respond
		cargo.put("contentSize", _asset.length());
		cargo.put("uploadDate", new Time().toISOString());
		cargo.put("author", state.getUserId());
		cargo.put("fileFormat", WebUtils2.getMimeType(_asset));
		cargo.put("name", _asset.getName());
		cargo.put("absolutePath", _asset.getAbsolutePath());
		String url = urlFromFile.apply(_asset);
		cargo.put("url", url);
		

		state.put(UPLOAD, _asset);
		
		// all OK
		state.addMessage(Printer.format("File {0} uploaded", _asset.getName()));
		return _asset;
	}
	
	
	protected File getUploadDir(XId uxid) {
		String udir = FileUtils.safeFilename(uxid==null? "anon" : uxid.toString(), false);		
		return new File(uploadsDir, udir);
	}

	/**
	 * Store the file in a new DBAsset
	 * @param tempFile
	 * @param name
	 * @param cargo 
	 * @param user Cannot be null
	 * @param group
	 * @param imageConversion
	 * @return
	 */
	public File doUpload2(XId uxid, File tempFile, String name, Map cargo) 
	{
		if (tempFile==null) {
			throw new MissingFieldException(UPLOAD);
		}
		if (tempFile.length() == 0) {
			throw new WebInputException("Upload failed: no data came through.");
		}
		checkFileSize(tempFile);
		// This is sort of atomic - an error will lead to some cleanup
		// being done
		File dest = null;
		try {
			// Do the upload
			Log.report("Accepting upload of "+tempFile.length()+" bytes, temp location "+tempFile.getAbsolutePath(), Level.FINE);
			dest = getDestFile(uxid, tempFile);
			// Shift it
			FileUtils.move(tempFile, dest);
			assert dest.exists() : "Destination file doesn't exist: "+tempFile.getAbsolutePath();
			Log.report(dest.length()+" bytes uploaded to "+dest.getAbsolutePath(), Level.FINE);
			// Resize images??
			if (FileUtils.isImage(dest)) {
				doProcessImage(dest, cargo);
			}		
			// done
			return dest;
		
		// Error handling
		} catch (Throwable e) {
			doUpload3_rollback(tempFile, dest);
			throw Utils.runtime(e);
		}
	}

	protected void doProcessImage(File dest, Map cargo) {
		// TODO Auto-generated method stub
		
	}

	private void checkFileSize(File tempFile) {
		if (tempFile.length() > MAX_UPLOAD) { // TODO detect this before uploading a 10gb movie!
			FileUtils.delete(tempFile);
			throw new FileTooLargeException("The file is too large. There is a limit of "+StrUtils.toNSigFigs(MAX_UPLOAD/1100000.0, 2)+"mb on uploads.");
		}
		// is it an image?
//		if (FileUtils.isImage(tempFile) && tempFile.length() >= Twitter.PHOTO_SIZE_LIMIT) {
//			FileUtils.delete(tempFile);
//			throw new FileTooLargeException("The image file is too large. There is a limit of "+StrUtils.toNSigFigs(Twitter.PHOTO_SIZE_LIMIT/1100000.0, 2)+"mb on image uploads.");			
//		}
//		// video?
//		if (FileUtils.isVideo(tempFile) && tempFile.length() >= Twitter.VIDEO_SIZE_LIMIT) {
//			FileUtils.delete(tempFile);
//			throw new FileTooLargeException("The video file is too large. There is a limit of "+StrUtils.toNSigFigs(Twitter.VIDEO_SIZE_LIMIT/1100000.0, 2)+"mb on image uploads.");			
//		}
	}

	File uploadsDir = new File("web/uploads");
	
	public UploadServlet setUploadDir(File uploadDir) {
		this.uploadsDir = uploadDir;
		return this;
	}

	/**
	 * Does NOT move the tempFile
	 * @param user
	 * @param tempFile
	 * @return suggested dest file
	 */
	public File getDestFile(XId uxid, File tempFile) {
		File destDir = getUploadDir(uxid);
		if ( ! destDir.exists()) {
			boolean ok = destDir.mkdirs();
			if ( ! ok) throw new FailureException("Could not create directory "+destDir);
		}
		File dest = FileUtils.getNewFile(new File(destDir, tempFile.getName()));
		return dest;
	}

	/**
	 * Partial clean up of an aborted upload
	 * @param tempFile
	 * @param dest
	 * @param asset
	 */
	private void doUpload3_rollback(File tempFile, File dest) {
		try {
			FileUtils.delete(tempFile);
			FileUtils.delete(dest);
		} catch (Exception e) {
			// swallow since we're already in a fail path
			Log.report(e);
		}
	}

	@Override
	public void process(WebRequest state) throws IOException {
		// must be logged in
		state.processMultipartIncoming(new ArrayMap());
		YouAgainClient ya = Dep.get(YouAgainClient.class);
		List<AuthToken> tokens = ya.getAuthTokens(state);		
		if (state.getUser() == null) throw new NoAuthException(state);
		if (ServletFileUpload.isMultipartContent(state.getRequest())) {
//			try {
			Map cargo = new ArrayMap();			
			File asset = doUpload(state, cargo);
			state.sendRedirect();
						
			// loosely based on http://schema.org/MediaObject
			WebUtils2.sendJson(new JsonResponse(state, cargo), state);
			
			return;
//			} catch(FileTooLargeException ex) {
			// Let the standard code handle it
		}
		throw new TodoException(state);
	}


}
