/**
 * 
 */
package com.anthavio.xml.validation;

import com.anthavio.xml.XPathTracker;

/**
 * @author vanek
 * 
 * XmlErrorHandler preconfigured NOT to fail fast on first parse error 
 */
public class SilentErrorHandler extends XmlErrorHandler {

	public SilentErrorHandler() {
		super(false);
	}

	public SilentErrorHandler(XPathTracker xpathTracker) {
		super(xpathTracker, false);
	}

}
