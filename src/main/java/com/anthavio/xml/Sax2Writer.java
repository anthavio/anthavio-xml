/**
 * 
 */
package com.anthavio.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * @author vanek
 * 
 * SAX->Out
 * kopie com.sun.xml.bind.marshaller.XmlWriter
 */
public class Sax2Writer extends XMLFilterImpl {

	////////////////////////////////////////////////////////////////////
	// Constructors.
	////////////////////////////////////////////////////////////////////

	public Sax2Writer(Writer writer, String encoding) {
		init(writer, encoding);
		this.escapeHandler = DumbEscapeHandler.theInstance;
	}

	public Sax2Writer(OutputStream stream, String encoding) {
		setOutput(new OutputStreamWriter(stream), encoding);
		this.escapeHandler = DumbEscapeHandler.theInstance;
	}

	/**
	 * Internal initialization method.
	 *
	 * <p>All of the public constructors invoke this method.
	 *
	 * @param writer The output destination, or null to use
	 *        standard output.
	 */
	private void init(Writer writer, String encoding) {
		setOutput(writer, encoding);
	}

	////////////////////////////////////////////////////////////////////
	// Public methods.
	////////////////////////////////////////////////////////////////////

	/**
	 * Reset the writer.
	 *
	 * <p>This method is especially useful if the writer throws an
	 * exception before it is finished, and you want to reuse the
	 * writer for a new document.  It is usually a good idea to
	 * invoke {@link #flush flush} before resetting the writer,
	 * to make sure that no output is lost.</p>
	 *
	 * <p>This method is invoked automatically by the
	 * {@link #startDocument startDocument} method before writing
	 * a new document.</p>
	 *
	 * <p><strong>Note:</strong> this method will <em>not</em>
	 * clear the prefix or URI information in the writer or
	 * the selected output writer.</p>
	 *
	 * @see #flush()
	 */
	public void reset() {
		elementLevel = 0;
		startTagIsClosed = true;
	}

	/**
	 * Flush the output.
	 *
	 * <p>This method flushes the output stream.  It is especially useful
	 * when you need to make certain that the entire document has
	 * been written to output but do not want to close the output
	 * stream.</p>
	 *
	 * <p>This method is invoked automatically by the
	 * {@link #endDocument endDocument} method after writing a
	 * document.</p>
	 *
	 * @see #reset()
	 */
	public void flush() throws IOException {
		output.flush();
	}

	/**
	 * Set a new output destination for the document.
	 *
	 * @param writer The output destination, or null to use
	 *        standard output.
	 * @see #flush()
	 */
	public void setOutput(Writer writer, String _encoding) {
		if (writer == null) {
			output = new OutputStreamWriter(System.out);
		} else {
			output = writer;
		}
		encoding = _encoding;
	}

	/**
	 * Set whether the writer should print out the XML declaration
	 * (&lt;?xml version='1.0' ... ?>).
	 * <p>
	 * This option is set to true by default. 
	 */
	public void setXmlDecl(boolean _writeXmlDecl) {
		this.writeXmlDecl = _writeXmlDecl;
	}

	/**
	 * Sets the header string.
	 * 
	 * This string will be written right after the xml declaration
	 * without any escaping. Useful for generating a boiler-plate
	 * DOCTYPE decl, PIs, and comments.
	 * 
	 * @param _header
	 *      passing null will work as if the empty string is passed.   
	 */
	public void setHeader(String _header) {
		this.header = _header;
	}

	private final HashMap<String, String> locallyDeclaredPrefix = new HashMap<String, String>();

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		locallyDeclaredPrefix.put(prefix, uri);
	}

	////////////////////////////////////////////////////////////////////
	// Methods from org.xml.sax.ContentHandler.
	////////////////////////////////////////////////////////////////////

	/**
	 * Write the XML declaration at the beginning of the document.
	 *
	 * Pass the event on down the filter chain for further processing.
	 *
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the XML declaration, or if a handler further down
	 *            the filter chain raises an exception.
	 * @see org.xml.sax.ContentHandler#startDocument()
	 */
	@Override
	public void startDocument() throws SAXException {
		try {
			reset();

			if (writeXmlDecl) {
				String e = "";
				if (encoding != null) {
					e = " encoding=\"" + encoding + '\"';
				}

				writeXmlDecl("<?xml version=\"1.0\"" + e + " standalone=\"yes\"?>");
			}

			if (header != null) {
				write(header);
			}

			super.startDocument();
		} catch (IOException e) {
			throw new SAXException(e);
		}
	}

	protected void writeXmlDecl(String decl) throws IOException {
		write(decl);
	}

	/**
	 * Write a newline at the end of the document.
	 *
	 * Pass the event on down the filter chain for further processing.
	 *
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the newline, or if a handler further down
	 *            the filter chain raises an exception.
	 * @see org.xml.sax.ContentHandler#endDocument()
	 */
	@Override
	public void endDocument() throws SAXException {
		try {
			super.endDocument();
			flush();
		} catch (IOException e) {
			throw new SAXException(e);
		}
	}

	/**
	 * Write a start tag.
	 *
	 * Pass the event on down the filter chain for further processing.
	 *
	 * @param uri The Namespace URI, or the empty string if none
	 *        is available.
	 * @param localName The element's local (unprefixed) name (required).
	 * @param qName The element's qualified (prefixed) name, or the
	 *        empty string is none is available.  This method will
	 *        use the qName as a template for generating a prefix
	 *        if necessary, but it is not guaranteed to use the
	 *        same qName.
	 * @param atts The element's attribute list (must not be null).
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the start tag, or if a handler further down
	 *            the filter chain raises an exception.
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		try {
			if (!startTagIsClosed) {
				write(">");
			}
			elementLevel++;
			//            nsSupport.pushContext();

			write('<');
			write(qName);
			writeAttributes(atts);

			// declare namespaces specified by the startPrefixMapping methods
			if (!locallyDeclaredPrefix.isEmpty()) {
				for (Map.Entry<String, String> e : locallyDeclaredPrefix.entrySet()) {
					String p = e.getKey();
					String u = e.getValue();
					if (u == null) {
						u = "";
					}
					write(' ');
					if ("".equals(p)) {
						write("xmlns=\"");
					} else {
						write("xmlns:");
						write(p);
						write("=\"");
					}
					char ch[] = u.toCharArray();
					writeEsc(ch, 0, ch.length, true);
					write('\"');
				}
				locallyDeclaredPrefix.clear(); // clear the contents
			}

			//            if (elementLevel == 1) {
			//                forceNSDecls();
			//            }
			//            writeNSDecls();
			super.startElement(uri, localName, qName, atts);
			startTagIsClosed = false;
		} catch (IOException e) {
			throw new SAXException(e);
		}
	}

	/**
	 * Write an end tag.
	 *
	 * Pass the event on down the filter chain for further processing.
	 *
	 * @param uri The Namespace URI, or the empty string if none
	 *        is available.
	 * @param localName The element's local (unprefixed) name (required).
	 * @param qName The element's qualified (prefixed) name, or the
	 *        empty string is none is available.  This method will
	 *        use the qName as a template for generating a prefix
	 *        if necessary, but it is not guaranteed to use the
	 *        same qName.
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the end tag, or if a handler further down
	 *            the filter chain raises an exception.
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			if (startTagIsClosed) {
				write("</");
				write(qName);
				write('>');
			} else {
				write("/>");
				startTagIsClosed = true;
			}
			super.endElement(uri, localName, qName);
			//            nsSupport.popContext();
			elementLevel--;
		} catch (IOException e) {
			throw new SAXException(e);
		}
	}

	/**
	 * Write character data.
	 *
	 * Pass the event on down the filter chain for further processing.
	 *
	 * @param ch The array of characters to write.
	 * @param start The starting position in the array.
	 * @param len The number of characters to write.
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the characters, or if a handler further down
	 *            the filter chain raises an exception.
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char ch[], int start, int len) throws SAXException {
		try {
			if (!startTagIsClosed) {
				write('>');
				startTagIsClosed = true;
			}
			writeEsc(ch, start, len, false);
			super.characters(ch, start, len);
		} catch (IOException e) {
			throw new SAXException(e);
		}
	}

	/**
	 * Write ignorable whitespace.
	 *
	 * Pass the event on down the filter chain for further processing.
	 *
	 * @param ch The array of characters to write.
	 * @param start The starting position in the array.
	 * @param length The number of characters to write.
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the whitespace, or if a handler further down
	 *            the filter chain raises an exception.
	 * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
	 */
	@Override
	public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
		try {
			writeEsc(ch, start, length, false);
			super.ignorableWhitespace(ch, start, length);
		} catch (IOException e) {
			throw new SAXException(e);
		}
	}

	/**
	 * Write a processing instruction.
	 *
	 * Pass the event on down the filter chain for further processing.
	 *
	 * @param target The PI target.
	 * @param data The PI data.
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the PI, or if a handler further down
	 *            the filter chain raises an exception.
	 * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
	 */
	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		try {
			if (!startTagIsClosed) {
				write('>');
				startTagIsClosed = true;
			}
			write("<?");
			write(target);
			write(' ');
			write(data);
			write("?>");
			if (elementLevel < 1) {
				write('\n');
			}
			super.processingInstruction(target, data);
		} catch (IOException e) {
			throw new SAXException(e);
		}
	}

	////////////////////////////////////////////////////////////////////
	// Convenience methods.
	////////////////////////////////////////////////////////////////////

	/**
	 * Start a new element without a qname or attributes.
	 *
	 * <p>This method will provide a default empty attribute
	 * list and an empty string for the qualified name.  
	 * It invokes {@link 
	 * #startElement(String, String, String, Attributes)}
	 * directly.</p>
	 *
	 * @param uri The element's Namespace URI.
	 * @param localName The element's local name.
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the start tag, or if a handler further down
	 *            the filter chain raises an exception.
	 * @see #startElement(String, String, String, Attributes)
	 */
	public void startElement(String uri, String localName) throws SAXException {
		startElement(uri, localName, "", EMPTY_ATTS);
	}

	/**
	 * Start a new element without a qname, attributes or a Namespace URI.
	 *
	 * <p>This method will provide an empty string for the
	 * Namespace URI, and empty string for the qualified name,
	 * and a default empty attribute list. It invokes
	 * #startElement(String, String, String, Attributes)}
	 * directly.</p>
	 *
	 * @param localName The element's local name.
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the start tag, or if a handler further down
	 *            the filter chain raises an exception.
	 * @see #startElement(String, String, String, Attributes)
	 */
	public void startElement(String localName) throws SAXException {
		startElement("", localName, "", EMPTY_ATTS);
	}

	/**
	 * End an element without a qname.
	 *
	 * <p>This method will supply an empty string for the qName.
	 * It invokes {@link #endElement(String, String, String)}
	 * directly.</p>
	 *
	 * @param uri The element's Namespace URI.
	 * @param localName The element's local name.
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the end tag, or if a handler further down
	 *            the filter chain raises an exception.
	 * @see #endElement(String, String, String)
	 */
	public void endElement(String uri, String localName) throws SAXException {
		endElement(uri, localName, "");
	}

	/**
	 * End an element without a Namespace URI or qname.
	 *
	 * <p>This method will supply an empty string for the qName
	 * and an empty string for the Namespace URI.
	 * It invokes {@link #endElement(String, String, String)}
	 * directly.</p>
	 *
	 * @param localName The element's local name.
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the end tag, or if a handler further down
	 *            the filter chain raises an exception.
	 * @see #endElement(String, String, String)
	 */
	public void endElement(String localName) throws SAXException {
		endElement("", localName, "");
	}

	/**
	 * Write an element with character data content.
	 *
	 * <p>This is a convenience method to write a complete element
	 * with character data content, including the start tag
	 * and end tag.</p>
	 *
	 * <p>This method invokes
	 * {@link #startElement(String, String, String, Attributes)},
	 * followed by
	 * {@link #characters(String)}, followed by
	 * {@link #endElement(String, String, String)}.</p>
	 *
	 * @param uri The element's Namespace URI.
	 * @param localName The element's local name.
	 * @param qName The element's default qualified name.
	 * @param atts The element's attributes.
	 * @param content The character data content.
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the empty tag, or if a handler further down
	 *            the filter chain raises an exception.
	 * @see #startElement(String, String, String, Attributes)
	 * @see #characters(String)
	 * @see #endElement(String, String, String)
	 */
	public void dataElement(String uri, String localName, String qName, Attributes atts, String content)
			throws SAXException {
		startElement(uri, localName, qName, atts);
		characters(content);
		endElement(uri, localName, qName);
	}

	/**
	 * Write an element with character data content but no attributes.
	 *
	 * <p>This is a convenience method to write a complete element
	 * with character data content, including the start tag
	 * and end tag.  This method provides an empty string
	 * for the qname and an empty attribute list.</p>
	 *
	 * <p>This method invokes
	 * {@link #startElement(String, String, String, Attributes)},
	 * followed by
	 * {@link #characters(String)}, followed by
	 * {@link #endElement(String, String, String)}.</p>
	 *
	 * @param uri The element's Namespace URI.
	 * @param localName The element's local name.
	 * @param content The character data content.
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the empty tag, or if a handler further down
	 *            the filter chain raises an exception.
	 * @see #startElement(String, String, String, Attributes)
	 * @see #characters(String)
	 * @see #endElement(String, String, String)
	 */
	public void dataElement(String uri, String localName, String content) throws SAXException {
		dataElement(uri, localName, "", EMPTY_ATTS, content);
	}

	/**
	 * Write an element with character data content but no attributes or Namespace URI.
	 *
	 * <p>This is a convenience method to write a complete element
	 * with character data content, including the start tag
	 * and end tag.  The method provides an empty string for the
	 * Namespace URI, and empty string for the qualified name,
	 * and an empty attribute list.</p>
	 *
	 * <p>This method invokes
	 * {@link #startElement(String, String, String, Attributes)},
	 * followed by
	 * {@link #characters(String)}, followed by
	 * {@link #endElement(String, String, String)}.</p>
	 *
	 * @param localName The element's local name.
	 * @param content The character data content.
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the empty tag, or if a handler further down
	 *            the filter chain raises an exception.
	 * @see #startElement(String, String, String, Attributes)
	 * @see #characters(String)
	 * @see #endElement(String, String, String)
	 */
	public void dataElement(String localName, String content) throws SAXException {
		dataElement("", localName, "", EMPTY_ATTS, content);
	}

	/**
	 * Write a string of character data, with XML escaping.
	 *
	 * <p>This is a convenience method that takes an XML
	 * String, converts it to a character array, then invokes
	 * {@link #characters(char[], int, int)}.</p>
	 *
	 * @param data The character data.
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the string, or if a handler further down
	 *            the filter chain raises an exception.
	 * @see #characters(char[], int, int)
	 */
	public void characters(String data) throws SAXException {
		try {
			if (!startTagIsClosed) {
				write('>');
				startTagIsClosed = true;
			}
			char ch[] = data.toCharArray();
			characters(ch, 0, ch.length);
		} catch (IOException e) {
			throw new SAXException(e);
		}
	}

	////////////////////////////////////////////////////////////////////
	// Internal methods.
	////////////////////////////////////////////////////////////////////

	/**
	 * Write a raw character.
	 *
	 * @param c The character to write.
	 */
	protected final void write(char c) throws IOException {
		output.write(c);
	}

	/**
	 * Write a raw string.
	 */
	protected final void write(String s) throws IOException {
		output.write(s);
	}

	/**
	 * Write out an attribute list, escaping values.
	 *
	 * The names will have prefixes added to them.
	 *
	 * @param atts The attribute list to write.
	 */
	private void writeAttributes(Attributes atts) throws IOException {
		int len = atts.getLength();
		for (int i = 0; i < len; i++) {
			char ch[] = atts.getValue(i).toCharArray();
			write(' ');
			write(atts.getQName(i));
			write("=\"");
			writeEsc(ch, 0, ch.length, true);
			write('"');
		}
	}

	/**
	 * Write an array of data characters with escaping.
	 *
	 * @param ch The array of characters.
	 * @param start The starting position.
	 * @param length The number of characters to use.
	 * @param isAttVal true if this is an attribute value literal.
	 */
	private void writeEsc(char ch[], int start, int length, boolean isAttVal) throws IOException {
		escapeHandler.escape(ch, start, length, isAttVal, output);
	}

	////////////////////////////////////////////////////////////////////
	// Constants.
	////////////////////////////////////////////////////////////////////

	private final Attributes EMPTY_ATTS = new AttributesImpl();

	////////////////////////////////////////////////////////////////////
	// Internal state.
	////////////////////////////////////////////////////////////////////

	private int elementLevel = 0;
	private Writer output;
	private String encoding;
	private boolean writeXmlDecl = true;
	/**
	 * This string will be written right after the xml declaration
	 * without any escaping. Useful for generating a boiler-plate DOCTYPE decl
	 * , PIs, and comments.
	 */
	private String header = null;

	private final DumbEscapeHandler escapeHandler;

	private boolean startTagIsClosed = true;

	static class DumbEscapeHandler {

		private DumbEscapeHandler() {
		} // no instanciation please

		public static final DumbEscapeHandler theInstance = new DumbEscapeHandler();

		public void escape(char[] ch, int start, int length, boolean isAttVal, Writer out) throws IOException {
			int limit = start + length;
			for (int i = start; i < limit; i++) {
				switch (ch[i]) {
				case '&':
					out.write("&amp;");
					break;
				case '<':
					out.write("&lt;");
					break;
				case '>':
					out.write("&gt;");
					break;
				case '\"':
					if (isAttVal) {
						out.write("&quot;");
					} else {
						out.write('\"');
					}
					break;
				default:
					if (ch[i] > '\u007f') {
						out.write("&#");
						out.write(Integer.toString(ch[i]));
						out.write(';');
					} else {
						out.write(ch[i]);
					}
				}
			}
		}
	}
}
