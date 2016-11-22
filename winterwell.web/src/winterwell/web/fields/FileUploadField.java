package winterwell.web.fields;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import winterwell.utils.Mutable;
import winterwell.utils.Mutable.Strng;
import winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;

public class FileUploadField extends AField<File> {
	private static final long serialVersionUID = 1L;

	/**
	 * Warning: multipart forms do not work properly with AField because they
	 * don't show up as request parameters.
	 * 
	 * @param request
	 * @return map of field-name to String or File
	 */
	public static Map<String, Object> processFormFields(
			HttpServletRequest request) {
		// Check that we have a file upload request
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		if ( ! isMultipart)
			return Collections.emptyMap();
		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload();
		// Parse the request
		try {
			FileItemIterator iter = upload.getItemIterator(request);
			Map<String, Object> map = new HashMap<String, Object>();
			while (iter.hasNext()) {
				FileItemStream item = iter.next();
				processItem(request, item, map);
			}
			return map;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * 
	 * @param request
	 * @param item
	 * @param map
	 *            This will be adjusted. For file uploads, you get a File and a
	 *            second field "fieldname-filename" which holds the original
	 *            filename.
	 * @throws IOException
	 */
	private static void processItem(HttpServletRequest request,
			FileItemStream item, Map<String, Object> map) throws IOException {
		String name = item.getFieldName();
		// Form field? Easy
		if (item.isFormField()) {
			String value = FileUtils.read(item.openStream());
			map.put(name, value);
			return;
		}
		// File upload...
		InputStream stream = item.openStream();
		Strng filename = new Mutable.Strng(item.getName());
		File file = processItem2(stream, filename);
		map.put(name, file);
		// must match with #getFilenameField
		map.put(name + "-filename", filename.value);
	}

	/**
	 * Save the stream contents to a *temporary* file.
	 * 
	 * @param stream
	 * @param filename
	 *            This can change due to security concerns
	 * @return
	 * @throws IOException
	 */
	public static File processItem2(InputStream stream, Mutable.Strng filename)
			throws IOException {
		// Security protection - against commands hidden in a filename
		filename.value = FileUtils.safeFilename(filename.value);
		File file = new File(filename.value);
		String extension = FileUtils.getExtension(file);
		String basename = new File(filename.value).getName();
		basename = basename.substring(0,
				basename.length() - (extension.length()));
		// create temp file will complain if basename is too short
		if (basename.length() < 2) {
			basename = "up" + basename;
		}
		file = File.createTempFile(basename + "-", extension);
		FileUtils.copy(stream, file);
		return file;
	}

	public FileUploadField(String name) {
		super(name, "file");
	}

	/**
	 * A "pseudo field" created for File uploads. This field holds the filename
	 * of the original file. Use this as a key to the map of fields as returned
	 * by {@link #processFormFields(HttpServletRequest)} to get the filename.
	 */
	public String getFilenameField() {
		return getName() + "-filename";
	}

}
