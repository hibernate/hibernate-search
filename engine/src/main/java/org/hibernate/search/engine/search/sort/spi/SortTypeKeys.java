/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.spi;

import org.hibernate.search.engine.backend.types.IndexFieldTraits;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public final class SortTypeKeys {

	private SortTypeKeys() {
	}

	public static final SearchQueryElementTypeKey<FieldSortBuilder> FIELD =
			SearchQueryElementTypeKey.of( IndexFieldTraits.Sorts.FIELD );
	public static final SearchQueryElementTypeKey<DistanceSortBuilder> DISTANCE =
			SearchQueryElementTypeKey.of( IndexFieldTraits.Sorts.DISTANCE );

}
