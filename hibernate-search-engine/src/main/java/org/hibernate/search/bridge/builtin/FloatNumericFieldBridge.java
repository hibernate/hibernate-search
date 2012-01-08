package org.hibernate.search.bridge.builtin;

import org.apache.lucene.document.Document;

/**
 * Float numeric field bridge, capable of decoding float values from Field
 *
 * @author Gustavo Fernandes
 */
public class FloatNumericFieldBridge extends NumericFieldBridge {

	public Object get(String name, Document document) {
		return Float.valueOf(document.getFieldable(name).stringValue());
	}
}
