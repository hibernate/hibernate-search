/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.List;

import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;


public abstract class AbstractLuceneSpatialWithinCirclePredicateBuilder
		extends AbstractLuceneSingleFieldPredicateBuilder
		implements SpatialWithinCirclePredicateBuilder<LuceneSearchPredicateBuilder> {

	protected GeoPoint center;

	protected double radiusInMeters;

	protected AbstractLuceneSpatialWithinCirclePredicateBuilder(String absoluteFieldPath, List<String> nestedPathHierarchy) {
		super( absoluteFieldPath, nestedPathHierarchy );
	}

	@Override
	public void circle(GeoPoint center, double radius, DistanceUnit unit) {
		this.center = center;
		this.radiusInMeters = unit.toMeters( radius );
	}
}
