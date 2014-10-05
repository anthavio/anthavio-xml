/**
 * 
 */
package net.anthavio.xml.test;

import java.io.FileReader;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;

import net.anthavio.xml.JaxpTraxFactory;
import net.anthavio.xml.JaxpTraxFactory.TraxImplementation;
import net.anthavio.xml.XmlParser;
import net.anthavio.xml.validation.JaxpSchemaFactory;
import net.anthavio.xml.validation.JaxpSchemaFactory.SchemaImplementation;
import net.anthavio.xml.validation.XmlSchemaLoader;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * @author vanek
 *
 */
public class XmlParserTest {

	XmlParser parser;

	public XmlParserTest() {
		JaxpSchemaFactory schemaFactory = new JaxpSchemaFactory(SchemaImplementation.XERCES);
		//Schema schema = XmlSchemaLoader.load(getClass().getResource("/schema/PingMessages.xsd"), schemaFactory);
		Schema schema = XmlSchemaLoader.load(getClass().getResource("/schema/sis/joint/NSMessages.xsd"), schemaFactory);

		parser = new XmlParser(schema);
		parser.setTraxFactory(new JaxpTraxFactory(TraxImplementation.JDK));
		parser.setIndenting(false);
	}

	@Test
	public void test() throws Exception {
		//FileReader reader = new FileReader("src/test/resources/xml/PingRequest-OK.xml");
		FileReader reader = new FileReader("src/test/resources/xml/NSCreateAlert.xml");
		Document document = parser.parse(reader);
		String string = parser.write(document);
		System.out.println(string);
		print(document);
	}

	private static String print(Document dom) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance(JaxpTraxFactory.TRAX_FACTORY_JAXP, null);
		//transformerFactory.setAttribute("indent-number", 2);
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(dom), new StreamResult(writer));

		System.out.println(writer.toString());
		return writer.toString();
	}

}
