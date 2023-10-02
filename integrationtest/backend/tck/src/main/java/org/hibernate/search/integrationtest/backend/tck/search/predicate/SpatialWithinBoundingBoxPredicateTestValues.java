/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.List;

import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;

public final class SpatialWithinBoundingBoxPredicateTestValues
		extends AbstractPredicateTestValues<GeoPoint> {
	private final List<GeoPoint> values;

	public SpatialWithinBoundingBoxPredicateTestValues() {
		super( GeoPointFieldTypeDescriptor.INSTANCE );
		this.values = GeoPointFieldTypeDescriptor.INSTANCE.getUniquelyMatchableValues();
	}

	@Override
	public GeoPoint fieldValue(int docOrdinal) {
		return values.get( docOrdinal );
	}

	public GeoBoundingBox matchingArg(int docOrdinal) {
		GeoPoint pointToMatch = fieldValue( docOrdinal );
		return GeoBoundingBox.of( pointToMatch.latitude() + 0.001, pointToMatch.longitude() - 0.001,
				pointToMatch.latitude() - 0.001, pointToMatch.longitude() + 0.001 );
	}

	@Override
	public int size() {
		return values.size();
	}
}
