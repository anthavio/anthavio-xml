/**
 * 
 */
package net.anthavio.xml;

/**
 * @author vanek
 *
 */
public class XmlParseException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public XmlParseException(String message, Throwable cause) {
		super(message, cause);
	}

	public XmlParseException(String message) {
		super(message);
	}

	public XmlParseException(Throwable cause) {
		super(cause);
	}

}
