/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * @author Emmanuel Bernard
 */
public class TruncateFieldBridge implements FieldBridge {

	public Object get(String name, Document document) {
		IndexableField field = document.getField( name );
		return field.stringValue();
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		String stringValue = (String) value;
		if ( stringValue != null ) {
			String indexedString = stringValue.substring( 0, stringValue.length() / 2 );
			luceneOptions.addFieldToDocument( name, indexedString, document );
		}
	}

}
