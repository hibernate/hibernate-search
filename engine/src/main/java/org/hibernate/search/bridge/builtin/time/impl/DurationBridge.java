/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.Duration;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Converts a {@link Duration} to a {@link Long} expressing the duration in nanoseconds.
 * <p>
 * If the duration cannot be expressed using a long, a {@link org.hibernate.search.exception.SearchException} get thrown.
 *
 * @author Davide D'Alto
 */
public class DurationBridge implements TwoWayFieldBridge, NumericTimeBridge {

	private static final Log log = LoggerFactory.make();

	public static final DurationBridge INSTANCE = new DurationBridge();

	private DurationBridge() {
	}

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return null;
		}

		return String.valueOf( toNanos( (Duration) object ) );
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value != null ) {
			Long nanos = toNanos( (Duration) value );
			luceneOptions.addNumericFieldToDocument( name, nanos, document );
		}
	}

	private Long toNanos(Duration value) {
		try {
			Long nanos = value.toNanos();
			return nanos;
		}
		catch (ArithmeticException ae) {
			throw log.valueTooLargeForConversionException( Duration.class, value, ae );
		}
	}

	@Override
	public Object get(String name, Document document) {
		String nanosFromIndex = document.get( name );
		Long nanos = Long.valueOf( nanosFromIndex );
		return Duration.ofNanos( nanos );
	}

	@Override
	public NumericEncodingType getEncodingType() {
		return NumericEncodingType.LONG;
	}
}
