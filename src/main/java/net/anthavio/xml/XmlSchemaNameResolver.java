/**
 * 
 */
package net.anthavio.xml;

import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.dom.DOMSource;

import net.anthavio.io.ResetableInputStream;
import net.anthavio.io.ResetableReader;
import net.anthavio.xml.stax.ResetableStaxEventReader;

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
