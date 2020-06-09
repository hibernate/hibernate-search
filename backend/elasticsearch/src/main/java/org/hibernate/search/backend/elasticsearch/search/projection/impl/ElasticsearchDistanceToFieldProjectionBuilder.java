/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Set;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;


public class ElasticsearchDistanceToFieldProjectionBuilder implements DistanceToFieldProjectionBuilder {

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final String[] absoluteFieldPathComponents;
	private boolean nested;

	private final GeoPoint center;

	private DistanceUnit unit = DistanceUnit.METERS;

	public ElasticsearchDistanceToFieldProjectionBuilder(Set<String> indexNames, String absoluteFieldPath,
			String[] absoluteFieldPathComponents, boolean nested, GeoPoint center) {
		this.indexNames = indexNames;
		this.absoluteFieldPath = absoluteFieldPath;
		this.absoluteFieldPathComponents = absoluteFieldPathComponents;
		this.nested = nested;
		this.center = center;
	}

	@Override
	public DistanceToFieldProjectionBuilder unit(DistanceUnit unit) {
		this.unit = unit;
		return this;
	}

	@Override
	public <P> SearchProjection<P> build(ProjectionAccumulator.Provider<Double, P> accumulatorProvider) {
		return new ElasticsearchDistanceToFieldProjection<>( indexNames, absoluteFieldPath, absoluteFieldPathComponents,
				nested, !accumulatorProvider.isSingleValued(), center, unit, accumulatorProvider.get() );
	}
}
