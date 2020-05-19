/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.List;

import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneSpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;

class LuceneGeoPointSpatialWithinBoundingBoxPredicateBuilder extends
		AbstractLuceneSpatialWithinBoundingBoxPredicateBuilder {

	LuceneGeoPointSpatialWithinBoundingBoxPredicateBuilder(String absoluteFieldPath, List<String> nestedPathHierarchy) {
		super( absoluteFieldPath, nestedPathHierarchy );
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		return LatLonPoint.newBoxQuery( absoluteFieldPath, boundingBox.bottomRight().latitude(), boundingBox.topLeft().latitude(),
				boundingBox.topLeft().longitude(), boundingBox.bottomRight().longitude() );
	}
}
