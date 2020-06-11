/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;


public class ElasticsearchDistanceToFieldProjectionBuilder implements DistanceToFieldProjectionBuilder {

	private final ElasticsearchSearchContext searchContext;
	private final ElasticsearchSearchFieldContext<GeoPoint> field;

	private final GeoPoint center;

	private DistanceUnit unit = DistanceUnit.METERS;

	public ElasticsearchDistanceToFieldProjectionBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<GeoPoint> field, GeoPoint center) {
		this.searchContext = searchContext;
		this.field = field;
		this.center = center;
	}

	@Override
	public DistanceToFieldProjectionBuilder unit(DistanceUnit unit) {
		this.unit = unit;
		return this;
	}

	@Override
	public <P> SearchProjection<P> build(ProjectionAccumulator.Provider<Double, P> accumulatorProvider) {
		return new ElasticsearchDistanceToFieldProjection<>( searchContext.indexes().hibernateSearchIndexNames(),
				field.absolutePath(), field.absolutePathComponents(), !field.nestedPathHierarchy().isEmpty(),
				!accumulatorProvider.isSingleValued(), center, unit, accumulatorProvider.get() );
	}
}
