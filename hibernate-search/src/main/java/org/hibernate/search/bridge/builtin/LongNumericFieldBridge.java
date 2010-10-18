package org.hibernate.search.bridge.builtin;

import org.apache.lucene.document.Document;

/**
 * Long numeric field bridge, capable of decoding long values from Field
 *
 * @author: Gustavo Fernandes
 */
public class LongNumericFieldBridge extends NumericFieldBridge {

	public Object get(String name, Document document) {
		return Long.valueOf(document.getFieldable(name).stringValue());
	}
}
