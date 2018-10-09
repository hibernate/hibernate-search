/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchTargetModel;
import org.hibernate.search.engine.search.projection.spi.DistanceFieldSearchProjectionBuilder;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;


public class DistanceFieldSearchProjectionBuilderImpl extends AbstractFieldSearchProjectionBuilderImpl<GeoPoint, Double>
		implements DistanceFieldSearchProjectionBuilder {

	private final GeoPoint center;

	private DistanceUnit unit = DistanceUnit.METERS;

	public DistanceFieldSearchProjectionBuilderImpl(ElasticsearchSearchTargetModel searchTargetModel,
			String absoluteFieldPath,
			GeoPoint center) {
		super( searchTargetModel, absoluteFieldPath, GeoPoint.class );
		this.center = center;
	}

	@Override
	public DistanceFieldSearchProjectionBuilder unit(DistanceUnit unit) {
		this.unit = unit;
		return this;
	}

	@Override
	protected ElasticsearchSearchProjection<Double> createProjection(String absoluteFieldPath,
			ElasticsearchIndexSchemaFieldNode<GeoPoint> schemaNode) {
		return new DistanceFieldSearchProjectionImpl( absoluteFieldPath, center, unit );
	}
}
