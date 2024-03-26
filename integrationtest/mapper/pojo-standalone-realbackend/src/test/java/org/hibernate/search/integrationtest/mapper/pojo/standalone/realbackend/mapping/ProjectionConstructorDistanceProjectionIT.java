/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@GenericField(projectable = Projectable.YES)
			public GeoPoint point;

			public IndexedEntity() {
			}

			public IndexedEntity(Integer id, GeoPoint point) {
				this.id = id;
				this.point = point;
			}
		}
		class MyProjection {
			public final Double point;

			@ProjectionConstructor
			public MyProjection(@DistanceProjection(fromParam = "param") Double point) {
				this.point = point;
			}
		}

		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy",
						IndexingPlanSynchronizationStrategyNames.READ_SYNC )
				.setup( IndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			SearchIndexingPlan searchIndexingPlan = session.indexingPlan();
			for ( int i = 0; i < 5; i++ ) {
				searchIndexingPlan.add( new IndexedEntity( i, GeoPoint.of( i, i ) ) );
			}
		}

		try ( SearchSession session = mapping.createSession() ) {
			List<MyProjection> projections = session.search( IndexedEntity.class )
					.select( MyProjection.class )
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

	@Test
	void path() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@GenericField(projectable = Projectable.YES)
			public GeoPoint point;

			public IndexedEntity() {
			}

			public IndexedEntity(Integer id, GeoPoint point) {
				this.id = id;
				this.point = point;
			}
		}
		class MyProjection {
			public final Double distance;

			@ProjectionConstructor
			public MyProjection(@DistanceProjection(fromParam = "param", path = "point") Double distance) {
				this.distance = distance;
			}
		}

		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy",
						IndexingPlanSynchronizationStrategyNames.READ_SYNC )
				.setup( IndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			SearchIndexingPlan searchIndexingPlan = session.indexingPlan();
			for ( int i = 0; i < 5; i++ ) {
				searchIndexingPlan.add( new IndexedEntity( i, GeoPoint.of( i, i ) ) );
			}
		}

		try ( SearchSession session = mapping.createSession() ) {
			List<MyProjection> projections = session.search( IndexedEntity.class )
					.select( MyProjection.class )
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

	@Test
	void unit() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@GenericField(projectable = Projectable.YES)
			public GeoPoint point;

			public IndexedEntity() {
			}

			public IndexedEntity(Integer id, GeoPoint point) {
				this.id = id;
				this.point = point;
			}
		}
		class MyProjection {
			public final Double distance;

			@ProjectionConstructor
			public MyProjection(
					@DistanceProjection(fromParam = "param", path = "point", unit = DistanceUnit.KILOMETERS) Double distance) {
				this.distance = distance;
			}
		}

		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy",
						IndexingPlanSynchronizationStrategyNames.READ_SYNC )
				.setup( IndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			SearchIndexingPlan searchIndexingPlan = session.indexingPlan();
			for ( int i = 0; i < 5; i++ ) {
				searchIndexingPlan.add( new IndexedEntity( i, GeoPoint.of( i, i ) ) );
			}
		}

		try ( SearchSession session = mapping.createSession() ) {
			List<MyProjection> projections = session.search( IndexedEntity.class )
					.select( MyProjection.class )
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

	@Test
	void multi() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@GenericField(projectable = Projectable.YES)
			public Collection<GeoPoint> points;

			public IndexedEntity() {
			}

			public IndexedEntity(Integer id, Collection<GeoPoint> points) {
				this.id = id;
				this.points = points;
			}
		}
		class MyProjection {
			public final Collection<Double> distance;

			@ProjectionConstructor
			public MyProjection(@DistanceProjection(fromParam = "param", path = "points") Collection<Double> distance) {
				this.distance = distance;
			}
		}

		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy",
						IndexingPlanSynchronizationStrategyNames.READ_SYNC )
				.setup( IndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			SearchIndexingPlan searchIndexingPlan = session.indexingPlan();
			for ( int i = 0; i < 5; i++ ) {
				searchIndexingPlan.add(
						new IndexedEntity( i, List.of( GeoPoint.of( i, i ), GeoPoint.of( i * 1.5, i * 1.5 ) ) ) );
			}
		}

		try ( SearchSession session = mapping.createSession() ) {
			List<MyProjection> projections = session.search( IndexedEntity.class )
					.select( MyProjection.class )
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

	@Test
	void multi_number() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@GenericField(projectable = Projectable.YES)
			public Collection<GeoPoint> points;

			public IndexedEntity() {
			}

			public IndexedEntity(Integer id, Collection<GeoPoint> points) {
				this.id = id;
				this.points = points;
			}
		}
		class MyProjection {
			public final Collection<Number> distance;

			@ProjectionConstructor
			public MyProjection(@DistanceProjection(fromParam = "param", path = "points") Collection<Number> distance) {
				this.distance = distance;
			}
		}

		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy",
						IndexingPlanSynchronizationStrategyNames.READ_SYNC )
				.setup( IndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			SearchIndexingPlan searchIndexingPlan = session.indexingPlan();
			for ( int i = 0; i < 5; i++ ) {
				searchIndexingPlan.add(
						new IndexedEntity( i, List.of( GeoPoint.of( i, i ), GeoPoint.of( i * 1.5, i * 1.5 ) ) ) );
			}
		}

		try ( SearchSession session = mapping.createSession() ) {
			List<MyProjection> projections = session.search( IndexedEntity.class )
					.select( MyProjection.class )
					.where( SearchPredicateFactory::matchAll )
					.param( "param", GeoPoint.of( 1, 2 ) )
					.fetchAllHits();
			assertThat( projections ).hasSize( 5 )
					.allSatisfy( proj -> assertThat( proj.distance ).hasSize( 2 ) );
		}
	}

	@Test
	void invalidType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@GenericField(projectable = Projectable.YES)
			public Collection<GeoPoint> points;

			public IndexedEntity() {
			}

			public IndexedEntity(Integer id, Collection<GeoPoint> points) {
				this.id = id;
				this.points = points;
			}
		}
		class MyProjection {
			public final List<String> distance;

			@ProjectionConstructor
			public MyProjection(@DistanceProjection(fromParam = "param", path = "points") List<String> distance) {
				this.distance = distance;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class )
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid constructor parameter type: 'java.lang.String'. The distance projection results in values of type 'List<Double>'" );
	}
}
