/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.backend.lucene;

import org.apache.lucene.search.TotalHits;

public final class TotalHitsUtils {
	private TotalHitsUtils() {
	}

	public static long value(TotalHits totalHits) {
		return totalHits.value;
	}

	public static TotalHits.Relation relation(TotalHits totalHits) {
		return totalHits.relation;
	}
}
