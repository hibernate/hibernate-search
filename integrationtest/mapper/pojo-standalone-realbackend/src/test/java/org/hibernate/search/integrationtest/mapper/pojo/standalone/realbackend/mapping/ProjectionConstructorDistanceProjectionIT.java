/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DistanceProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ProjectionConstructorDistanceProjectionIT {

	public static final String INDEX_NAME = "index_name";

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withSingleBackend(
			MethodHandles.lookup(), BackendConfigurations.simple() );

	@Test
	void noArg() {
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( NoArgMyProjection.class )
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy",
						IndexingPlanSynchronizationStrategyNames.READ_SYNC )
				.setup( NoArgIndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			SearchIndexingPlan searchIndexingPlan = session.indexingPlan();
			for ( int i = 0; i < 5; i++ ) {
				searchIndexingPlan.add( new NoArgIndexedEntity( i, GeoPoint.of( i, i ) ) );
			}
		}

		try ( SearchSession session = mapping.createSession() ) {
			List<NoArgMyProjection> projections = session.search( NoArgIndexedEntity.class )
					.select( NoArgMyProjection.class )
					.where( SearchPredicateFactory::matchAll )
					.param( "param", GeoPoint.of( 1, 1 ) )
					.fetchAllHits();
			assertThat( projections ).hasSize( 5 )
					.satisfiesOnlyOnce(
							proj -> assertThat( proj.point ).isZero() )
					.anySatisfy(
							proj -> assertThat( proj.point ).isGreaterThan( 0.0 ) );
		}
	}

	@Indexed(index = INDEX_NAME)
	static class NoArgIndexedEntity {
		@DocumentId
		public Integer id;
		@GenericField(projectable = Projectable.YES)
		public GeoPoint point;

		public NoArgIndexedEntity() {
		}

		public NoArgIndexedEntity(Integer id, GeoPoint point) {
			this.id = id;
			this.point = point;
		}
	}

	static class NoArgMyProjection {
		public final Double point;

		@ProjectionConstructor
		public NoArgMyProjection(@DistanceProjection(fromParam = "param") Double point) {
			this.point = point;
		}
	}

	@Test
	void path() {
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( PathMyProjection.class )
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy",
						IndexingPlanSynchronizationStrategyNames.READ_SYNC )
				.setup( PathIndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			SearchIndexingPlan searchIndexingPlan = session.indexingPlan();
			for ( int i = 0; i < 5; i++ ) {
				searchIndexingPlan.add( new PathIndexedEntity( i, GeoPoint.of( i, i ) ) );
			}
		}

		try ( SearchSession session = mapping.createSession() ) {
			List<PathMyProjection> projections = session.search( PathIndexedEntity.class )
					.select( PathMyProjection.class )
					.where( SearchPredicateFactory::matchAll )
					.param( "param", GeoPoint.of( 1, 1 ) )
					.fetchAllHits();
			assertThat( projections ).hasSize( 5 )
					.satisfiesOnlyOnce(
							proj -> assertThat( proj.distance ).isZero() )
					.anySatisfy(
							proj -> assertThat( proj.distance ).isGreaterThan( 0.0 ) );
		}
	}

	@Indexed(index = INDEX_NAME)
	static class PathIndexedEntity {
		@DocumentId
		public Integer id;
		@GenericField(projectable = Projectable.YES)
		public GeoPoint point;

		public PathIndexedEntity() {
		}

		public PathIndexedEntity(Integer id, GeoPoint point) {
			this.id = id;
			this.point = point;
		}
	}

	static class PathMyProjection {
		public final Double distance;

		@ProjectionConstructor
		public PathMyProjection(@DistanceProjection(fromParam = "param", path = "point") Double distance) {
			this.distance = distance;
		}
	}

	@Test
	void unit() {
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( UnitMyProjection.class )
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy",
						IndexingPlanSynchronizationStrategyNames.READ_SYNC )
				.setup( UnitIndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			SearchIndexingPlan searchIndexingPlan = session.indexingPlan();
			for ( int i = 0; i < 5; i++ ) {
				searchIndexingPlan.add( new UnitIndexedEntity( i, GeoPoint.of( i, i ) ) );
			}
		}

		try ( SearchSession session = mapping.createSession() ) {
			List<UnitMyProjection> projections = session.search( UnitIndexedEntity.class )
					.select( UnitMyProjection.class )
					.where( SearchPredicateFactory::matchAll )
					.param( "param", GeoPoint.of( 1, 1 ) )
					.fetchAllHits();
			assertThat( projections ).hasSize( 5 )
					.satisfiesOnlyOnce(
							proj -> assertThat( proj.distance ).isZero() )
					.anySatisfy(
							proj -> assertThat( proj.distance ).isGreaterThan( 0.0 )
									.isLessThan( 1000 ) );
		}
	}

	@Indexed(index = INDEX_NAME)
	static class UnitIndexedEntity {
		@DocumentId
		public Integer id;
		@GenericField(projectable = Projectable.YES)
		public GeoPoint point;

		public UnitIndexedEntity() {
		}

		public UnitIndexedEntity(Integer id, GeoPoint point) {
			this.id = id;
			this.point = point;
		}
	}

	static class UnitMyProjection {
		public final Double distance;

		@ProjectionConstructor
		public UnitMyProjection(
				@DistanceProjection(fromParam = "param", path = "point", unit = DistanceUnit.KILOMETERS) Double distance) {
			this.distance = distance;
		}
	}

	@Test
	void multi() {
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MultiMyProjection.class )
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy",
						IndexingPlanSynchronizationStrategyNames.READ_SYNC )
				.setup( MultiIndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			SearchIndexingPlan searchIndexingPlan = session.indexingPlan();
			for ( int i = 0; i < 5; i++ ) {
				searchIndexingPlan.add(
						new MultiIndexedEntity( i, List.of( GeoPoint.of( i, i ), GeoPoint.of( i * 1.5, i * 1.5 ) ) ) );
			}
		}

		try ( SearchSession session = mapping.createSession() ) {
			List<MultiMyProjection> projections = session.search( MultiIndexedEntity.class )
					.select( MultiMyProjection.class )
					.where( SearchPredicateFactory::matchAll )
					.param( "param", GeoPoint.of( 1, 2 ) )
					.fetchAllHits();
			assertThat( projections ).hasSize( 5 )
					.allSatisfy(
							proj -> assertThat( proj.distance ).hasSize( 2 )
									.allSatisfy( distance -> assertThat( distance ).isGreaterThan( 0.0 )
											.isLessThan( 1_000_000 ) )
					);
		}
	}

	@Indexed(index = INDEX_NAME)
	static class MultiIndexedEntity {
		@DocumentId
		public Integer id;
		@GenericField(projectable = Projectable.YES)
		public Collection<GeoPoint> points;

		public MultiIndexedEntity() {
		}

		public MultiIndexedEntity(Integer id, Collection<GeoPoint> points) {
			this.id = id;
			this.points = points;
		}
	}

	static class MultiMyProjection {
		public final Collection<Double> distance;

		@ProjectionConstructor
		public MultiMyProjection(@DistanceProjection(fromParam = "param", path = "points") Collection<Double> distance) {
			this.distance = distance;
		}
	}

	@Test
	void multi_number() {
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MultiNumberMyProjection.class )
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy",
						IndexingPlanSynchronizationStrategyNames.READ_SYNC )
				.setup( MultiNumberIndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			SearchIndexingPlan searchIndexingPlan = session.indexingPlan();
			for ( int i = 0; i < 5; i++ ) {
				searchIndexingPlan.add(
						new MultiNumberIndexedEntity( i, List.of( GeoPoint.of( i, i ), GeoPoint.of( i * 1.5, i * 1.5 ) ) ) );
			}
		}

		try ( SearchSession session = mapping.createSession() ) {
			List<MultiNumberMyProjection> projections = session.search( MultiNumberIndexedEntity.class )
					.select( MultiNumberMyProjection.class )
					.where( SearchPredicateFactory::matchAll )
					.param( "param", GeoPoint.of( 1, 2 ) )
					.fetchAllHits();
			assertThat( projections ).hasSize( 5 )
					.allSatisfy( proj -> assertThat( proj.distance ).hasSize( 2 ) );
		}
	}

	@Indexed(index = INDEX_NAME)
	static class MultiNumberIndexedEntity {
		@DocumentId
		public Integer id;
		@GenericField(projectable = Projectable.YES)
		public Collection<GeoPoint> points;

		public MultiNumberIndexedEntity() {
		}

		public MultiNumberIndexedEntity(Integer id, Collection<GeoPoint> points) {
			this.id = id;
			this.points = points;
		}
	}

	static class MultiNumberMyProjection {
		public final Collection<Number> distance;

		@ProjectionConstructor
		public MultiNumberMyProjection(@DistanceProjection(fromParam = "param", path = "points") Collection<Number> distance) {
			this.distance = distance;
		}
	}

	@Test
	void invalidType() {
		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( InvalidTypeMyProjection.class )
				.setup( InvalidTypeIndexedEntity.class )
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid constructor parameter type: 'java.lang.String'. The distance projection results in values of type 'SomeContainer<Double>'" );
	}

	@Indexed(index = INDEX_NAME)
	static class InvalidTypeIndexedEntity {
		@DocumentId
		public Integer id;
		@GenericField(projectable = Projectable.YES)
		public Collection<GeoPoint> points;

		public InvalidTypeIndexedEntity() {
		}

		public InvalidTypeIndexedEntity(Integer id, Collection<GeoPoint> points) {
			this.id = id;
			this.points = points;
		}
	}

	static class InvalidTypeMyProjection {
		public final List<String> distance;

		@ProjectionConstructor
		public InvalidTypeMyProjection(@DistanceProjection(fromParam = "param", path = "points") List<String> distance) {
			this.distance = distance;
		}
	}
}
