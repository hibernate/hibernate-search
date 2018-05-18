/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.engine.backend.spatial.GeoPolygon;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;


public abstract class AbstractSpatialWithinPolygonPredicateBuilder<T> extends AbstractSearchPredicateBuilder
		implements SpatialWithinPolygonPredicateBuilder<LuceneSearchPredicateCollector> {

	protected final String absoluteFieldPath;

	protected GeoPolygon polygon;

	protected AbstractSpatialWithinPolygonPredicateBuilder(String absoluteFieldPath) {
		this.absoluteFieldPath = absoluteFieldPath;
	}

	@Override
	public void polygon(GeoPolygon polygon) {
		this.polygon = polygon;
	}
}
