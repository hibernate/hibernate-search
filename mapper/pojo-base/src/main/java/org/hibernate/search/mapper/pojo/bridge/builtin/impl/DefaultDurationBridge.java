/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.lang.invoke.MethodHandles;
import java.time.Duration;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class DefaultDurationBridge extends AbstractConvertingDefaultBridge<Duration, Long> {

	public static final DefaultDurationBridge INSTANCE = new DefaultDurationBridge();

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private DefaultDurationBridge() {
	}

	@Override
	protected String toString(Duration value) {
		return value.toString();
	}

	@Override
	protected Duration fromString(String value) {
		return ParseUtils.parseDuration( value );
	}

	@Override
	protected Long toConvertedValue(Duration value) {
		try {
			return value.toNanos();
		}
		catch (ArithmeticException ae) {
			throw log.valueTooLargeForConversionException( Long.class, value, ae );
		}
	}

	@Override
	protected Duration fromConvertedValue(Long value) {
		return Duration.ofNanos( value );
	}
}
