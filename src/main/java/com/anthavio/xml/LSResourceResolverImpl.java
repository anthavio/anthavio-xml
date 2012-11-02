package com.anthavio.xml;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;

import org.apache.commons.lang.UnhandledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * @author vanek
 * 
 *         Specialni ResourceResolver pouzivany pokud xml referencuje externi
 *         dokument z divneho zdroje
 * 
 *         Pouzito pro nahravani importovanych xml schemat, pro ktere OC4J
 *         pouzival vlastni nestandardni code-source: pseudoprotokol coz se
 *         muselo specialne nahravat.
 * 
 *         Pro 99% situaci neni tato trida potreba a staci standardni
 *         implementace v parserech.
 */
public class LSResourceResolverImpl implements LSResourceResolver {

	private static Logger log = LoggerFactory.getLogger(LSResourceResolverImpl.class);

	private static final String PATH_BACK = "..";

	public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
		log.debug("resolving type=" + type + ", namespaceURI=" + namespaceURI + ", publicId=" + publicId + ", systemId="
				+ systemId + ", baseURI=" + baseURI);

		String newPath = baseURI.substring(0, baseURI.lastIndexOf("/") + 1);
		URL targetUrl = null;
		try {
			targetUrl = new URL(newPath + systemId);
		} catch (MalformedURLException mue) {
			throw new IllegalArgumentException("Malformed URL " + newPath + systemId);
		}

		if (targetUrl.getPath().indexOf(PATH_BACK) != -1) {
			ArrayList<String> newPathParts = new ArrayList<String>();
			String[] oldPathParts = targetUrl.getPath().split("/");
			for (int i = 0; i < oldPathParts.length; i++) {
				String oldPart = oldPathParts[i];
				if (oldPart.equals(PATH_BACK)) {
					newPathParts.remove(newPathParts.size() - 1);
				} else {
					newPathParts.add(oldPart);
				}
			}

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < newPathParts.size(); ++i) {
				sb.append(newPathParts.get(i));
				if (i + 1 < newPathParts.size()) {
					sb.append("/");
				}
			}

			try {
				targetUrl = new URL(targetUrl.getProtocol(), targetUrl.getHost(), targetUrl.getPort(), sb.toString());
			} catch (MalformedURLException mux) {
				throw new UnhandledException(mux);
			}
		}

		BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(targetUrl.openStream(), "utf-8"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		String line = null;
		StringBuilder sb = new StringBuilder(1024);

		try {
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// ignore
			}
		}

		// System.out.println("resolveResource: type=" + type + " namespaceURI="
		// + namespaceURI + " publicId=" + publicId + " systemId=" + systemId
		// + " baseURI=" + baseURI + " -> ");

		return new LSInputImpl(baseURI, publicId, systemId, null, false, null, null, sb.toString());
	}

	public static void main(String[] args) {

		URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
			public URLStreamHandler createURLStreamHandler(String protocol) {
				return new URLStreamHandler() {
					@Override
					protected URLConnection openConnection(URL u) throws IOException {
						return new URLConnection(u) {
							@Override
							public InputStream getInputStream() throws IOException {
								return new ByteArrayInputStream("bla bla bla".getBytes());
							}

							@Override
							public void connect() throws IOException {

							}
						};
					}
				};
			}

		});
		new LSResourceResolverImpl()
				.resolveResource(
						"http://www.w3.org/2001/XMLSchema",
						"http://www.komix.cz/sis/nssis/types/alert",
						null,
						"NsSisAlertDataTypes.xsd",
						"code-source:/oracle/product/10.1.3/OracleAS_1/j2ee/dev1/applications/sis2/sis2-web/WEB-INF/lib/sis2-common.jar!/schema/nssis/NsSisXtraDataTypes.xsd");
		new LSResourceResolverImpl()
				.resolveResource(
						"http://www.w3.org/2001/XMLSchema",
						"http://www.komix.cz/sis/qsis/types",
						null,
						"../qsis/NsSisQsisTypes.xsd",
						"code-source:/oracle/product/10.1.3/OracleAS_1/j2ee/dev1/applications/sis2/sis2-web/WEB-INF/lib/sis2-common.jar!/schema/nssis/NsSisXtraDataTypes.xsd");
	}
}
