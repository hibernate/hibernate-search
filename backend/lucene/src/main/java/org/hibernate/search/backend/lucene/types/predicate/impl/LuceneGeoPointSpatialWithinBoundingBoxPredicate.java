/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.parameter;
import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.simple;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.search.Query;

public class LuceneGeoPointSpatialWithinBoundingBoxPredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private LuceneGeoPointSpatialWithinBoundingBoxPredicate(Builder builder) {
		super( builder );
	}

	public static class Factory
			extends
			AbstractLuceneValueFieldSearchQueryElementFactory<SpatialWithinBoundingBoxPredicateBuilder, GeoPoint> {
		@Override
		public Builder create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<GeoPoint> field) {
			return new Builder( scope, field );
		}
	}

	private static class Builder extends AbstractBuilder<GeoPoint> implements SpatialWithinBoundingBoxPredicateBuilder {
		protected QueryParametersValueProvider<GeoBoundingBox> boundingBoxProvider;

		private Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<GeoPoint> field) {
			super( scope, field );
		}

		@Override
		public void boundingBox(GeoBoundingBox boundingBox) {
			this.boundingBoxProvider = simple( boundingBox );
		}

		@Override
		public void param(String parameterName) {
			this.boundingBoxProvider = parameter( parameterName, GeoBoundingBox.class );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneGeoPointSpatialWithinBoundingBoxPredicate( this );
		}

		@Override
		protected Query buildQuery(PredicateRequestContext context) {
			GeoBoundingBox boundingBox = boundingBoxProvider.provide( context.toQueryParametersContext() );

			return LatLonPoint.newBoxQuery( absoluteFieldPath, boundingBox.bottomRight().latitude(),
					boundingBox.topLeft().latitude(),
					boundingBox.topLeft().longitude(), boundingBox.bottomRight().longitude() );
		}
	}
}
