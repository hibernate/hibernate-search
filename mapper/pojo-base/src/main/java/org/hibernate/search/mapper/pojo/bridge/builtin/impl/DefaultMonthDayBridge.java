/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.MonthDay;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultMonthDayBridge extends AbstractPassThroughDefaultBridge<MonthDay> {

	// The DateTimeFormatter class does not expose a public constant for the ISO format, so we need to do it ourselves.
	private static final DateTimeFormatter FORMATTER = ParseUtils.ISO_MONTH_DAY;

	@Override
	protected String toString(MonthDay value) {
		return FORMATTER.format( value );
	}

	@Override
	protected MonthDay fromString(String value) {
		return ParseUtils.parseMonthDay( value );
	}

}
