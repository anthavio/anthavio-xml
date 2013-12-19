/**
 * 
 */
package net.anthavio.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.NamespaceContext;

/**
 * @author vanek
 *
 */
public class XmlNamespaceContext implements NamespaceContext {

	public static final String URI_XSI = "http://www.w3.org/2001/XMLSchema-instance";
	public static final String URI_XSD = "http://www.w3.org/2001/XMLSchema";

	private final HashMap<String, String> uri2prefix;

	private final HashMap<String, String> prefix2uri;

	public static final Iterator<String> EMPTY_ITERATOR = new EmptyIterator();

	public XmlNamespaceContext(HashMap<String, String> uri2prefix) {
		this.uri2prefix = uri2prefix;
		uri2prefix.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
		uri2prefix.put("http://www.w3.org/2001/XMLSchema", "xsd");

		prefix2uri = new HashMap<String, String>();
		Set<String> uriSet = uri2prefix.keySet();
		for (String uri : uriSet) {
			prefix2uri.put(uri2prefix.get(uri), uri);
		}
	}

	public List<String> getKnownNamespaceURIs() {
		Set<String> uris = uri2prefix.keySet();
		List<String> retval = new ArrayList<String>(uris.size());
		for (String uri : uris) {
			retval.add(uri);
		}
		return retval;
	}

	public String getNamespaceURI(String prefix) {
		return prefix2uri.get(prefix);
	}

	public String getPrefix(String namespaceURI) {
		return uri2prefix.get(namespaceURI);
	}

	public Iterator<?> getPrefixes(String namespaceUri) {
		String prefix = uri2prefix.get(namespaceUri);
		return prefix == null ? EMPTY_ITERATOR : new OneIterator(prefix);
	}

	public static class EmptyIterator implements Iterator<String> {

		public boolean hasNext() {
			return false;
		}

		public String next() {
			return null;
		}

		public void remove() {
		}

	}

	public static class OneIterator implements Iterator<String> {

		private final String string;

		private boolean taken;

		public OneIterator(String string) {
			this.string = string;
		}

		public boolean hasNext() {
			if (taken) {
				return false;
			} else {
				taken = true;
				return true;
			}
		}

		public String next() {
			return string;
		}

		public void remove() {
		}

	}
}
