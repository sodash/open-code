package winterwell.utils.io;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import winterwell.utils.IReplace;
import winterwell.utils.StrUtils;

/**
 * Partially convert RTF into plain-text/markdown. See
 * http://latex2rtf.sourceforge.net/RTF-Spec-1.0.txt
 * 
 * @author daniel
 * @testedby {@link RTFHelperTest}
 */
public class RTFHelper {

	static final Pattern CONTROL_CODE = Pattern
			.compile("(^|\\b|[^\\\\])(\\\\[a-zA-Z0-9\\-]+\\s?|\\{[^\\} \r\n]+\\})");

	public String decodeRTF(String text) {
		// Ignored codes include:
		// \pard = default paragraph settings
		// \s<N> = user defined style, referenced by number

		text = text.replaceAll("\\\\tab\\b", "\t");
		text = text.replaceAll("\\\\lquote\\s?", "'");
		text = text.replaceAll("\\\\rquote\\s?", "'");
		text = text.replaceAll("\\\\ldblquote\\s?", "\"");
		text = text.replaceAll("\\\\rdblquote\\s?", "\"");
		// hex-encoded non-ascii chars
		text = StrUtils.replace(text, Pattern.compile("\\\\'([0-9a-f]{2})"),
				new IReplace() {
					@Override
					public void appendReplacementTo(StringBuilder sb,
							Matcher match) {
						// String s = match.group();
						char c = (char) Integer.parseInt(match.group(1), 16);
						sb.append(c);
					}
				});
		// paragraph markers -- can we ignore these and count on line breaks??
		text = text.replaceAll("\\\\par\\b[^\r\n]", "\n\n");
		text = text.replaceAll("\\\\par\\b", "");
		text = text.replaceAll("\\\\line\\b", "\n");

		// italics and bold into markdown
		// NB: \i0 is "italics off"
		text = text.replaceAll("\\\\b0?", "**");
		text = text.replaceAll("\\\\i0?", "*");

		// strip the stuff we don't convert!
		text = StrUtils.replace(text, CONTROL_CODE, new IReplace() {
			@Override
			public void appendReplacementTo(StringBuilder sb, Matcher match) {
				String s = match.group();
				String s1 = match.group(1);
				// String s2 = match.group(2);
				// String code = StrUtils.ellipsize(match.group(2), 140);
				// Log.report("rtf", "Ignoring code "+code, Level.FINER);
				sb.append(s1);
			}
		});
		// nested {{}}s can fool the clean-up above -- get rid of any stray
		// unescaped }s
		text = text.replaceAll("(^|\\b|[^\\\\])\\}", "$1");

		text = text.replace("\\~", " ");
		text = text.replace("\\{", "{");
		text = text.replace("\\}", "}");
		return text;
	}

}
