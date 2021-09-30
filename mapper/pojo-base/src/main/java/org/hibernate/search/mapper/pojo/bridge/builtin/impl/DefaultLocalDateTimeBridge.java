/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultLocalDateTimeBridge extends AbstractPassThroughDefaultBridge<LocalDateTime> {

	public static final DefaultLocalDateTimeBridge INSTANCE = new DefaultLocalDateTimeBridge();

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	private DefaultLocalDateTimeBridge() {
	}

	@Override
	protected String toString(LocalDateTime value) {
		return FORMATTER.format( value );
	}

	@Override
	protected LocalDateTime fromString(String value) {
		return ParseUtils.parseLocalDateTime( value );
	}

}
