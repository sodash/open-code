package com.winterwell.maths.graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.List;

import com.winterwell.utils.Proc;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.io.FileUtils;

/**
 * Export in dot format. See ?? for details.
 * <p>
 * Override the {@link #getEdgeStyle(IEdge)} and {@link #getNodeStyle(Object)}
 * methods to change the output
 * 
 * @author daniel
 * @testedby DotPrinterTest
 */
public class DotPrinter<N> {

	private static String DOT;

	/**
	 * 
	 * @param dotFile
	 * @param type
	 *            The output type. Use "png", "svg", "cmapx" or any of dot's
	 *            other types. For "cmapx" or "imap" (client/server image maps)
	 *            we will also generate a .png. Using a cmapx:
	 * 
	 *            <pre>
	 * 		[content of x.map]
	 * 		&gt;img src="x.png" usemap="#G" /&lt;
	 * </pre>
	 * @param pngFile
	 */
	public static void create(File dotFile, String type, File pngFile) {
		if (!dotFile.exists())
			throw new WrappedException(new FileNotFoundException(
					dotFile.toString()));
		if (DOT == null) {
			findDot();
		}
		if (DOT == null)
			throw new RuntimeException(
					"Could not find dot! Please install GraphViz Dot.");
		// image map? then also make a png
		String extra = "";
		if (type.equals("imap") || type.equals("cmapx")) {
			extra = " -Tpng -o " + FileUtils.changeType(pngFile, "png");
		}
		// run dot
		Proc p = new Proc(DOT + " -T" + type + " -o " + pngFile + " "
				+ dotFile + extra);
		p.run();
		p.waitFor();
	}
	
	public String createSVG() {
		try {
			File dotFile = File.createTempFile("dot", ".dot");
			File svgFile = File.createTempFile("dot", ".svg");
			FileUtils.write(dotFile, out());
			create(dotFile, "svg", svgFile);
			String svg = FileUtils.read(svgFile);
			FileUtils.delete(dotFile);
			FileUtils.delete(svgFile);
			return svg;
		} catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	/**
	 * Tries to find the dot program.
	 */
	public static void findDot() {
		try {
			// on the path?
			Proc p = new Proc("dot -V");
			p.redirectErrorStream(true);
			p.run();
			p.waitFor(1000);
			String out = p.getOutput();
			p.close();
			if (out.toLowerCase().contains("graphviz")) {
				// success
				DOT = "dot";
				return;
			}
			// which system?
			String os = Utils.getOperatingSystem();
			boolean windows = os.contains("windows");
			// try windows location
			if (windows) {
				findDot2_win();
				return;
			}
			System.err
					.println("Could not find dot. Please install GraphViz Dot! And maybe add it to your path.");
		} catch (Exception e) {
			// NB: don't wrap the low-level exception, 'cos accidental unwrapping would be unhelpful
			throw new RuntimeException("Could not find dot: "+e+" Is GraphViz Dot installed? Is it on your path?");
		}
	}

	private static void findDot2_win() {
		File pf = new File("C:\\Program Files");
		if (!pf.isDirectory()) {
			System.err
					.println("...DotPrinter: Could not find C:\\Program Files :(");
			return;
		}
		File[] gvs = pf.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().contains("graphviz");
			}
		});
		for (File dir : gvs) {
			List<File> dot = FileUtils.find(dir, "dot.exe");
			if (dot.isEmpty()) {
				continue;
			}
			System.out.println("...DotPrinter: found dot at " + dot);
			DOT = dot.get(0).getAbsolutePath();
			return;
		}
		System.err
				.println("...DotPrinter: Could not find graphviz in C:\\Program Files :(");
	}

	private IGraph<N> graph;

	/**
	 * Override the {@link #getEdgeStyle(IEdge)} and
	 * {@link #getNodeStyle(Object)} methods to change the output
	 * 
	 * @param g
	 */
	public DotPrinter(IGraph<N> g) {
		this.graph = g;
	}

	/**
	 * 
	 * @param edge
	 * @return E.g. "label=\"My Edge\",style=dotted"
	 */
	protected String getEdgeStyle(IEdge<N> edge) {
		// weighted?
		if (edge instanceof IEdge.Weighted) {
			double w = ((IEdge.Weighted) edge).getWeight();
			if (w>0) {
				return "label=\""+StrUtils.toNSigFigs(w, 2)+"\"";
			}
		}		
		
		return null;
	}

	/**
	 * Dot node names need to be simple & unique. Labels (in the style) can be more complex.
	 * <p>
	 * Default behaviour: rely on {@link #getLabel(Object)}
	 * @param object
	 * @return
	 */
	protected String getName(N object) {
		String lbl = getLabel(object);
		if (lbl==null || lbl.isEmpty()) lbl = object.toString()+object.hashCode();
		return lbl.replaceAll("\\W", "");
	}
	
	protected String getLabel(N object) {
		if (object instanceof IHasValue) {
			Object v = ((IHasValue) object).getValue();
			if (v != null) {
				return StrUtils.ellipsize(v.toString(), 140);		
			}
		}
		return StrUtils.ellipsize(object.toString(), 140);
	}

	/**
	 * 
	 * @param object
	 * @return E.g. "label=\"My Node\",shape=circle"
	 */
	protected String getNodeStyle(N object) {
		return "shape=box,label=\""+getLabel(object)+"\"";
	}

	/**
	 * 
	 * @return dot format output
	 */
	public String out() {
		StringBuilder sb = new StringBuilder();
		String e;
		if (graph instanceof IGraph.Directed) {
			sb.append("digraph G {\n");
			e = "->";
		} else {
			sb.append("graph G {\n");
			e = "--";
		}
		Collection<? extends N> nodes = graph.getNodes();
		for (N object : nodes) {
			assert object != null;
			sb.append(getName(object) + "[" + getNodeStyle(object) + "]\n");
			Collection<? extends IEdge> edges = graph.getEdges(object);
			for (IEdge edge : edges) {
				N end = (N) edge.getOtherEnd(object);
				assert end != null : edge;
				sb.append(getName(object) + " " + e + " " + getName(end));
				if (getEdgeStyle(edge) != null) {
					sb.append("[" + getEdgeStyle(edge) + "]");
				}
				sb.append(StrUtils.LINEEND);
			}
		}
		sb.append("}\n");
		return sb.toString();
	}

	/**
	 * @deprecated You probably want {@link #out()}
	 */
	@Deprecated
	@Override
	public String toString() {
		return super.toString();
	}

	public void writePng(File pngFile) {
		File dotFile = null;
		try {
			dotFile = File.createTempFile("dot", ".dot");
			FileUtils.write(dotFile, out());
	
			DotPrinter.create(dotFile, "png", pngFile);
						
		} catch(Exception ex) {
			throw Utils.runtime(ex);
		} finally {
			FileUtils.delete(dotFile);
		}
	}

}
