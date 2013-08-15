/**
 * 
 */
package com.anthavio.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author vanek
 *
 */
public class Sax2IndentingWriter extends Sax2Writer {

	////////////////////////////////////////////////////////////////////
	// Constructors.
	////////////////////////////////////////////////////////////////////

	public Sax2IndentingWriter(Writer writer, String encoding) {
		super(writer, encoding);
	}

	public Sax2IndentingWriter(OutputStream stream, String encoding) {
		super(stream, encoding);
	}

	public void setIndentStep(String s) {
		this.indentStep = s;
	}

	////////////////////////////////////////////////////////////////////
	// Override methods from XMLWriter.
	////////////////////////////////////////////////////////////////////

	/**
	 * Reset the writer so that it can be reused.
	 *
	 * <p>This method is especially useful if the writer failed
	 * with an exception the last time through.</p>
	 *
	 * @see XMLWriter#reset()
	 */
	@Override
	public void reset() {
		depth = 0;
		state = SEEN_NOTHING;
		stateStack = new Stack<Object>();
		super.reset();
	}

	@Override
	protected void writeXmlDecl(String decl) throws IOException {
		super.writeXmlDecl(decl);
		write('\n');
	}

	/**
	 * Write a start tag.
	 *
	 * <p>Each tag will begin on a new line, and will be
	 * indented by the current indent step times the number
	 * of ancestors that the element has.</p>
	 *
	 * <p>The newline and indentation will be passed on down
	 * the filter chain through regular characters events.</p>
	 *
	 * @param uri The element's Namespace URI.
	 * @param localName The element's local name.
	 * @param qName The element's qualified (prefixed) name.
	 * @param atts The element's attribute list.
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the start tag, or if a filter further
	 *            down the chain raises an exception.
	 * @see XMLWriter#startElement(String, String, String, Attributes)
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		stateStack.push(SEEN_ELEMENT);
		state = SEEN_NOTHING;
		if (depth > 0) {
			super.characters("\n");
		}
		doIndent();
		super.startElement(uri, localName, qName, atts);
		depth++;
	}

	/**
	 * Write an end tag.
	 *
	 * <p>If the element has contained other elements, the tag
	 * will appear indented on a new line; otherwise, it will
	 * appear immediately following whatever came before.</p>
	 *
	 * <p>The newline and indentation will be passed on down
	 * the filter chain through regular characters events.</p>
	 *
	 * @param uri The element's Namespace URI.
	 * @param localName The element's local name.
	 * @param qName The element's qualified (prefixed) name.
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the end tag, or if a filter further
	 *            down the chain raises an exception.
	 * @see XMLWriter#endElement(String, String, String)
	 */
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		depth--;
		if (state == SEEN_ELEMENT) {
			super.characters("\n");
			doIndent();
		}
		super.endElement(uri, localName, qName);
		state = stateStack.pop();
	}

	@Override
	public void endDocument() throws SAXException {
		try {
			write('\n');
		} catch (IOException e) {
			throw new SAXException(e);
		}
		super.endDocument();
	}

	//  /**
	//   * Write a empty element tag.
	//   *
	//   * <p>Each tag will appear on a new line, and will be
	//   * indented by the current indent step times the number
	//   * of ancestors that the element has.</p>
	//   *
	//   * <p>The newline and indentation will be passed on down
	//   * the filter chain through regular characters events.</p>
	//   *
	//   * @param uri The element's Namespace URI.
	//   * @param localName The element's local name.
	//   * @param qName The element's qualified (prefixed) name.
	//   * @param atts The element's attribute list.
	//   * @exception org.xml.sax.SAXException If there is an error
	//   *            writing the empty tag, or if a filter further
	//   *            down the chain raises an exception.
	//   * @see XMLWriter#emptyElement(String, String, String, Attributes)
	//   */
	//  public void emptyElement (String uri, String localName,
	//                            String qName, Attributes atts)
	//      throws SAXException
	//  {
	//      state = SEEN_ELEMENT;
	//      if (depth > 0) {
	//          super.characters("\n");
	//      }
	//      doIndent();
	//      super.emptyElement(uri, localName, qName, atts);
	//  }

	/**
	 * Write a sequence of characters.
	 *
	 * @param ch The characters to write.
	 * @param start The starting position in the array.
	 * @param length The number of characters to use.
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the characters, or if a filter further
	 *            down the chain raises an exception.
	 * @see XMLWriter#characters(char[], int, int)
	 */
	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		state = SEEN_DATA;
		super.characters(ch, start, length);
	}

	////////////////////////////////////////////////////////////////////
	// Internal methods.
	////////////////////////////////////////////////////////////////////

	/**
	 * Print indentation for the current level.
	 *
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the indentation characters, or if a filter
	 *            further down the chain raises an exception.
	 */
	private void doIndent() throws SAXException {
		if (depth > 0) {
			char[] ch = indentStep.toCharArray();
			for (int i = 0; i < depth; i++) {
				characters(ch, 0, ch.length);
			}
		}
	}

	////////////////////////////////////////////////////////////////////
	// Constants.
	////////////////////////////////////////////////////////////////////

	private final static Object SEEN_NOTHING = new Object();
	private final static Object SEEN_ELEMENT = new Object();
	private final static Object SEEN_DATA = new Object();

	////////////////////////////////////////////////////////////////////
	// Internal state.
	////////////////////////////////////////////////////////////////////

	private Object state = SEEN_NOTHING;
	private Stack<Object> stateStack = new Stack<Object>();

	private String indentStep = "  ";
	private int depth = 0;

}
