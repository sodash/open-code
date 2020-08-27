package com.winterwell.utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;


/**
 * Copied from ScriptProperties.java
 * 
 * TODO maybe replace with jsx style {} templates or js style ${}??
 * 
 * TODO maybe use Rhino to process full js??
 * 
 * Provide String based access to properties.
 * There is also a {@link #process(String)} method which does Perl style variable
 * interpolation.
 * @author daniel
 * @testedby {@link SimpleTemplateVarsTest}
 */
public final class SimpleTemplateVars {

	private Map<String, ?> vars;
	private boolean useJS;
	
	public SimpleTemplateVars setUseJS(boolean useJS) {
		this.useJS = useJS;
		return this;
	}

	/**
	 * Convenience (mostly for quick access to {@link #process(String)}).
	 * @param properties
	 */
	public SimpleTemplateVars(Map properties) {
		this.vars = properties;
	}
	
	
	/**
	 * Converts Perl style variables, e.g. $name, using these properties
	 * (converted into Strings with {@link Printer#toString(Object)}).
	 * 
	 * TODO It will even convert object properties (one deep - not recursive). E.g.
	 * $person.name This only handles properties that have a no-arg getXXX() method.  
	 * ??
	 * @param txtWithVars Can be blank (returned as-is)
	 * @return 
	 */
	public String process(String txtWithVars) {
		if (Utils.isBlank(txtWithVars)) {
			return txtWithVars; // blank = no-op
		}
		String txt1 = txtWithVars;

		// TODO process ${name} 
		String txt2 = process2_dollarBracket(txt1);

		// process $vars
		String txt3 = process2_vars(txt2);
		
		// process js
		if (useJS) {
			txt2 = process2_js(txtWithVars, txt2);
		}
		
		// done
		return txt3;
	}

	/**
	 * TODO replace ${var}
	 * @param txt2
	 * @return
	 */
	private String process2_dollarBracket(String txt2) {
		return txt2; // TODO
	}

	static final Pattern p = Pattern.compile("\\$([a-zA-Z0-9_]+)(\\.[a-zA-Z0-9_]+)?");
	
	private String process2_vars(String txt) {		
		List unsets = new ArrayList();
		String txtOut = StrUtils.replace(txt, p, (StringBuilder sb, Matcher m) -> {
//				// Is it in a url?
			boolean inUrl = false; // minor TODO
//				for(Slice url : urls) {
//					if (url.start <= m.start() && url.end >= m.end()) {
//						inUrl=true; break;
//					}
//				}			
			// key and value
			String k = m.group(1);
			Object v = vars.get(k);			
			if (v==null) {
				// If you put a $var in a url, and the var is unset, then enter a blank value.
				if (inUrl) {
					v = "";
				} else {
					unsets.add(k);
					sb.append(m.group());
					return;
				}
			}			
			String extra = null;
			// properties?
			if (m.group(2) != null) {
				String prop = m.group(2).substring(1);
				// try for a match in vars
				Object v2 = vars.get(k+"."+prop);
				if (v2==null) {
					// then try a reflective call
					v2 = ReflectionUtils.getProperty(v, prop);
				}
				if (v2 != null) {
					v = v2;
				} else {
					extra = m.group(2);
				}				
			}
			// convert v to string
			String vs = Printer.toString(v);
			if (numberFormat!=null && MathUtils.isNumber(v)) {
				vs = numberFormat.format(MathUtils.toNum(v));
			}
			if (inUrl) {
				// escape
				vs = WebUtils.urlEncode(vs);
			}
			// append
			sb.append(vs);
			if (extra!=null) sb.append(extra);	
		});
		// log unset
		if ( ! unsets.isEmpty()) {
			Log.i("template", "Ignoring unset variables: $"+StrUtils.join(unsets,", $"));
		}
		return txtOut;
	}
	
	public void setNumberFormat(DecimalFormat numberFormat) {
		this.numberFormat = numberFormat;
	}
	
	DecimalFormat numberFormat;

	private String process2_js(String txtWithVars, String txt2) {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine js = manager.getEngineByName("javascript");
		Bindings bindings = js.getBindings(ScriptContext.ENGINE_SCOPE);
		if (vars != null) {
			// TODO blanks as ""
		    bindings.putAll(vars);		    
		}
		Mutable.Ref result = new Mutable.Ref();
		bindings.put("result", result);
		// set global and window (both!) as references to the bindings
		// so people can use possibly-undefined variables
		HashMap global = new HashMap(bindings);
		bindings.put("global", global);
		bindings.put("window", global);

		try {
			// Nashorn doesn't support ES6 in Java 8 :( So we can't just use `s
			txtWithVars = txtWithVars.replaceAll("(\r\n|\r|\n)", "\\\\n");
			txtWithVars = txtWithVars.replaceAll("\"", "'"); // FIXME
			IReplace replace = (StringBuilder sb, Matcher match) -> {
				String snippet = match.group(1);
				sb.append("\"+(" +snippet+ ")+\"");
			};
			String txtFn = StrUtils.replace(txtWithVars, Pattern.compile("\\$\\{(.+?)\\}"), replace);
			String cmd = "result.value = \""+txtFn+"\";";
			txt2 = js.eval(cmd).toString();
		} catch (ScriptException e) {
			throw Utils.runtime(e); // TODO fail gracefully?
		}
		return txt2;
	}


}
