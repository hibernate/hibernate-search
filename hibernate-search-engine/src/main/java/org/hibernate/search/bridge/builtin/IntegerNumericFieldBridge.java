package org.hibernate.search.bridge.builtin;

import org.apache.lucene.document.Document;

/**
 * Integer numeric field bridge, capable of decoding integer values from Field
 *
 * @author Gustavo Fernandes
 */
public class IntegerNumericFieldBridge extends NumericFieldBridge {

	public Object get(String name, Document document) {
		return Integer.valueOf(document.getFieldable(name).stringValue());
	}
}
