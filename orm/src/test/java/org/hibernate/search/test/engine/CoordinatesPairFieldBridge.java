/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * Assumes values are strings containing integer pairs in the form "12;34"
 * (strongly assumes valid format)
 */
public class CoordinatesPairFieldBridge implements TwoWayFieldBridge {

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		String[] coordinates = value.toString().split( ";" );
		Double x = Double.parseDouble( coordinates[0] );
		Double y = Double.parseDouble( coordinates[1] );
		luceneOptions.addNumericFieldToDocument( getXFieldName( name ), x, document );
		luceneOptions.addNumericFieldToDocument( getYFieldName( name ), y, document );
	}

	@Override
	public Object get(String name, Document document) {
		StringBuilder sb = new StringBuilder( 7 );
		IndexableField xFieldable = document.getField( getXFieldName( name ) );
		IndexableField yFieldable = document.getField( getYFieldName( name ) );
		appendValue( xFieldable, sb );
		sb.append( ';' );
		appendValue( yFieldable, sb );
		return sb.toString();
	}

	private void appendValue(final IndexableField field, final StringBuilder sb) {
		if ( field != null ) {
			sb.append( field.stringValue() );
		}
		else {
			sb.append( '0' );
		}
	}

	@Override
	public String objectToString(Object object) {
		return object.toString();
	}

	private String getYFieldName(final String name) {
		return name + "_y";
	}

	private String getXFieldName(final String name) {
		return name + "_x";
	}

}
