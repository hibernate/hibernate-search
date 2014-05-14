/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.builtin.impl;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;

/**
 * @author Davide D'Alto
 */
public class NullEncodingFieldBridge implements FieldBridge, StringBridge {

	private final String2FieldBridgeAdaptor bridge;
	private final String nullMarker;

	public NullEncodingFieldBridge(StringBridge bridge, String nullMarker) {
		this.bridge = new String2FieldBridgeAdaptor( bridge );
		this.nullMarker = nullMarker;
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value == null ) {
			luceneOptions.addFieldToDocument( name, nullMarker, document );
		}
		else {
			bridge.set( name, value, document, luceneOptions );
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.hibernate.search.bridge.StringBridge#objectToString(java.lang.Object)
	 */
	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return nullMarker;
		}
		return bridge.objectToString( object );
	}

}
