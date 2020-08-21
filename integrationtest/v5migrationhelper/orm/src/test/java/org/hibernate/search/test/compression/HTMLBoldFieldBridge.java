/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.compression;

import java.util.zip.DataFormatException;

import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.Document;

import org.apache.lucene.index.IndexableField;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * This FieldBridge is storing strings in the index wrapping the entity value in html bold tags.
 *
 * @author Sanne Grinovero
 * @see LuceneOptions
 */
public class HTMLBoldFieldBridge implements FieldBridge, TwoWayFieldBridge {

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		String fieldValue = objectToString( value );
		luceneOptions.addFieldToDocument( name, fieldValue, document );
	}

	@Override
	public Object get(String name, Document document) {
		IndexableField field = document.getField( name );
			String stringValue;
			if ( field.binaryValue() != null ) {
				try {
					stringValue = CompressionTools.decompressString( field.binaryValue() );
				}
			catch (DataFormatException e) {
					throw new SearchException( "Field " + name + " looks like binary but couldn't be decompressed" );
				}
			}
			else {
				stringValue = field.stringValue();
			}
			return stringValue.substring( 3, stringValue.length() - 4 );
	}

	@Override
	public String objectToString(Object value) {
		String originalValue = value.toString();
		return "<b>" + originalValue + "</b>";
	}
}
