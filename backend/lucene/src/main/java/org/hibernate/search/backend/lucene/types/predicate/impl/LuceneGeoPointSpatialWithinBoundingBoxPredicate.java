/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneSingleFieldPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.search.Query;

class LuceneGeoPointSpatialWithinBoundingBoxPredicate extends AbstractLuceneSingleFieldPredicate {

	private final Query query;

	private LuceneGeoPointSpatialWithinBoundingBoxPredicate(Builder builder) {
		super( builder );
		query = builder.buildQuery();
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {
		return query;
	}

	static class Builder extends AbstractBuilder implements SpatialWithinBoundingBoxPredicateBuilder {
		protected GeoBoundingBox boundingBox;

		Builder(LuceneSearchContext searchContext, LuceneSearchFieldContext<GeoPoint> field) {
			super( searchContext, field );
		}

		@Override
		public void boundingBox(GeoBoundingBox boundingBox) {
			this.boundingBox = boundingBox;
		}

		@Override
		public SearchPredicate build() {
			return new LuceneGeoPointSpatialWithinBoundingBoxPredicate( this );
		}

		private Query buildQuery() {
			return LatLonPoint.newBoxQuery( absoluteFieldPath, boundingBox.bottomRight().latitude(), boundingBox.topLeft().latitude(),
					boundingBox.topLeft().longitude(), boundingBox.bottomRight().longitude() );
		}
	}
}
