/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultOffsetTimeBridge extends AbstractPassThroughDefaultBridge<OffsetTime> {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_TIME;

	public static final DefaultOffsetTimeBridge INSTANCE = new DefaultOffsetTimeBridge();

	private DefaultOffsetTimeBridge() {
	}

	@Override
	protected String toString(OffsetTime value) {
		return FORMATTER.format( value );
	}

	@Override
	protected OffsetTime fromString(String value) {
		return ParseUtils.parseOffsetTime( value );
	}

}
