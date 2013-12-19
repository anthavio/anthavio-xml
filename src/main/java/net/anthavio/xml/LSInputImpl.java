package net.anthavio.xml;

import java.io.InputStream;
import java.io.Reader;

import org.w3c.dom.ls.LSInput;

/**
 * @author vanek
 *
 */
public class LSInputImpl implements LSInput {

	private String baseURI;

	private String publicId;

	private String systemId;

	private InputStream byteStream;

	private boolean certifiedText;

	private Reader characterStream;

	private String encoding;

	private String stringData;

	public LSInputImpl(String baseURI, String publicId, String systemId, InputStream byteStream,
			boolean certifiedText, Reader characterStream, String encoding, String stringData) {
		this.baseURI = baseURI;
		this.publicId = publicId;
		this.systemId = systemId;
		this.byteStream = byteStream;
		this.certifiedText = certifiedText;
		this.characterStream = characterStream;
		this.encoding = encoding;
		this.stringData = stringData;
	}

	public String getBaseURI() {
		return baseURI;
	}

	public InputStream getByteStream() {
		return byteStream;
	}

	public boolean getCertifiedText() {
		return certifiedText;
	}

	public Reader getCharacterStream() {
		return characterStream;
	}

	public String getEncoding() {
		return encoding;
	}

	public String getPublicId() {
		return publicId;
	}

	public String getStringData() {
		return stringData;
	}

	public String getSystemId() {
		return systemId;
	}

	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
	}

	public void setByteStream(InputStream byteStream) {
		this.byteStream = byteStream;
	}

	public void setCertifiedText(boolean certifiedText) {
		this.certifiedText = certifiedText;
	}

	public void setCharacterStream(Reader characterStream) {
		this.characterStream = characterStream;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public void setPublicId(String publicId) {
		this.publicId = publicId;
	}

	public void setStringData(String stringData) {
		this.stringData = stringData;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

}
