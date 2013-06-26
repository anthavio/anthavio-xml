package com.anthavio.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.xml.sax.InputSource;

/**
 * Stax XMLResolver
 * 
 * resolver = XmlEntityResolver("...");
 * XMLInputFactory factory = XMLInputFactory.newInstance();
 * factory.setProperty(XMLInputFactory.RESOLVER, resolver);
 * factory.setProperty(XMLInputFactory.SUPPORT_DTD, supportDtd);
 * 
 * Sax EntityResolver
 * 
 * resolver = XmlEntityResolver("...");
 * XMLReader xmlReader = saxParser.getXMLReader();
 * xmlReader.setEntityResolver(resolver);
 * 
 * @author martin.vanek
 *
 */
public class XmlEntityResolver implements javax.xml.stream.XMLResolver, org.xml.sax.EntityResolver {

	public static enum Case {
		PRESERVE, TO_UPPER, TO_LOWER;
	}

	private Case caseHandling = Case.PRESERVE;

	private final File baseDirectory;

	private final String baseResource;

	private Map<String, String> systemIdMap;

	public XmlEntityResolver(File file) {
		this.baseDirectory = init(file);
		this.baseResource = null;
	}

	private File init(File file) {
		if (!file.exists()) {
			throw new IllegalArgumentException("Does not exist " + file.getAbsolutePath());
		}

		if (file.isFile()) {
			if (!file.canRead()) {
				throw new IllegalArgumentException("Can't read " + file.getAbsolutePath());
			}
			return file.getParentFile();

		} else if (file.isDirectory()) {
			return file;
		} else {
			throw new IllegalStateException("Neither File nor Directory " + file.getAbsolutePath());
		}
	}

	public XmlEntityResolver(String resource) {
		if (resource.startsWith("file:")) {
			this.baseDirectory = init(new File(resource.substring(5)));
			this.baseResource = null;
		} else {
			if (resource.startsWith("classpath:")) {
				resource = resource.substring(10);
			}
			//remove leading / if exist
			//ClassLoader.getResource(...) search goes for the root (/) 
			//Class.getResource(...) search is relative to the package of used Class (then leading / is needed for root selection)
			if (resource.charAt(0) == '/') {
				resource = resource.substring(1);
			}
			//append trailing / if not exist
			if (resource.charAt(resource.length() - 1) != '/') {
				resource = resource + "/";
			}

			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			if (classLoader == null) {
				classLoader = getClass().getClassLoader();
			}
			URL url = classLoader.getResource(resource);
			if (url == null) {
				throw new IllegalArgumentException("Resource does not exist " + resource);
			}
			this.baseResource = resource;
			this.baseDirectory = null;
		}
	}

	public Case getCaseHandling() {
		return caseHandling;
	}

	public void setCaseHandling(Case caseHandling) {
		this.caseHandling = caseHandling;
	}

	public Map<String, String> getSystemIdMap() {
		return systemIdMap;
	}

	public void setSystemIdMap(Map<String, String> systemIdMap) {
		this.systemIdMap = systemIdMap;
	}

	/**
	 * Stax
	 */
	@Override
	public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace) {
		String resource = systemID;
		if (caseHandling == Case.TO_UPPER) {
			resource = resource.toUpperCase();
		} else if (caseHandling == Case.TO_LOWER) {
			resource = resource.toLowerCase();
		} else {
			//PRESERVE = do nothing
		}

		if (systemIdMap != null) {
			String mappedID = systemIdMap.get(resource);
			if (mappedID != null) {
				resource = mappedID;
			}
		}

		if (baseResource != null) {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			if (classLoader == null) {
				classLoader = getClass().getClassLoader();
			}
			InputStream stream = classLoader.getResourceAsStream(baseResource + resource);
			if (stream == null) {
				throw new IllegalArgumentException("Entity resource '" + systemID + "' not found as " + baseResource + resource);
			}
			return stream;
		} else { //baseDirectory then
			File file = new File(baseDirectory, resource);
			try {
				return new FileInputStream(file);
			} catch (FileNotFoundException fnfx) {
				throw new IllegalArgumentException("Entity file '" + systemID + "' not found as " + file.getAbsolutePath());
			}
		}
	}

	private String firstSystemIdPath;

	/**
	 * SAX
	 */
	@Override
	public InputSource resolveEntity(String publicId, String systemId) {
		//SAX parser sends systemId as absolute path/url - usually useless information

		//when parsing XML from File or Url, xml parser can use xml path in systemID for Entity lookup...
		//While this might be useful when parsing URL, we must throw away this path
		String resource;
		int lastSlash = systemId.lastIndexOf('/');
		if (firstSystemIdPath == null) {
			//remove whole path from first entity
			if (lastSlash != -1) {
				firstSystemIdPath = systemId.substring(0, lastSlash);
				resource = systemId.substring(lastSlash + 1);
			} else {
				resource = systemId;
			}
		} else {
			//aditional entity 
			if (systemId.startsWith(firstSystemIdPath)) {
				//this one's path is relative to first entity (often in subdirectory)
				resource = systemId.substring(firstSystemIdPath.length());
			} else if (lastSlash != -1) {
				resource = systemId.substring(lastSlash + 1);
			} else {
				resource = systemId;
			}
		}

		InputStream stream = (InputStream) resolveEntity(publicId, resource, null, null);
		if (stream != null) {
			return new InputSource(stream);
		} else {
			return null;
		}
	}

}
