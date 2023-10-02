/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.lang.invoke.MethodHandles;
import java.time.Period;
import java.util.Locale;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class DefaultPeriodBridge extends AbstractConvertingDefaultBridge<Period, String> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final int PADDING = 11;
	private static final String INDEXED_FORMAT = "%+0" + PADDING + "d%+0" + PADDING + "d%+0" + PADDING + "d";

	public static final DefaultPeriodBridge INSTANCE = new DefaultPeriodBridge();

	private DefaultPeriodBridge() {
	}

	@Override
	protected String toString(Period value) {
		return value.toString();
	}

	@Override
	protected Period fromString(String value) {
		return ParseUtils.parsePeriod( value );
	}

	@Override
	protected String toConvertedValue(Period value) {
		return String.format( Locale.ROOT, INDEXED_FORMAT, value.getYears(), value.getMonths(), value.getDays() );
	}

	@Override
	protected Period fromConvertedValue(String value) {
		try {
			int years = Integer.parseInt( value.substring( 0, PADDING ) );
			int months = Integer.parseInt( value.substring( PADDING, 2 * PADDING ) );
			int days = Integer.parseInt( value.substring( 2 * PADDING ) );

			return Period.of( years, months, days );
		}
		catch (NumberFormatException e) {
			throw log.parseException( value, Period.class, e.getMessage(), e );
		}
	}

}
