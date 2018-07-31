/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.search.engine.search.dsl.ExplicitEndContext;
import org.hibernate.search.engine.search.dsl.predicate.SpatialWithinPredicateFieldSetContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;
import org.hibernate.search.util.impl.common.CollectionHelper;
import org.hibernate.search.util.impl.common.Contracts;


class SpatialWithinPredicateFieldSetContextImpl<N, B>
		implements SpatialWithinPredicateFieldSetContext<N>, MultiFieldPredicateCommonState.FieldSetContext<B> {

	private final CommonState<N, B> commonState;

	private final List<String> absoluteFieldPaths;

	private final List<SearchPredicateBuilder<B>> predicateBuilders;

	private Float boost;

	SpatialWithinPredicateFieldSetContextImpl(CommonState<N, B> commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.absoluteFieldPaths = CollectionHelper.toImmutableList( absoluteFieldPaths );
		this.predicateBuilders = new ArrayList<>( absoluteFieldPaths.size() );
	}

	@Override
	public SpatialWithinPredicateFieldSetContext<N> orFields(String... absoluteFieldPaths) {
		return new SpatialWithinPredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public SpatialWithinPredicateFieldSetContext<N> boostedTo(float boost) {
		this.boost = boost;
		return this;
	}

	@Override
	public ExplicitEndContext<N> circle(GeoPoint center, double radius, DistanceUnit unit) {
		Contracts.assertNotNull( center, "center" );
		Contracts.assertNotNull( radius, "radius" );
		Contracts.assertNotNull( unit, "unit" );

		return commonState.circle( center, radius, unit );
	}

	@Override
	public ExplicitEndContext<N> polygon(GeoPolygon polygon) {
		Contracts.assertNotNull( polygon, "polygon" );

		return commonState.polygon( polygon );
	}

	@Override
	public ExplicitEndContext<N> boundingBox(GeoBoundingBox boundingBox) {
		Contracts.assertNotNull( boundingBox, "boundingBox" );

		return commonState.boundingBox( boundingBox );
	}

	@Override
	public void contributePredicateBuilders(Consumer<B> collector) {
		for ( SearchPredicateBuilder<B> predicateBuilder : predicateBuilders ) {
			collector.accept( predicateBuilder.toImplementation() );
		}
	}

	private void generateWithinCircleQueryBuilders(GeoPoint center, double radius, DistanceUnit unit) {
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			SpatialWithinCirclePredicateBuilder<B> predicateBuilder = commonState.getFactory().spatialWithinCircle( absoluteFieldPath );
			predicateBuilder.circle( center, radius, unit );
			if ( boost != null ) {
				predicateBuilder.boost( boost );
			}
			predicateBuilders.add( predicateBuilder );
		}
	}

	private void generateWithinPolygonQueryBuilders(GeoPolygon polygon) {
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			SpatialWithinPolygonPredicateBuilder<B> predicateBuilder = commonState.getFactory().spatialWithinPolygon( absoluteFieldPath );
			predicateBuilder.polygon( polygon );
			if ( boost != null ) {
				predicateBuilder.boost( boost );
			}
			predicateBuilders.add( predicateBuilder );
		}
	}

	private void generateWithinBoundingBoxQueryBuilders(GeoBoundingBox boundingBox) {
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			SpatialWithinBoundingBoxPredicateBuilder<B> predicateBuilder = commonState.getFactory().spatialWithinBoundingBox( absoluteFieldPath );
			predicateBuilder.boundingBox( boundingBox );
			if ( boost != null ) {
				predicateBuilder.boost( boost );
			}
			predicateBuilders.add( predicateBuilder );
		}
	}

	static class CommonState<N, B> extends MultiFieldPredicateCommonState<N, B, SpatialWithinPredicateFieldSetContextImpl<N, B>>
			implements ExplicitEndContext<N> {

		CommonState(SearchPredicateFactory<?, B> factory, Supplier<N> nextContextProvider) {
			super( factory, nextContextProvider );
		}

		public ExplicitEndContext<N> circle(GeoPoint center, double radius, DistanceUnit unit) {
			for ( SpatialWithinPredicateFieldSetContextImpl<N, B> fieldSetContext : getFieldSetContexts() ) {
				fieldSetContext.generateWithinCircleQueryBuilders( center, radius, unit );
			}

			return this;
		}

		public ExplicitEndContext<N> polygon(GeoPolygon polygon) {
			for ( SpatialWithinPredicateFieldSetContextImpl<N, B> fieldSetContext : getFieldSetContexts() ) {
				fieldSetContext.generateWithinPolygonQueryBuilders( polygon );
			}

			return this;
		}

		public ExplicitEndContext<N> boundingBox(GeoBoundingBox boundingBox) {
			for ( SpatialWithinPredicateFieldSetContextImpl<N, B> fieldSetContext : getFieldSetContexts() ) {
				fieldSetContext.generateWithinBoundingBoxQueryBuilders( boundingBox );
			}

			return this;
		}

		@Override
		public N end() {
			return getNextContextProvider().get();
		}
	}

}
