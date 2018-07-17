/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractSpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.engine.spatial.GeoPoint;

class GeoPointSpatialWithinBoundingBoxPredicateBuilder extends AbstractSpatialWithinBoundingBoxPredicateBuilder<GeoPoint> {

	GeoPointSpatialWithinBoundingBoxPredicateBuilder(String absoluteFieldPath) {
		super( absoluteFieldPath );
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		return LatLonPoint.newBoxQuery( absoluteFieldPath, boundingBox.getBottomRight().getLatitude(), boundingBox.getTopLeft().getLatitude(),
				boundingBox.getTopLeft().getLongitude(), boundingBox.getBottomRight().getLongitude() );
	}
}
