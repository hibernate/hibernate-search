/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.logging.spi;

import org.hibernate.search.engine.search.aggregation.AggregationKey;

public final class AggregationKeyFormatter {

	private final AggregationKey<?> key;

	public AggregationKeyFormatter(AggregationKey<?> key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return key != null ? key.name() : "null";
	}
}
