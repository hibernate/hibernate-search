/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneSpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.search.Query;

class LuceneGeoPointSpatialWithinBoundingBoxPredicateBuilder extends
		AbstractLuceneSpatialWithinBoundingBoxPredicateBuilder {

	LuceneGeoPointSpatialWithinBoundingBoxPredicateBuilder(LuceneSearchFieldContext<GeoPoint> field) {
		super( field );
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		return LatLonPoint.newBoxQuery( absoluteFieldPath, boundingBox.bottomRight().latitude(), boundingBox.topLeft().latitude(),
				boundingBox.topLeft().longitude(), boundingBox.bottomRight().longitude() );
	}
}
