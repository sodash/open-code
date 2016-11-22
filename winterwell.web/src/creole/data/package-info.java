
/**
 * What are these creole/SoDash classes doing here in a utils library?
 * <p>
 * Well, {@link creole.data.XId} has turned out to be a really nifty id wrapper (hence utils). 
 * But if we changed the package, we'd break lots of xstream-serialised objects.  
 * 
 * @author daniel
 */
package creole.data;