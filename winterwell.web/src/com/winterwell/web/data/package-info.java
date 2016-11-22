
/**
 * What are these creole/SoDash classes doing here in a utils library?
 * <p>
 * Well, {@link com.winterwell.web.data.XId} has turned out to be a really nifty id wrapper (hence utils). 
 * But if we changed the package, we'd break lots of xstream-serialised objects.  
 * 
 * @author daniel
 */
package com.winterwell.web.data;