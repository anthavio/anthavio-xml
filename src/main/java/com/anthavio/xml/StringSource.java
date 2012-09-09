package com.anthavio.xml;

import java.io.StringReader;

import javax.xml.transform.stream.StreamSource;

/**
 * @author vanek
 */
public class StringSource extends StreamSource {

	public StringSource(String content) {
		super(new StringReader(content));
	}

	public StringSource(String content, String systemId) {
		super(new StringReader(content), systemId);
	}

}