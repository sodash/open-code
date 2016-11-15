///**
// *
// */
//package winterwell.utils;
//
//import java.lang.management.ManagementFactory;
//
//import javax.management.MBeanServer;
//
//import com.sun.management.HotSpotDiagnosticMXBean;
//
///**
// * Create a heap dump on Sun Hotspot JVMs
// * 
// * Halfinched from:
// * http://swik.net/openjdk/Java+Attributes/Programmatically+dumping
// * +heap+from+Java+applications
// * 
// * @author Joe Halliwell <joe@winterwell.com>
// * 
// */
//public class HotspotHeapDump {
//
//	// This is the name of the HotSpot Diagnostic MBean
//	private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";
//
//	// field to store the hotspot diagnostic MBean
//	private static volatile HotSpotDiagnosticMXBean hotspotMBean;
//
//	/**
//	 * Call this method from your application whenever you want to dump the heap
//	 * snapshot into a file.
//	 * 
//	 * @param fileName
//	 *            name of the heap dump file
//	 * @param live
//	 *            flag that tells whether to dump only the live objects
//	 */
//	public static void dump(String fileName, boolean live) {
//		// initialize hotspot diagnostic MBean
//		initHotspotMBean();
//		try {
//			hotspotMBean.dumpHeap(fileName, live);
//		} catch (RuntimeException re) {
//			throw re;
//		} catch (Exception exp) {
//			throw new RuntimeException(exp);
//		}
//	}
//
//	// get the hotspot diagnostic MBean from the
//	// platform MBean server
//	private static HotSpotDiagnosticMXBean getHotspotMBean() {
//		try {
//			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
//			HotSpotDiagnosticMXBean bean = ManagementFactory
//					.newPlatformMXBeanProxy(server, HOTSPOT_BEAN_NAME,
//							HotSpotDiagnosticMXBean.class);
//			return bean;
//		} catch (RuntimeException re) {
//			throw re;
//		} catch (Exception exp) {
//			throw new RuntimeException(exp);
//		}
//	}
//
//	// initialize the hotspot diagnostic MBean field
//	private static void initHotspotMBean() {
//		if (hotspotMBean == null) {
//			synchronized (HotspotHeapDump.class) {
//				if (hotspotMBean == null) {
//					hotspotMBean = getHotspotMBean();
//				}
//			}
//		}
//	}
//}
