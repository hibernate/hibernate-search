//$Id$
package org.hibernate.search.bridge;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.util.StringHelper;

/**
 * Bridge to use a StringBridge as a FieldBridge.
 *
 * @author Emmanuel Bernard
 */
public class String2FieldBridgeAdaptor implements FieldBridge {
	private final StringBridge stringBridge;

	public String2FieldBridgeAdaptor(StringBridge stringBridge) {
		this.stringBridge = stringBridge;
	}

	public void set(String name, Object value, Document document, LuceneOptions parameterObject) {
		String indexedString = stringBridge.objectToString( value );
		//Do not add fields on empty strings, seems a sensible default in most situations
		//TODO if Store, probably also save empty ones
		if ( StringHelper.isNotEmpty( indexedString ) ) {
			Field field = new Field( name, indexedString, parameterObject.store, parameterObject.index, parameterObject.termVector );
			if ( parameterObject.boost != null ) field.setBoost( parameterObject.boost );
			document.add( field );
		}
	}

}
