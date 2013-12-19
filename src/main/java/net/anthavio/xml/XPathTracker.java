/**
 * 
 */
package net.anthavio.xml;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * @author vanek
 *
 */
public interface XPathTracker {

	public String getXPath();

	/**
	 * DataHolder class for tracking XPath 
	 */
	static class XpathHolder {
		private final Map<String, Integer> occurrence = new HashMap<String, Integer>();

		private String elementName;

		private int occ;

		/*
			public Histogram(String namespaceUri, String localName, String prefix) {
				update(namespaceUri, localName, prefix);
			}
		*/
		public XpathHolder() {
			//default
		}

		public void update(String namespaceUri, String localName, String prefix) {
			String unique = namespaceUri + localName;
			Integer occ = occurrence.get(unique);
			if (occ == null) {
				occ = 1;
			} else {
				occ++;
			}
			occurrence.put(unique, occ);
			this.occ = occ;

			if (StringUtils.isNotEmpty(prefix)) {
				elementName = prefix + ":" + localName;
			} else {
				elementName = localName;
			}
		}

		public void appendPath(StringBuilder buf) {
			if (elementName == null) {
				return; // this is the head
			}
			buf.append('/');
			buf.append(elementName);
			if (occ != 1) {
				buf.append('[');
				buf.append(occ);
				buf.append(']');
			}
		}
	}
}
