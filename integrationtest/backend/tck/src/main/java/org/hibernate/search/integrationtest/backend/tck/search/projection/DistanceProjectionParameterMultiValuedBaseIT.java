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
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

@TestForIssue(jiraKey = "HSEARCH-3391")
class DistanceProjectionParameterMultiValuedBaseIT extends AbstractDistanceProjectionMultiValuedBaseIT {

	@Override
	protected void addParameter(SearchQueryOptionsStep<?, ?, ?, ?, ?> query, String parameterName, Object value) {
		query.param( parameterName, value );
	}

	@Override
	protected DistanceToFieldProjectionValueStep<?, Double> distance(
			SearchProjectionFactory<EntityReference, DocumentReference> projection, String path, GeoPoint center,
			String parameterName) {
		return projection.distance( path ).fromParam( parameterName );
	}

	@Override
	protected DistanceToFieldProjectionOptionsStep<?, List<Double>> distance(
			SearchProjectionFactory<EntityReference, DocumentReference> projection, String path, GeoPoint center,
			DistanceUnit unit, String centerParam, String unitParam) {
		return projection.distance( path ).fromParam( centerParam ).multi().unitParam( unitParam );
	}

	@Override
	protected DistanceSortOptionsStep<?, ?> sort(SearchSortFactory sort, String path, GeoPoint center,
			String parameterName) {
		return sort.distance( path ).fromParam( parameterName );
	}
}
