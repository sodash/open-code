package winterwell.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.winterwell.utils.io.FileUtils;

/**
 * @deprecated Use the com.winterwell version
 * Gobble output from a stream. Create then call start().
 * 
 * @author Based on code by Michael C. Daconta, published in
 *         http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps_p.html
 */
public final class StreamGobbler extends com.winterwell.utils.StreamGobbler {

	public StreamGobbler(InputStream is) {
		super(is);
	}
}
