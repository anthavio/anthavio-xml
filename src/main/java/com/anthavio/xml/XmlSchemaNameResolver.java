/**
 * 
 */
package com.anthavio.xml;

import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.dom.DOMSource;

import com.anthavio.io.ResetableInputStream;
import com.anthavio.io.ResetableReader;
import com.anthavio.xml.stax.ResetableStaxEventReader;

/**
 * @author vanek
 *
 */
public interface XmlSchemaNameResolver {

	public String getSchemaName(DOMSource source);

	public String getSchemaName(ResetableStaxEventReader resetableStaxReader);

	public String getSchemaName(ResetableStreamSource resetableSource, XMLInputFactory staxInputFactory);

	public String getSchemaName(ResetableInputStream resetableStream, XMLInputFactory staxInputFactory);

	public String getSchemaName(ResetableReader resetableReader, XMLInputFactory staxInputFactory);
}
