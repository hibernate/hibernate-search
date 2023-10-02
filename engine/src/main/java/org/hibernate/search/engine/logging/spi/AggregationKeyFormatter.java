/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
