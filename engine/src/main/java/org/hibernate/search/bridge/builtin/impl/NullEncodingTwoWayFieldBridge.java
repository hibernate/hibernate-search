/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.builtin.impl;

import org.apache.lucene.document.Document;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.engine.impl.nullencoding.NullMarkerCodec;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Hardy Ferentschik
 */
public class NullEncodingTwoWayFieldBridge implements TwoWayFieldBridge {

	private static final Log LOG = LoggerFactory.make();

	private final TwoWayFieldBridge fieldBridge;
	private final NullMarkerCodec nullTokenCodec;

	public NullEncodingTwoWayFieldBridge(TwoWayFieldBridge fieldBridge, NullMarkerCodec nullTokenCodec) {
		this.fieldBridge = fieldBridge;
		this.nullTokenCodec = nullTokenCodec;
	}

	@Override
	public Object get(String name, Document document) {
		final IndexableField field = document.getField( name );
		if ( field == null ) {
			//Avoid an NPE if the field isn't defined
			LOG.loadingNonExistentField( name );
			return null;
		}
		if ( nullTokenCodec.representsNullValue( field ) ) {
			return null;
		}
		else {
			return fieldBridge.get( name, document );
		}
	}

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return nullTokenCodec.nullRepresentedAsString();
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
