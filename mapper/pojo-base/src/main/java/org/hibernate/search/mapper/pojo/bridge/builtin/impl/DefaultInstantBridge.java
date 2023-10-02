/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultInstantBridge extends AbstractPassThroughDefaultBridge<Instant> {

	public static final DefaultInstantBridge INSTANCE = new DefaultInstantBridge();

	private DefaultInstantBridge() {
	}

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
