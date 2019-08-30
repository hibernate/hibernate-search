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

import org.hibernate.search.engine.search.dsl.predicate.SpatialWithinPredicateFieldMoreStep;
import org.hibernate.search.engine.search.dsl.predicate.SpatialWithinPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.impl.Contracts;


class SpatialWithinPredicateFieldMoreStepImpl<B>
		implements SpatialWithinPredicateFieldMoreStep, AbstractBooleanMultiFieldPredicateCommonState.FieldSetState<B> {

	private final CommonState<B> commonState;

	private final List<String> absoluteFieldPaths;

	private final List<SearchPredicateBuilder<B>> predicateBuilders;

	private Float fieldSetBoost;

	SpatialWithinPredicateFieldMoreStepImpl(CommonState<B> commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.absoluteFieldPaths = CollectionHelper.toImmutableList( absoluteFieldPaths );
		this.predicateBuilders = new ArrayList<>( absoluteFieldPaths.size() );
	}

	@Override
	public SpatialWithinPredicateFieldMoreStep fields(String... absoluteFieldPaths) {
		return new SpatialWithinPredicateFieldMoreStepImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public SpatialWithinPredicateFieldMoreStep boost(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public SpatialWithinPredicateOptionsStep circle(GeoPoint center, double radius, DistanceUnit unit) {
		Contracts.assertNotNull( center, "center" );
		Contracts.assertNotNull( radius, "radius" );
		Contracts.assertNotNull( unit, "unit" );

		return commonState.circle( center, radius, unit );
	}

	@Override
	public SpatialWithinPredicateOptionsStep polygon(GeoPolygon polygon) {
		Contracts.assertNotNull( polygon, "polygon" );

		return commonState.polygon( polygon );
	}

	@Override
	public SpatialWithinPredicateOptionsStep boundingBox(GeoBoundingBox boundingBox) {
		Contracts.assertNotNull( boundingBox, "boundingBox" );

		return commonState.boundingBox( boundingBox );
	}

	@Override
	public List<String> getAbsoluteFieldPaths() {
		return absoluteFieldPaths;
	}

	@Override
	public void contributePredicateBuilders(Consumer<B> collector) {
		for ( SearchPredicateBuilder<B> predicateBuilder : predicateBuilders ) {
			// Perform last-minute changes, since it's the last call that will be made on this field set state
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.toImplementation() );
		}
	}

	private void generateWithinCircleQueryBuilders(GeoPoint center, double radius, DistanceUnit unit) {
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			SpatialWithinCirclePredicateBuilder<B> predicateBuilder = commonState.getFactory().spatialWithinCircle( absoluteFieldPath );
			predicateBuilder.circle( center, radius, unit );
			predicateBuilders.add( predicateBuilder );
		}
	}

	private void generateWithinPolygonQueryBuilders(GeoPolygon polygon) {
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			SpatialWithinPolygonPredicateBuilder<B> predicateBuilder = commonState.getFactory().spatialWithinPolygon( absoluteFieldPath );
			predicateBuilder.polygon( polygon );
			predicateBuilders.add( predicateBuilder );
		}
	}

	private void generateWithinBoundingBoxQueryBuilders(GeoBoundingBox boundingBox) {
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			SpatialWithinBoundingBoxPredicateBuilder<B> predicateBuilder = commonState.getFactory().spatialWithinBoundingBox( absoluteFieldPath );
			predicateBuilder.boundingBox( boundingBox );
			predicateBuilders.add( predicateBuilder );
		}
	}

	static class CommonState<B> extends AbstractBooleanMultiFieldPredicateCommonState<CommonState<B>, B, SpatialWithinPredicateFieldMoreStepImpl<B>>
			implements SpatialWithinPredicateOptionsStep {

		CommonState(SearchPredicateBuilderFactory<?, B> builderFactory) {
			super( builderFactory );
		}

		SpatialWithinPredicateOptionsStep circle(GeoPoint center, double radius, DistanceUnit unit) {
			for ( SpatialWithinPredicateFieldMoreStepImpl<B> fieldSetState : getFieldSetStates() ) {
				fieldSetState.generateWithinCircleQueryBuilders( center, radius, unit );
			}

			return this;
		}

		SpatialWithinPredicateOptionsStep polygon(GeoPolygon polygon) {
			for ( SpatialWithinPredicateFieldMoreStepImpl<B> fieldSetState : getFieldSetStates() ) {
				fieldSetState.generateWithinPolygonQueryBuilders( polygon );
			}

			return this;
		}

		SpatialWithinPredicateOptionsStep boundingBox(GeoBoundingBox boundingBox) {
			for ( SpatialWithinPredicateFieldMoreStepImpl<B> fieldSetState : getFieldSetStates() ) {
				fieldSetState.generateWithinBoundingBoxQueryBuilders( boundingBox );
			}

			return this;
		}

		@Override
		protected CommonState<B> thisAsS() {
			return this;
		}
	}
}
