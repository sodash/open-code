//package com.winterwell.bob.tasks;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URL;
//import java.util.Enumeration;
//
//class TrackingClassLoader extends ClassLoader {
//
//	@Override
//	protected String findLibrary(String libname) {
//		
//		String lib = super.findLibrary(libname);
//		System.out.println(lib);
//		return lib;
//	}
//	
//	@Override
//	protected Enumeration<URL> findResources(String name) throws IOException {
//		// TODO Auto-generated method stub
//		return super.findResources(name);
//	}
//	
//	@Override
//	public InputStream getResourceAsStream(String name) {
//		 System.out.println("get resource as stream: "+name);
//		return super.getResourceAsStream(name);
//	}
//	
//	@Override
//	public Class<?> loadClass(String name) throws ClassNotFoundException {		
//		System.out.println(name);
//		Class<?> k = super.loadClass(name);
//		int i = name.lastIndexOf('.');
//		String cn = i==-1? name : name.substring(i+1);
//		URL r = k.getResource("");
//		System.out.println(r);
//		URL r2 = k.getResource(cn+".class");
//		System.out.println(cn+": "+r2);
//		return k;
//	}
//	
//	@Override
//	protected synchronized Class<?> loadClass(String name, boolean resolve)
//			throws ClassNotFoundException {
//		System.out.println(name);
//		return super.loadClass(name, resolve);
//	}
//	
//	
//	
//	public TrackingClassLoader(ClassLoader cl) {
//		super(cl);
//	}
//	
//	@Override
//	protected URL findResource(String name) {
//		System.out.println(name);
//		URL r = super.findResource(name);	
//		System.out.println(r);
//		return r;
//	}
//	
//	@Override
//	protected Class<?> findClass(String name) throws ClassNotFoundException {
//		System.out.println(name);
//		return super.findClass(name);
//	}
//	@Override
//	public URL getResource(String name) {
//		URL url = super.getResource(name);
//		System.out.println(url);
//		return url;
//	}
//	
//	@Override
//	public Enumeration<URL> getResources(String name) throws IOException {
//		Enumeration<URL> rs = super.getResources(name);
//		return rs;
//	}
//}