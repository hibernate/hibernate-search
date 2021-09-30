/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultOffsetDateTimeBridge extends AbstractPassThroughDefaultBridge<OffsetDateTime> {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	public static final DefaultOffsetDateTimeBridge INSTANCE = new DefaultOffsetDateTimeBridge();

	private DefaultOffsetDateTimeBridge() {
	}

	@Override
	protected String toString(OffsetDateTime value) {
		return FORMATTER.format( value );
	}

	@Override
	protected OffsetDateTime fromString(String value) {
		return ParseUtils.parseOffsetDateTime( value );
	}

}
