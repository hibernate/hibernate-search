/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.lang.invoke.MethodHandles;
import java.time.Duration;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class DefaultDurationBridge extends AbstractConvertingDefaultBridge<Duration, Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
