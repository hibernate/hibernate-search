/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultInstantBridge extends AbstractPassThroughDefaultBridge<Instant> {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

	@Override
	protected String toString(Instant value) {
		return FORMATTER.format( value );
	}

	@Override
	protected Instant fromString(String value) {
		return ParseUtils.parseInstant( value );
	}

}
