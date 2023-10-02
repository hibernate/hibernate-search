/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.logging.impl;

import java.time.Duration;
import java.util.Locale;

public final class DurationInSecondsAndFractionsFormatter {

	private final Duration duration;

	public DurationInSecondsAndFractionsFormatter(Duration duration) {
		this.duration = duration;
	}

	@Override
	public String toString() {
		if ( duration == null ) {
			return "null";
		}
		long nanos = duration.getNano();
		long millis = nanos / 1_000_000;
		nanos %= 1_000_000;
		return String.format(
				Locale.ROOT, "%ds, %dms and %dns",
				duration.getSeconds(), millis, nanos
		);
	}
}
