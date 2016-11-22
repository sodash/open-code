/**
 * 
 */
package winterwell.web;

import java.io.IOException;

import org.apache.http.HttpResponse;

import com.winterwell.utils.io.FileUtils;

/**
 * Helper methods for the unwieldy, error-prone bloat that Apache's HttpClient
 * library introduces.
 * 
 * Can you tell I'm not a fan?
 * 
 * @author daniel
 * 
 */
public class WebUtilsJakarta {

	/**
	 * Usage: <code><pre>
	 * HttpResponse response = null;
	 * try {
	 * 	response = client.execute(method);
	 * 	// do stuff
	 * } finally {
	 *    WebUtilsJakarta.close(response);
	 * }
	 * </pre></code> "In order to ensure proper release of system resources one
	 * must close the content stream associated with the entity."
	 * 
	 * @param response
	 *            Never throws an exception.
	 */
	public static void close(HttpResponse response) {
		if (response == null)
			return;
		try {
			response.getEntity().getContent().close();
		} catch (Exception e) {
			// oh well
		}
	}

	/**
	 * 
	 * @param response
	 * @return the web-page (or any other String content) returned. Closes the
	 *         inputstream after reading.
	 * @throws IOException
	 */
	public static String read(HttpResponse response) throws IOException {
		return FileUtils.read(response.getEntity().getContent());
	}

}
