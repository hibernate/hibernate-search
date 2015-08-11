/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * A bridge that converts a temporal type (a class in the package java.time.*) to a number.
 *
 * @param T the temporal type
 * @param N the type of the number resulting from the conversion
 *
 * @author Davide D'Alto
 */
public abstract class JavaTimeNumericBridge<T, N extends Number> implements FieldBridge, TwoWayFieldBridge {

	/**
	 * Converts an instance of T to an instance of N.
	 *
	 * @param temporal an instance of T. It's never null
	 * @return an instance of N representing the temporal
	 */
	public abstract N encode(T temporal);

	/**
	 * Converts an instance of N to an instance of T.
	 *
	 * @param number an instance of N. It's never null
	 * @return an instance of T representing the number
	 */
	public abstract T decode(N number);

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value != null ) {
			@SuppressWarnings("unchecked")
			N encoded = encode( (T) value );
			luceneOptions.addNumericFieldToDocument( name, encoded, document );
		}
	}

	@Override
	public final String objectToString(final Object object) {
		@SuppressWarnings("unchecked")
		N encoded = encode( (T) object );
		return object == null ? null : String.valueOf( encoded );
	}

	@Override
	public Object get(final String name, final Document document) {
		final IndexableField field = document.getField( name );
		if ( field == null ) {
			return null;
		}

		@SuppressWarnings("unchecked")
		N numericValue = (N) field.numericValue();
		return decode( numericValue );
	}
}
