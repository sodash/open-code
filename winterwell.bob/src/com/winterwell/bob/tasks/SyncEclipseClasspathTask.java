package com.winterwell.bob.tasks;

import java.io.File;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.containers.Tree;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.utils.web.XMLNode;

/**
 * status: SKETCH
 * Add/remove to .classpath so that the dependencies/x.jar match what's in the dependencies folder.
 * 
 * Use this after {@link MavenDependencyTask} as a convenient way to maintain the classpath.
 * 
 * @testedby {@link SyncEclipseClasspathTaskTest}
 * @author daniel
 *
 */
public class SyncEclipseClasspathTask extends BuildTask {

	File projectDir;
	File depsDir;
	File classpathFile;
	
	public SyncEclipseClasspathTask(File projectDir) {
		this.projectDir = projectDir;
		depsDir = new File(projectDir, MavenDependencyTask.MAVEN_DEPENDENCIES_FOLDER);
		classpathFile = new File(projectDir, ".classpath");
	}
	
	@Override
	protected void doTask() throws Exception {
		String[] ajars = depsDir.list();
		HashSet<String> jars = new HashSet();
		for (String j : ajars) {
			if (j.endsWith(".jar")) jars.add(j);
		}
		System.out.println(jars);
		
		String xml = FileUtils.read(classpathFile);
		Document doc= WebUtils2.parseXml(xml);
		// <classpathentry kind="src" path="server/src"/>
		List<Node> nodes = WebUtils2.xpathQuery2("/classpath/classpathentry[@kind='lib']", doc, false);
		for (Node node : nodes) {
			String path = WebUtils2.getAttribute("path", node);
			if ( ! path.startsWith(depsDir.getName())) {
				continue;
			}
			File jarFile = new File(depsDir.getParentFile(), path);
			if ( ! jarFile.exists()) {
				Log.i(LOGTAG, "remove stale dependency "+path+" from "+projectDir.getName());
				node.getParentNode().removeChild(node);
			} else {
				jars.remove(jarFile.getName());
			}
		}
		// add missing ones
		Node cp = doc.getElementsByTagName("classpath").item(0);
		Node n = WebUtils2.xpathQuery2("/classpath/classpathentry", doc, false).get(0).cloneNode(false);
		Document odoc = n.getOwnerDocument();
		Map<String, String> attr = WebUtils2.getAttributeMap(n);
		attr.clear();
		String drel = depsDir.getName(); // TODO relative to projectDir??
		for(String j : jars) {
			Node dep = n.cloneNode(false);
			Map<String, String> dattr = WebUtils2.getAttributeMap(dep);
			Document odoc2 = dep.getOwnerDocument();
			dattr.put("kind", "lib");
			dattr.put("path", drel+"/"+j);
			Node p = dep.getParentNode();
			cp.appendChild(dep);
			Log.i(LOGTAG, "add fresh dependency "+dattr.get("path")+" to "+projectDir.getName());
		}
		// output
		String xml2 = WebUtils2.xmlDocToString(doc);
		FileUtils.write(classpathFile, xml2);
	}

}
