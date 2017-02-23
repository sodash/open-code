package com.winterwell.utils.io;

import com.winterwell.utils.ReflectionUtils;

/**
 * @deprecated Prefer DBOptions, ESConfig - there isn't much common ground here
 * 
 * This class exists to contain generics from SqlUtils which might also exist in ElasticSearch.
 * @author alexn
 *
 */
public class DBUtils{
	
	public static DBOptions options;
	
	/**
	 * Set static database connection options.
	 * Obviously this won't do if you need to connect to multiple databases, but for most programs, that's fine.
	 * @param options Can be null to clear the options
	 */
	public static void setDBOptions(DBOptions newOptions) {
		assert (options == null 
				|| ReflectionUtils.equalish(options, newOptions)) : "Incompatible setup for SqlUtils static db connection.";
		options = newOptions;
		if (newOptions!=null) {
			assert (newOptions.dbUrl != null): newOptions;
		}
	}
	
	public static class DBOptions {
		@Option
		public String dbUrl;
		@Option
		public String dbUser;
		@Option
		public String dbPassword;		
				
		@Override
		public String toString() {
			return getClass().getSimpleName()+"["+dbUser+"@"+dbUrl+"]";
		}
	}
}
