/*
 * (c) Winterwell Associates Ltd, 2008-2011
 * All rights reserved except.
 */
package com.winterwell.utils.web;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import winterwell.utils.FailureException;
import winterwell.utils.IReplace;
import winterwell.utils.NotUniqueException;

import com.winterwell.utils.Printer;
import com.winterwell.utils.Proc;

import winterwell.utils.StrUtils;
import winterwell.utils.TodoException;
import winterwell.utils.Utils;
import winterwell.utils.WrappedException;

import com.winterwell.utils.containers.ArrayMap;

import winterwell.utils.containers.ITree;
import winterwell.utils.containers.Tree;

import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.io.FileUtils;

import winterwell.utils.reporting.Log;
import winterwell.utils.time.TUnit;

/**
 * @deprecated use com.
 * ...Upgrade path...
 */
public class WebUtils extends winterwell.utils.web.WebUtils {
	

}
