/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
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
		protected GeoBoundingBox boundingBox;

		private Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<GeoPoint> field) {
			super( scope, field );
		}

		@Override
		public void boundingBox(GeoBoundingBox boundingBox) {
			this.boundingBox = boundingBox;
		}

		@Override
		public SearchPredicate build() {
			return new LuceneGeoPointSpatialWithinBoundingBoxPredicate( this );
		}

		@Override
		protected Query buildQuery() {
			return LatLonPoint.newBoxQuery( absoluteFieldPath, boundingBox.bottomRight().latitude(),
					boundingBox.topLeft().latitude(),
					boundingBox.topLeft().longitude(), boundingBox.bottomRight().longitude() );
		}
	}
}
