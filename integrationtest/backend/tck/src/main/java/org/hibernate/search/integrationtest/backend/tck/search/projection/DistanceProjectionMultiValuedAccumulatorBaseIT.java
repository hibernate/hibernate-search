/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import java.util.List;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.TypedSearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.search.sort.dsl.TypedSearchSortFactory;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

class DistanceProjectionMultiValuedAccumulatorBaseIT extends AbstractDistanceProjectionMultiValuedBaseIT {

	@Override
	protected void addParameter(SearchQueryOptionsStep<?, ?, ?, ?, ?, ?> query, String parameterName, Object value) {
		// do nothing
	}

	@Override
	protected ProjectionFinalStep<List<Double>> distance(
			TypedSearchProjectionFactory<?, EntityReference, DocumentReference> projection, String path, GeoPoint center,
			String parameterName) {
		return projection.distance( path, center ).collector( ProjectionCollector.list() );
	}

	@Override
	protected ProjectionFinalStep<List<Double>> distance(
			TypedSearchProjectionFactory<?, EntityReference, DocumentReference> projection, String path, GeoPoint center,
			DistanceUnit unit, String centerParam, String unitParam) {
		return projection.distance( path, center ).collector( ProjectionCollector.list() ).unit( unit );
	}

	@Override
	protected SortFinalStep sort(TypedSearchSortFactory<?> sort, String path, GeoPoint center,
			String parameterName) {
		return sort.distance( path, center );
	}
}
