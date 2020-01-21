/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		long nanos = duration.getNano();
		long millis = nanos / 1_000_000;
		nanos %= 1_000_000;
		return String.format(
				Locale.ROOT, "%ds, %dms and %dns",
				duration.getSeconds(), millis, nanos
		);
	}
}
