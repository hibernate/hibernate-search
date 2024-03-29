/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultLocalTimeBridge extends AbstractPassThroughDefaultBridge<LocalTime> {

	public static final DefaultLocalTimeBridge INSTANCE = new DefaultLocalTimeBridge();

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

	private DefaultLocalTimeBridge() {
	}

	@Override
	protected String toString(LocalTime value) {
		return FORMATTER.format( value );
	}

	@Override
	protected LocalTime fromString(String value) {
		return ParseUtils.parseLocalTime( value );
	}

}
