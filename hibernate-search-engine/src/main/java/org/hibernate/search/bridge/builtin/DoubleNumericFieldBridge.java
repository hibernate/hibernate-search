package org.hibernate.search.bridge.builtin;

import org.apache.lucene.document.Document;

/**
 * Double numeric field bridge, capable of decoding double values from Field
 *
 * @author Gustavo Fernandes
 */
public class DoubleNumericFieldBridge extends NumericFieldBridge {

	public Object get(String name, Document document) {
		return Double.valueOf(document.getFieldable(name).stringValue());
	}
}
