/**
 * 
 */
package net.anthavio.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.transform.stream.StreamSource;

import net.anthavio.io.ResetableInputStream;
import net.anthavio.io.ResetableReader;

/**
 * @author vanek
 * 
 */
public class ResetableStreamSource extends StreamSource {

	public ResetableStreamSource(StreamSource streamSource, int size) {
		setPublicId(streamSource.getPublicId());
		setSystemId(streamSource.getSystemId());
		if (streamSource.getInputStream() != null) {
			setInputStream(new ResetableInputStream(streamSource.getInputStream(), size));
		} else if (streamSource.getReader() != null) {
			setReader(new ResetableReader(streamSource.getReader(), size));
		} else {
			throw new IllegalArgumentException("StreamSource contains neither InputStream nor Reader");
		}
	}

	public ResetableStreamSource(InputStream inputStream, int size) {
		setInputStream(new ResetableInputStream(inputStream, size));
	}

	public ResetableStreamSource(Reader reader, int size) {
		setReader(new ResetableReader(reader, size));
	}

	public void reset() throws IOException {
		if (getInputStream() != null) {
			((ResetableInputStream) getInputStream()).reset();
		} else if (getReader() != null) {
			((ResetableReader) getReader()).reset();
		} else {
			throw new IllegalArgumentException("StreamSource contains neither InputStream nor Reader");
		}
	}
}
