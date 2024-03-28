/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import java.util.List;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

class DistanceProjectionSingleValuedBaseIT extends AbstractDistanceProjectionSingleValuedBaseIT {


	@Override
	protected void addParameter(SearchQueryOptionsStep<?, ?, ?, ?, ?> query, String parameterName, Object value) {
		// do nothing
	}

	@Override
	protected DistanceToFieldProjectionValueStep<?, Double> distance(
			SearchProjectionFactory<EntityReference, DocumentReference> projection, String path, GeoPoint center,
			String parameterName) {
		return projection.distance( path, center );
	}

	@Override
	protected ProjectionFinalStep<List<Double>> distanceMulti(
			SearchProjectionFactory<EntityReference, DocumentReference> projection, String path, GeoPoint center,
			String parameterName) {
		return projection.distance( path, center ).multi();
	}

	@Override
	protected DistanceToFieldProjectionOptionsStep<?, Double> distance(
			SearchProjectionFactory<EntityReference, DocumentReference> projection, String path, GeoPoint center,
			DistanceUnit unit, String centerParam, String unitParam) {
		return projection.distance( path, center ).unit( unit );
	}

	@Override
	protected SortFinalStep sort(SearchSortFactory sort, String path, GeoPoint center, String parameterName) {
		return sort.distance( path, center );
	}
}
