/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.impl;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.engine.impl.DocumentBuilderHelper;

/**
 * Bridge to use a TwoWayStringBridge as a TwoWayFieldBridge
 *
 * @author Emmanuel Bernard
 */
//TODO use Generics to avoid double declaration of stringBridge
public class TwoWayString2FieldBridgeAdaptor extends String2FieldBridgeAdaptor implements TwoWayFieldBridge {

	private final TwoWayStringBridge stringBridge;

	public TwoWayString2FieldBridgeAdaptor(TwoWayStringBridge stringBridge) {
		super( stringBridge );
		this.stringBridge = stringBridge;
	}

	@Override
	public String objectToString(Object object) {
		return stringBridge.objectToString( object );
	}

	@Override
	public Object get(String name, Document document) {
		final IndexableField field = document.getField( name );
		if ( field == null ) {
			return stringBridge.stringToObject( null );
		}
		else {
			String stringValue = DocumentBuilderHelper.extractStringFromFieldable( field );
			return stringBridge.stringToObject( stringValue );
		}
	}

	public TwoWayStringBridge unwrap() {
		return stringBridge;
	}

	@Override
	public String toString() {
		return "TwoWayString2FieldBridgeAdaptor [stringBridge=" + stringBridge + "]";
	}
}
