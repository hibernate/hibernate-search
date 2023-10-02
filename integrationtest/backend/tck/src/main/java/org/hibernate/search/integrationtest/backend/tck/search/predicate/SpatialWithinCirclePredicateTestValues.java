/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.List;

import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;

public final class SpatialWithinCirclePredicateTestValues
		extends AbstractPredicateTestValues<GeoPoint> {
	private final List<GeoPoint> values;

	public SpatialWithinCirclePredicateTestValues() {
		super( GeoPointFieldTypeDescriptor.INSTANCE );
		this.values = GeoPointFieldTypeDescriptor.INSTANCE.getUniquelyMatchableValues();
	}

	@Override
	public GeoPoint fieldValue(int docOrdinal) {
		return values.get( docOrdinal );
	}

	public GeoPoint matchingCenter(int docOrdinal) {
		return fieldValue( docOrdinal );
	}

	public double matchingRadius(int docOrdinal) {
		return 10.0;
	}

	@Override
	public int size() {
		return values.size();
	}
}
