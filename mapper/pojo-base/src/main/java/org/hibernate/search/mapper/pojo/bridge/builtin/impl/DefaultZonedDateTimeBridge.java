/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultZonedDateTimeBridge extends AbstractPassThroughDefaultBridge<ZonedDateTime> {

	public static final DefaultZonedDateTimeBridge INSTANCE = new DefaultZonedDateTimeBridge();

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

	private DefaultZonedDateTimeBridge() {
	}

	@Override
	protected String toString(ZonedDateTime value) {
		return FORMATTER.format( value );
	}

	@Override
	protected ZonedDateTime fromString(String value) {
		return ParseUtils.parseZonedDateTime( value );
	}

}
