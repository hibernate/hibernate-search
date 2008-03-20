//$Id$
package org.hibernate.search.test.bridge;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.util.StringHelper;

/**
 * @author Emmanuel Bernard
 */
public class TruncateFieldBridge implements FieldBridge {
	public Object get(String name, Document document) {
		Field field = document.getField( name );
		return field.stringValue();
	}

	public void set(String name, Object value, Document document, Field.Store store, Field.Index index, Field.TermVector termVector, Float boost) {
		String indexedString = (String) value;
		//Do not add fields on empty strings, seems a sensible default in most situations
		if ( StringHelper.isNotEmpty( indexedString ) ) {
			Field field = new Field( name, indexedString.substring( 0, indexedString.length() / 2 ), store, index, termVector );
			if ( boost != null ) field.setBoost( boost );
			document.add( field );
		}
	}
}
