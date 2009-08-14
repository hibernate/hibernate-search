package org.hibernate.search.util;

import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A utility class to help with xml parsing
 *
 * @author Lukasz Moren
 */
public class XMLHelper {


	/**
	 * Converts a String representing an XML snippet into an {@link org.w3c.dom.Element}.
	 *
	 * @param xml snippet as a string
	 *
	 * @return a DOM Element
	 *
	 * @throws Exception if unable to parse the String or if it doesn't contain valid XML.
	 */
	public static Element elementFromString(String xml) throws Exception {
		ByteArrayInputStream bais = new ByteArrayInputStream( xml.getBytes( "UTF-8" ) );
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse( bais );
		bais.close();
		return document.getDocumentElement();
	}
}
