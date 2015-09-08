/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.Instant;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Store a {@link Instant} in a numeric field representing it as the number of milliseconds form Epoch.
 * Note that the instant is truncated to the milliseconds.
 * <p>
 * If the instant cannot be expressed using a long, a {@link org.hibernate.search.exception.SearchException} get thrown.
 *
 * @see Instant#toEpochMilli()
 * @author Davide D'Alto
 */
public class InstantBridge implements TwoWayFieldBridge, NumericTimeBridge {

	private static final Log log = LoggerFactory.make();

	public static final InstantBridge INSTANCE = new InstantBridge();

	@Override
	public NumericEncodingType getEncodingType() {
		return NumericEncodingType.LONG;
	}

	@Override
	public Object get(String name, Document document) {
		String millisFromEpoch = document.get( name );
		return Instant.ofEpochMilli( Long.valueOf( millisFromEpoch ) );
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value != null ) {
			Long nanos = toEpochMillis( (Instant) value );
			luceneOptions.addNumericFieldToDocument( name, nanos, document );
		}
	}

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return null;
		}
		Instant instant = (Instant) object;
		return String.valueOf( toEpochMillis( instant ) );
	}

	private Long toEpochMillis(Instant value) {
		try {
			Long millis = value.toEpochMilli();
			return millis;
		}
		catch (ArithmeticException ae) {
			throw log.valueTooLargeForConversionException( Instant.class, value, ae );
		}
	}
}
