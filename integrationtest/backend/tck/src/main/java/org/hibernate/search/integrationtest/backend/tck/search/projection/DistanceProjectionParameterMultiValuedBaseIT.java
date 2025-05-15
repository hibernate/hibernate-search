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
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

@TestForIssue(jiraKey = "HSEARCH-3391")
class DistanceProjectionParameterMultiValuedBaseIT extends AbstractDistanceProjectionMultiValuedBaseIT {

	@Override
	protected void addParameter(SearchQueryOptionsStep<?, ?, ?, ?, ?, ?> query, String parameterName, Object value) {
		query.param( parameterName, value );
	}

	@Override
	protected ProjectionFinalStep<List<Double>> distance(
			TypedSearchProjectionFactory<?, EntityReference, DocumentReference> projection, String path, GeoPoint center,
			String parameterName) {
		return projection.withParameters(
				params -> projection.distance( path, params.get( parameterName, GeoPoint.class ) )
						.collector( ProjectionCollector.list() ) );
	}

	@Override
	protected ProjectionFinalStep<List<Double>> distance(
			TypedSearchProjectionFactory<?, EntityReference, DocumentReference> projection, String path, GeoPoint center,
			DistanceUnit unit, String centerParam, String unitParam) {
		return projection.withParameters(
				params -> projection.distance( path, params.get( centerParam, GeoPoint.class ) )
						.collector( ProjectionCollector.list() )
						.unit( params.get( unitParam, DistanceUnit.class ) ) );
	}

	@Override
	protected SortFinalStep sort(TypedSearchSortFactory<?> sort, String path, GeoPoint center, String parameterName) {
		return sort.withParameters( param -> sort.distance( path, param.get( parameterName, GeoPoint.class ) ) );
	}
}
