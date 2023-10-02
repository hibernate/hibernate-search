/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultLocalDateBridge extends AbstractPassThroughDefaultBridge<LocalDate> {

	public static final DefaultLocalDateBridge INSTANCE = new DefaultLocalDateBridge();

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

	private DefaultLocalDateBridge() {
	}

	@Override
	protected String toString(LocalDate value) {
		return FORMATTER.format( value );
	}

	@Override
	protected LocalDate fromString(String value) {
		return ParseUtils.parseLocalDate( value );
	}

}
