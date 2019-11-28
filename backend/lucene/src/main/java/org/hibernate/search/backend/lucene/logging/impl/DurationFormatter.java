/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.logging.impl;

import java.time.Duration;
import java.util.Locale;

public class DurationFormatter {

	private final String stringRepresentation;

	public DurationFormatter(Duration duration) {
		this.stringRepresentation = String.format( Locale.ROOT, "%d seconds and %d nanoseconds", duration.getSeconds(), duration.getNano() );
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}
}
