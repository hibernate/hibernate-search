/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.bridge.builtin.time.impl;

import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.bridge.spi.IgnoreAnalyzerBridge;
import org.hibernate.search.bridge.util.impl.EncodingStringBridge;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Base class for the conversion of {@link TemporalAccessor} to {@link String}.
 *
 * @author Davide D'Alto
 * @author Yoann Rodiere
 */
public abstract class ElasticsearchTemporalAccessorStringBridge<T extends TemporalAccessor>
		extends EncodingStringBridge<T>
		implements TwoWayStringBridge, IgnoreAnalyzerBridge {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final DateTimeFormatter formatter;

	private final Class<T> type;

	private final DateTimeFormatter indexNullAsFormatter;

	ElasticsearchTemporalAccessorStringBridge(DateTimeFormatter formatter, Class<T> type) {
		this( formatter, type, formatter );
	}


	ElasticsearchTemporalAccessorStringBridge(DateTimeFormatter formatter, Class<T> type, DateTimeFormatter indexNullAsFormatter) {
		this.formatter = formatter;
		this.type = type;
		this.indexNullAsFormatter = indexNullAsFormatter;
	}

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return null;
		}
		@SuppressWarnings("unchecked")
		String formatted = format( formatter, (T) object );
		return formatted;
	}

	@Override
	public Object stringToObject(String stringValue) {
		if ( stringValue == null ) {
			return null;
		}

		try {
			return parse( formatter, stringValue );
		}
		catch (Exception e) {
			throw LOG.parseException( stringValue, type, e );
		}
	}

	String format(DateTimeFormatter formatter, T object) {
		return formatter.format( object );
	}

	abstract T parse(DateTimeFormatter formatter, String stringValue) throws DateTimeException;

	@Override
	public NumericEncodingType getEncodingType() {
		return NumericEncodingType.UNKNOWN;
	}

	@Override
	protected T parseIndexNullAs(String indexNullAs) throws IllegalArgumentException {
		try {
			return parse( indexNullAsFormatter, indexNullAs );
		}
		catch (DateTimeException e) {
			throw createInvalidIndexNullAsException( indexNullAs, e );
		}
	}

	protected abstract IllegalArgumentException createInvalidIndexNullAsException(String indexNullAs, DateTimeException e);
}
