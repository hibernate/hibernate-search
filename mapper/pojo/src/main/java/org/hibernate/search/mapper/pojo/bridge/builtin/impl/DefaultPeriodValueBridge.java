/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Period;
import java.util.Locale;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class DefaultPeriodValueBridge implements ValueBridge<Period, String> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final int PADDING = 11;
	private static final String FORMAT = "%+0" + PADDING + "d%+0" + PADDING + "d%+0" + PADDING + "d";

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public String toIndexedValue(Period value, ValueBridgeToIndexedValueContext context) {
		return toIndexedValue( value );
	}

	@Override
	public Period fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
		if ( value == null ) {
			return null;
		}

		try {
			int years = Integer.parseInt( value.substring( 0, PADDING ) );
			int months = Integer.parseInt( value.substring( PADDING, 2 * PADDING ) );
			int days = Integer.parseInt( value.substring( 2 * PADDING ) );

			return Period.of( years, months, days );
		}
		catch (NumberFormatException e) {
			throw log.parseException( value, Duration.class, e );
		}
	}

	@Override
	public Period cast(Object value) {
		return (Period) value;
	}

	@Override
	public String parse(String value) {
		return toIndexedValue( ParseUtils.parsePeriod( value ) );
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

	private String toIndexedValue(Period value) {
		if ( value == null ) {
			return null;
		}
		return String.format( Locale.ROOT, FORMAT, value.getYears(), value.getMonths(), value.getDays() );
	}

}
