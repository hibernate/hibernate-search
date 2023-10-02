/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
