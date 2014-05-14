/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.builtin.impl;

import org.apache.lucene.document.Document;

import org.apache.lucene.index.IndexableField;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * @author Hardy Ferentschik
 */
public class NullEncodingTwoWayFieldBridge implements TwoWayFieldBridge {

	private final TwoWayFieldBridge fieldBridge;
	private final String nullMarker;

	public NullEncodingTwoWayFieldBridge(TwoWayFieldBridge fieldBridge, String nullMarker) {
		this.fieldBridge = fieldBridge;
		this.nullMarker = nullMarker;
	}

	@Override
	public Object get(String name, Document document) {
		final IndexableField field = document.getField( name );
		String stringValue = field.stringValue();
		if ( nullMarker.equals( stringValue ) ) {
			return null;
		}
		else {
			return fieldBridge.get( name, document );
		}
	}

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return nullMarker;
		}
		else {
			return fieldBridge.objectToString( object );
		}
	}

	public TwoWayFieldBridge unwrap() {
		return fieldBridge;
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		fieldBridge.set( name, value, document, luceneOptions );
	}
}
