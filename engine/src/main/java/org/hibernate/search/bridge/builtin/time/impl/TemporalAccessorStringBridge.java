/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Base class for the conversion of {@link TemporalAccessor} to {@link String}.
 *
 * @author Davide D'Alto
 */
public abstract class TemporalAccessorStringBridge<T extends TemporalAccessor> implements TwoWayStringBridge {

	private static final Log log = LoggerFactory.make();

	private final DateTimeFormatter formatter;

	private final Class<T> type;

	public TemporalAccessorStringBridge(DateTimeFormatter formatter, Class<T> type) {
		this.formatter = formatter;
		this.type = type;
	}

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return null;
		}
		@SuppressWarnings("unchecked")
		String formatted = formatter.format( (T) object );
		return formatted;
	}

	@Override
	public Object stringToObject(String stringValue) {
		if ( stringValue == null ) {
			return null;
		}

		try {
			return parse( stringValue );
		}
		catch (Exception e) {
			throw log.parseException( stringValue, type, e );
		}
	}

	abstract T parse(String stringValue) throws Exception;
}
