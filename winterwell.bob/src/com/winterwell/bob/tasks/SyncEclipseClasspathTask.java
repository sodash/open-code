package com.winterwell.bob.tasks;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.wwjobs.BuildHacks;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.app.KServerType;

/**
 * status: SKETCH
 * Add/remove to .classpath so that the dependencies/x.jar match what's in the dependencies folder.
 * 
 * Use this after {@link MavenDependencyTask} as a convenient way to maintain the classpath.
 * 
 * @testedby  SyncEclipseClasspathTaskTest}
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
		if (BuildHacks.getServerType() != KServerType.LOCAL) {
			Log.d(LOGTAG, "SKIP sync task because not a local dev box");
			return;
		}
		
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
		// WTF extra blank lines??
		Pattern p = Pattern.compile("\\n[ \\t]*\\n");
		String xml3;
		while(true) {
			xml3 = p.matcher(xml2).replaceAll("\n");
			if (xml3.equals(xml2)) break;
			xml2 = xml3;
		}
		//		String xml3 = xml2.replaceAll(p, "\n");
		FileUtils.write(classpathFile, xml3);
	}

}
