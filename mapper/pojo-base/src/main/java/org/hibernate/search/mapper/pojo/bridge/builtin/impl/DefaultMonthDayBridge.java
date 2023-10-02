/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.MonthDay;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultMonthDayBridge extends AbstractPassThroughDefaultBridge<MonthDay> {

	public static final DefaultMonthDayBridge INSTANCE = new DefaultMonthDayBridge();

	// The DateTimeFormatter class does not expose a public constant for the ISO format, so we need to do it ourselves.
	private static final DateTimeFormatter FORMATTER = ParseUtils.ISO_MONTH_DAY;

	private DefaultMonthDayBridge() {
	}

	@Override
	protected String toString(MonthDay value) {
		return FORMATTER.format( value );
	}

	@Override
	protected MonthDay fromString(String value) {
		return ParseUtils.parseMonthDay( value );
	}

}
