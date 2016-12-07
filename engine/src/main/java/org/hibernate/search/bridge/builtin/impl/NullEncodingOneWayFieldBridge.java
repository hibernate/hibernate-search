/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.builtin.impl;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.builtin.DefaultStringBridge;
import org.hibernate.search.bridge.spi.NullMarker;
import org.hibernate.search.bridge.util.impl.BridgeAdaptor;
import org.hibernate.search.bridge.util.impl.BridgeAdaptorUtils;
import org.hibernate.search.bridge.util.impl.String2FieldBridgeAdaptor;
import org.hibernate.search.engine.nulls.codec.impl.NullMarkerCodec;

/**
 * @author Yoann Rodiere
 */
public class NullEncodingOneWayFieldBridge implements FieldBridge, StringBridge, BridgeAdaptor {

	private static final String2FieldBridgeAdaptor DEFAULT_STRING_BRIDGE = new String2FieldBridgeAdaptor( DefaultStringBridge.INSTANCE );

	private final FieldBridge fieldBridge;
	private final StringBridge stringBridge;
	private final NullMarkerCodec nullTokenCodec;

	public NullEncodingOneWayFieldBridge(FieldBridge fieldBridge, NullMarkerCodec nullTokenCodec) {
		this.fieldBridge = fieldBridge;
		this.nullTokenCodec = nullTokenCodec;
		this.stringBridge = fieldBridge instanceof StringBridge ? (StringBridge) fieldBridge : DEFAULT_STRING_BRIDGE;
	}

	@Override
	public <T> T unwrap(Class<T> bridgeClass) {
		return BridgeAdaptorUtils.unwrapAdaptorOnly( fieldBridge, bridgeClass );
	}

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			NullMarker marker = nullTokenCodec.getNullMarker();
			return marker == null ? null : marker.nullRepresentedAsString();
		}
		else {
			return stringBridge.objectToString( object );
		}
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value == null ) {
			nullTokenCodec.encodeNullValue( name, document, luceneOptions );
		}
		else {
			fieldBridge.set( name, value, document, luceneOptions );
		}
	}

	public Query buildNullQuery(String fieldName) {
		return nullTokenCodec.createNullMatchingQuery( fieldName );
	}

}
