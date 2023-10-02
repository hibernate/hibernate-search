/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import java.util.List;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

class DistanceProjectionParameterSingleValuedBaseIT extends AbstractDistanceProjectionSingleValuedBaseIT {


	@Override
	protected void addParameter(SearchQueryOptionsStep<?, ?, ?, ?, ?> query, String parameterName, Object value) {
		query.param( parameterName, value );
	}

	@Override
	protected ProjectionFinalStep<Double> distance(
			SearchProjectionFactory<EntityReference, DocumentReference> projection, String path, GeoPoint center,
			String parameterName) {
		return projection.withParameters(
				params -> projection.distance( path, params.get( parameterName, GeoPoint.class ) ) );
	}

	@Override
	protected ProjectionFinalStep<List<Double>> distanceMulti(
			SearchProjectionFactory<EntityReference, DocumentReference> projection, String path, GeoPoint center,
			String parameterName) {
		return projection.withParameters(
				params -> projection.distance( path, params.get( parameterName, GeoPoint.class ) ).multi() );
	}

	@Override
	protected ProjectionFinalStep<Double> distance(
			SearchProjectionFactory<EntityReference, DocumentReference> projection, String path, GeoPoint center,
			DistanceUnit unit, String centerParam, String unitParam) {
		return projection.withParameters(
				params -> projection.distance( path, params.get( centerParam, GeoPoint.class ) )
						.unit( params.get( unitParam, DistanceUnit.class ) ) );
	}

	@Override
	protected SortFinalStep sort(SearchSortFactory sort, String path, GeoPoint center, String parameterName) {
		return sort.withParameters( param -> sort.distance( path, param.get( parameterName, GeoPoint.class ) ) );
	}
}
