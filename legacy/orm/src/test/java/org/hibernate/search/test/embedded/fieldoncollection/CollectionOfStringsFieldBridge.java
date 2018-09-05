/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.fieldoncollection;

import java.util.Collection;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;

public class CollectionOfStringsFieldBridge implements FieldBridge, StringBridge {

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return null;
		}
		if ( !( object instanceof String ) ) {
			throw new IllegalArgumentException( "This FieldBridge only supports String objects." );
		}
		return object.toString();
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value == null ) {
			return;
		}
		if ( !( value instanceof Collection ) ) {
			throw new IllegalArgumentException( "This FieldBridge only supports collections." );
		}
		Collection<?> objects = (Collection<?>) value;

		for ( Object object : objects ) {
			luceneOptions.addFieldToDocument( name, objectToString( object ), document );
		}
	}
}
