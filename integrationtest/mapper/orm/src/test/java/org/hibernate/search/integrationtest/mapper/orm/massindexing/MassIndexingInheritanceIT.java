/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.hibernate.SessionFactory;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MassIndexingInheritanceIT {

	private PrintStream sysOut;
	private MassIndexerStatementInspector statementInspector;
	private SessionFactory sessionFactory;

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@BeforeAll
	void setup() {
		sysOut = System.out;

		backendMock.resetExpectations();
		statementInspector = new MassIndexerStatementInspector();

		OrmSetupHelper.SetupContext setupContext = ormSetupHelper.start()
				.dataClearing( c -> c.clearIndexData( false ).clearDatabaseData( false ) )
				.withPropertyRadical( HibernateOrmMapperSettings.Radicals.INDEXING_LISTENERS_ENABLED, false )
				// .withProperty( "hibernate.session_factory.statement_inspector", statementInspector )
				.withProperty( "hibernate.show_sql", true )
				.withAnnotatedTypes( Car.class, Truck.class, DooredVehicle.class, BedVehicle.class, BaseVehicle.class,
						BaseEntity.class, Van.class,
						A.class, AA.class, BA.class, ABA.class, BBA.class, AABA.class, AAABA.class,
						Root.class, ARoot.class, BRoot.class,
						RootTablePerClass.class, ARootTablePerClass.class, BRootTablePerClass.class,
						RootSingleTable.class, ARootSingleTable.class, BRootSingleTable.class
				);

		// We add the schema expectation as a part of a configuration, and as a last configuration.
		// this way we will only set the expectation only when the entire config was a success:
		setupContext.withConfiguration(
				ignored -> backendMock.expectAnySchema( Car.INDEX ).expectAnySchema( Truck.INDEX )
						.expectAnySchema( BedVehicle.INDEX )
						.expectAnySchema( A.INDEX )
						.expectAnySchema( AA.INDEX )
						.expectAnySchema( BA.INDEX )
						.expectAnySchema( BBA.INDEX )
						.expectAnySchema( AAABA.INDEX )
						.expectAnySchema( Root.INDEX )
						.expectAnySchema( ARoot.INDEX )

						.expectAnySchema( RootTablePerClass.INDEX )
						.expectAnySchema( ARootTablePerClass.INDEX )
						.expectAnySchema( BRootTablePerClass.INDEX )

						.expectAnySchema( RootSingleTable.INDEX )
						.expectAnySchema( ARootSingleTable.INDEX )
		);
		sessionFactory = setupContext.setup();

		with( sessionFactory ).runInTransaction( session -> {
			session.persist( Car.create( 1L ) );
			session.persist( Car.create( 2L ) );
			session.persist( Car.create( 3L ) );
			session.persist( Truck.create( 10L ) );
			session.persist( BedVehicle.create( 100L ) );
			session.persist( BedVehicle.create( 101L ) );
			session.persist( Van.create( 10_000L ) );
			session.persist( A.create( 100_000L ) );
			session.persist( AA.create( 100_001L ) );
			session.persist( BA.create( 100_002L ) );
			session.persist( ABA.create( 100_003L ) );
			session.persist( BBA.create( 100_004L ) );
			session.persist( AAABA.create( 100_005L ) );
			session.persist( Root.create( 1_000_000L ) );
			session.persist( ARoot.create( 1_000_001L ) );
			session.persist( BRoot.create( 1_000_002L ) );

			session.persist( RootSingleTable.create( 2_000_000L ) );
			session.persist( ARootSingleTable.create( 2_000_001L ) );
			session.persist( BRootSingleTable.create( 2_000_002L ) );

			session.persist( RootTablePerClass.create( 3_000_000L ) );
			session.persist( ARootTablePerClass.create( 3_000_001L ) );
			session.persist( BRootTablePerClass.create( 3_000_002L ) );
		} );
	}

	@AfterAll
	void afterAll() {
		System.setOut( sysOut );
	}

	@BeforeEach
	void setUp() throws IOException {
		ByteArrayOutputStream testSysOut = new ByteArrayOutputStream();
		System.setOut( new PrintStream( testSysOut, false, StandardCharsets.UTF_8 ) );
		statementInspector.reset( testSysOut );
	}

	@AfterEach
	void tearDown() throws IOException {
		statementInspector.close();
	}

	@Test
	void singleEntity() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( Car.class );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					Car.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1", b -> b
							.field( "id", 1L )
					)
					.add( "2", b -> b
							.field( "id", 2L )
					)
					.add( "3", b -> b
							.field( "id", 3L )
					);

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( Car.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		statementInspector.hasSelects( 3 )
				// select count(c1_0.id) from car c1_0
				.anyMatch( "select count(_big)?\\([a-z0-9_.]+\\) from car [a-z0-9_.]+" )
				// select c1_0.id from Car c1_0
				.anyMatch( "select [a-z0-9_.]+ from car [a-z0-9_.]+" )
				// select c1_0.id,c1_1.bodyType,c1_2.doorType,c1_0.carHood from Car c1_0 join BaseVehicle c1_1 on c1_0.id=c1_1.id join DooredVehicle c1_2 on c1_0.id=c1_2.id where c1_0.id in (?,?,?)
				.anyMatch( "select [a-z0-9_.,]+ from car [a-z0-9_.]+ "
						+ "join basevehicle [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "join dooredvehicle [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "where .+" );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void singleEntity_notTopOne() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( BedVehicle.class );

			backendMock.expectWorks(
					Truck.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "10", b -> b
							.field( "id", 10L )
					);
			backendMock.expectWorks(
					BedVehicle.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "100", b -> b
							.field( "id", 100L )
					)
					.add( "101", b -> b
							.field( "id", 101L )
					);

			backendMock.expectIndexScaleWorks( BedVehicle.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( Truck.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		statementInspector.hasSelects( 3 )
				// select count(bv1_0.id) from bedvehicle bv1_0
				.anyMatch( "select count(_big)?\\([a-z0-9_.]+\\) from bedvehicle [a-z0-9_.]+" )
				// select bv1_0.id from bedvehicle bv1_0
				.anyMatch( "select [a-z0-9_.]+ from bedvehicle [a-z0-9_.]+" )
				// select bv1_0.id,bv1_1.type,bv1_2.bodytype,bv1_3.doortype,bv1_0.bedtype,bv1_4.truckroof
				//   from bedvehicle bv1_0 join baseentity bv1_1 on bv1_0.id=bv1_1.id join basevehicle bv1_2 on bv1_0.id=bv1_2.id join dooredvehicle bv1_3 on bv1_0.id=bv1_3.id left join truck bv1_4 on bv1_0.id=bv1_4.id where bv1_0.id=?
				.anyMatch( "select [a-z0-9_.,]+ from bedvehicle [a-z0-9_.]+ "
						+ "join baseentity [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "join basevehicle [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "join dooredvehicle [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "left join truck [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "where .+" );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void multipleTopLevelEntities() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( Car.class, Truck.class );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					Car.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1", b -> b
							.field( "id", 1L )
					)
					.add( "2", b -> b
							.field( "id", 2L )
					)
					.add( "3", b -> b
							.field( "id", 3L )
					);
			backendMock.expectWorks(
					Truck.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "10", b -> b
							.field( "id", 10L )
					);

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( Car.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( Truck.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		statementInspector.hasSelects( 6 )
				// select count(c1_0.id) from car c1_0
				.anyMatch( "select count(_big)?\\([a-z0-9_.]+\\) from car [a-z0-9_.]+" )
				// select c1_0.id from Car c1_0
				.anyMatch( "select [a-z0-9_.]+ from car [a-z0-9_.]+" )
				// select c1_0.id,c1_1.bodyType,c1_2.doorType,c1_0.carHood from Car c1_0 join BaseVehicle c1_1 on c1_0.id=c1_1.id join DooredVehicle c1_2 on c1_0.id=c1_2.id where c1_0.id in (?,?,?)
				.anyMatch( "select [a-z0-9_.,]+ from car [a-z0-9_.]+ "
						+ "join basevehicle [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "join dooredvehicle [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "where .+" )

				// count(t1_0.id) from truck t1_0
				.anyMatch( "select count(_big)?\\([a-z0-9_.]+\\) from truck [a-z0-9_.]+" )
				// select t1_0.id from truck t1_0
				.anyMatch( "select [a-z0-9_.]+ from truck [a-z0-9_.]+" )
				// select t1_0.id,t1_1.bodytype,t1_2.doortype,t1_3.bedtype,t1_0.truckroof
				// from truck t1_0 join basevehicle t1_1 on t1_0.id=t1_1.id join dooredvehicle t1_2 on t1_0.id=t1_2.id join bedvehicle t1_3 on t1_0.id=t1_3.id where t1_0.id=?
				.anyMatch( "select [a-z0-9_.,]+ from truck [a-z0-9_.]+ "
						+ "join basevehicle [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "join dooredvehicle [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "join bedvehicle [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "where .+" );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void multipleSameBranch() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( BedVehicle.class, Truck.class );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					Truck.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "10", b -> b
							.field( "id", 10L )
					);
			backendMock.expectWorks(
					BedVehicle.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "100", b -> b
							.field( "id", 100L )
					)
					.add( "101", b -> b
							.field( "id", 101L )
					);

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( BedVehicle.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( Truck.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		statementInspector.hasSelects( 3 )
				// select count(bv1_0.id) from bedvehicle bv1_0
				.anyMatch( "select count(_big)?\\([a-z0-9_.]+\\) from bedvehicle [a-z0-9_.]+" )
				// select bv1_0.id from bedvehicle bv1_0
				.anyMatch( "select [a-z0-9_.]+ from bedvehicle [a-z0-9_.]+" )
				// select bv1_0.id,bv1_1.type,bv1_2.bodytype,bv1_3.doortype,bv1_0.bedtype,bv1_4.truckroof
				//   from bedvehicle bv1_0 join baseentity bv1_1 on bv1_0.id=bv1_1.id join basevehicle bv1_2 on bv1_0.id=bv1_2.id join dooredvehicle bv1_3 on bv1_0.id=bv1_3.id left join truck bv1_4 on bv1_0.id=bv1_4.id where bv1_0.id=?
				.anyMatch( "select [a-z0-9_.,]+ from bedvehicle [a-z0-9_.]+ "
						+ "join baseentity [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "join basevehicle [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "join dooredvehicle [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "left join truck [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "where .+" );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void withMoreConcreteTypeDisabled() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( BA.class );


			backendMock.expectWorks( BA.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "100002", b -> b.field( "id", 100002L ) );
			backendMock.expectWorks( BBA.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "100004", b -> b.field( "id", 100004L ) );
			backendMock.expectWorks( AAABA.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "100005", b -> b.field( "id", 100005L ) );

			backendMock.expectIndexScaleWorks( BA.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( BBA.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( AAABA.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		statementInspector.hasSelects( 9 )
				//    select count(b1_0.id) from bba b1_0
				//    select count(a1_0.id) from aaaba a1_0
				//    select count(b1_0.id) from ba b1_0
				//            join a b1_1 on b1_0.id=b1_1.id
				//            where b1_1.type in (?)
				.anyMatch( "select count(_big)?\\([a-z0-9_.]+\\) from bba [a-z0-9_.]+" )
				.anyMatch( "select count(_big)?\\([a-z0-9_.]+\\) from aaaba [a-z0-9_.]+" )
				.anyMatch( "select count(_big)?\\([a-z0-9_.]+\\) from ba [a-z0-9_.]+ "
						+ "join a [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "where .+"
				)
				//    select b1_0.id from bba b1_0
				//    select a1_0.id from aaaba a1_0
				//    select b1_0.id from ba b1_0
				//            join a b1_1 on b1_0.id=b1_1.id
				//            where b1_1.type in (?)
				.anyMatch( "select [a-z0-9_.]+ from bba [a-z0-9_.]+" )
				.anyMatch( "select [a-z0-9_.]+ from aaaba [a-z0-9_.]+" )
				.anyMatch( "select [a-z0-9_.]+ from ba [a-z0-9_.]+ "
						+ "join a [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "where .+"
				)


				//    select a1_0.id from aaaba a1_0 where a1_0.id=?
				//    select b1_0.id from bba b1_0 where b1_0.id=?
				//    select b1_0.id,b1_1.type from ba b1_0 join a b1_1 on b1_0.id=b1_1.id where b1_0.id=?
				.anyMatch( "select [a-z0-9_.,]+ from bba [a-z0-9_.]+ where .+" )
				.anyMatch( "select [a-z0-9_.,]+ from aaaba [a-z0-9_.]+ where .+" )
				.anyMatch( "select [a-z0-9_.,]+ from ba [a-z0-9_.]+ "
						+ "join a [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "where .+"
				);

		backendMock.verifyExpectationsMet();
	}

	@Test
	void fromRoot() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( Root.class );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					Root.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1000000", b -> b
							.field( "id", 1000000L )
					);
			backendMock.expectWorks(
					ARoot.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1000001", b -> b
							.field( "id", 1000001L )
					);

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( Root.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( ARoot.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		statementInspector.hasSelects( 6 )
				// select count(a1_0.id) from aroot a1_0
				// select count(r1_0.id) from root r1_0
				//           where r1_0.type in (?)
				.anyMatch( "select count(_big)?\\([a-z0-9_.]+\\) from aroot [a-z0-9_.]+" )
				.anyMatch( "select count(_big)?\\([a-z0-9_.]+\\) from root [a-z0-9_.]+ "
						+ "where .+"
				)
				// select a1_0.id from aroot a1_0
				// select r1_0.id from root r1_0 where r1_0.type in (?)
				.anyMatch( "select [a-z0-9_.]+ from aroot [a-z0-9_.]+" )
				.anyMatch( "select [a-z0-9_.]+ from root [a-z0-9_.]+ "
						+ "where .+"
				)
				// select a1_0.id from aroot a1_0 where a1_0.id=?
				// select r1_0.id,r1_0.type from root r1_0 where r1_0.id=?
				.anyMatch( "select [a-z0-9_.,]+ from aroot [a-z0-9_.]+" )
				.anyMatch( "select [a-z0-9_.,]+ from root [a-z0-9_.]+ "
						+ "where .+"
				);

		backendMock.verifyExpectationsMet();
	}

	@Test
	void tablePerClass() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( RootTablePerClass.class );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					RootTablePerClass.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "3000000", b -> b.field( "id", 3000000L ) );
			backendMock.expectWorks(
					ARootTablePerClass.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "3000001", b -> b.field( "id", 3000001L ) );
			backendMock.expectWorks(
					BRootTablePerClass.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "3000002", b -> b.field( "id", 3000002L ) );


			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( RootTablePerClass.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( ARootTablePerClass.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( BRootTablePerClass.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		statementInspector.hasSelects( 9 )
				// select count(atpc1_0.id) from aroottableperclass atpc1_0
				// select count(btpc1_0.id) from broottableperclass btpc1_0
				// select count(rtpc1_0.id) from (select id, 0 as clazz_ from roottableperclass union all select id, 1 as clazz_ from aroottableperclass union all select id, 2 as clazz_ from broottableperclass) rtpc1_0 where rtpc1_0.clazz_ in (?)
				.anyMatch( "select count(_big)?\\([a-z0-9_.]+\\) from aroottableperclass [a-z0-9_.]+" )
				.anyMatch( "select count(_big)?\\([a-z0-9_.]+\\) from broottableperclass [a-z0-9_.]+" )
				.anyMatch( "select count(_big)?\\([a-z0-9_.]+\\) from \\(select .+union all.+\\) [a-z0-9_.]+ where .+" )

				// select btpc1_0.id from broottableperclass btpc1_0
				// select atpc1_0.id from aroottableperclass atpc1_0
				// select rtpc1_0.id from (select id, 0 as clazz_ from roottableperclass union all select id, 1 as clazz_ from aroottableperclass union all select id, 2 as clazz_ from broottableperclass) rtpc1_0 where rtpc1_0.clazz_ in (?)
				.anyMatch( "select [a-z0-9_.]+ from aroottableperclass [a-z0-9_.]+" )
				.anyMatch( "select [a-z0-9_.]+ from broottableperclass [a-z0-9_.]+" )
				.anyMatch( "select [a-z0-9_.]+ from \\(select .+union all.+\\) [a-z0-9_.]+ where .+" )

				// select btpc1_0.id from broottableperclass btpc1_0 where btpc1_0.id=?
				// select atpc1_0.id from aroottableperclass atpc1_0 where atpc1_0.id=?
				// select rtpc1_0.id,rtpc1_0.clazz_ from (select id, 0 as clazz_ from roottableperclass union all select id, 1 as clazz_ from aroottableperclass union all select id, 2 as clazz_ from broottableperclass) rtpc1_0 where rtpc1_0.id=?
				.anyMatch( "select [a-z0-9_.]+ from aroottableperclass [a-z0-9_.]+ where .+" )
				.anyMatch( "select [a-z0-9_.]+ from broottableperclass [a-z0-9_.]+ where .+" )
				.anyMatch( "select [a-z0-9_.]+ from \\(select .+union all.+\\) [a-z0-9_.]+ where .+" );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void singleTable() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer( RootSingleTable.class );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					RootSingleTable.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "2000000", b -> b.field( "id", 2000000L ) );
			backendMock.expectWorks(
					ARootSingleTable.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( "2000001", b -> b.field( "id", 2000001L ) );

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( RootSingleTable.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
			backendMock.expectIndexScaleWorks( ARootSingleTable.INDEX )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		statementInspector.hasSelects( 3 )
				// select count(rst1_0.id) from rootsingletable rst1_0 where rst1_0.dtype in (?,?)
				.anyMatch( "select count(_big)?\\([a-z0-9_.]+\\) from rootsingletable [a-z0-9_.]+ where .+" )

				// select rst1_0.id from rootsingletable rst1_0 where rst1_0.dtype in (?,?)
				.anyMatch( "select [a-z0-9_.]+ from rootsingletable [a-z0-9_.]+ where .+" )

				// select rst1_0.id,rst1_0.dtype from rootsingletable rst1_0 where rst1_0.id in (?,?)
				.anyMatch( "select [a-z0-9_.,]+ from rootsingletable [a-z0-9_.]+ where .+" );

		backendMock.verifyExpectationsMet();
	}

	@Entity(name = "Car")
	@Indexed(index = Car.INDEX)
	public static class Car extends DooredVehicle {
		public static final String INDEX = "car";
		public String carHood;

		public static Car create(Long id) {
			Car car = new Car();
			car.id = id;
			return car;
		}
	}

	@Entity(name = "Truck")
	@Indexed(index = Truck.INDEX)
	public static class Truck extends BedVehicle {
		public static final String INDEX = "truck";
		public String truckRoof;

		public static Truck create(Long id) {
			Truck truck = new Truck();
			truck.id = id;
			return truck;
		}
	}

	@Entity(name = "Van")
	public static class Van extends DooredVehicle {
		public static Van create(long id) {
			Van van = new Van();
			van.id = id;
			return van;
		}
	}

	@Entity(name = "DooredVehicle")
	public static class DooredVehicle extends BaseVehicle {
		private static final String INDEX = "DooredVehicle";
		public String doorType;
	}

	@Entity(name = "BedVehicle")
	@Indexed(index = BedVehicle.INDEX)
	public static class BedVehicle extends DooredVehicle {
		private static final String INDEX = "BedVehicle";
		public String bedType;

		public static BedVehicle create(Long id) {
			BedVehicle vehicle = new BedVehicle();
			vehicle.id = id;
			return vehicle;
		}
	}

	@Entity(name = "BaseVehicle")
	public static class BaseVehicle extends BaseEntity {
		public String bodyType;
	}

	@Entity(name = "BaseEntity")
	@DiscriminatorColumn(name = "type")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class BaseEntity implements Serializable {

		@Id
		@DocumentId
		@GenericField(aggregable = Aggregable.YES, sortable = Sortable.YES, searchable = Searchable.YES)
		public Long id;

	}

	@Entity(name = A.INDEX)
	@DiscriminatorColumn(name = "type")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Indexed(index = A.INDEX)
	public static class A implements Serializable {
		private static final String INDEX = "A";
		@Id
		@DocumentId
		@GenericField
		public Long id;

		public static A create(Long id) {
			A e = new A();
			e.id = id;
			return e;
		}
	}

	@Entity(name = "AA")
	@Indexed(index = AA.INDEX)
	public static class AA extends A {
		private static final String INDEX = "AA";

		public static AA create(Long id) {
			AA e = new AA();
			e.id = id;
			return e;
		}
	}

	@Entity(name = "BA")
	@Indexed(index = BA.INDEX)
	public static class BA extends A {
		private static final String INDEX = "BA";

		public static BA create(Long id) {
			BA e = new BA();
			e.id = id;
			return e;
		}
	}

	@Indexed(enabled = false)
	@Entity(name = "ABA")
	public static class ABA extends BA {
		public static ABA create(Long id) {
			ABA e = new ABA();
			e.id = id;
			return e;
		}
	}

	public abstract static class AABA extends ABA {
		public static ABA create(Long id) {
			ABA e = new ABA();
			e.id = id;
			return e;
		}
	}

	@Indexed(index = AAABA.INDEX)
	@Entity(name = "AAABA")
	public static class AAABA extends AABA {
		private static final String INDEX = "AAABA";

		public static AABA create(Long id) {
			AAABA e = new AAABA();
			e.id = id;
			return e;
		}
	}

	@Entity(name = "BBA")
	@Indexed(index = BBA.INDEX)
	public static class BBA extends BA {
		private static final String INDEX = "BBA";

		public static BBA create(Long id) {
			BBA e = new BBA();
			e.id = id;
			return e;
		}
	}


	@Entity(name = Root.INDEX)
	@DiscriminatorColumn(name = "type")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Indexed(index = Root.INDEX)
	public static class Root implements Serializable {
		private static final String INDEX = "Root";
		@Id
		@DocumentId
		@GenericField
		public Long id;

		public static Root create(Long id) {
			Root e = new Root();
			e.id = id;
			return e;
		}
	}

	@Entity(name = ARoot.INDEX)
	@Indexed(index = ARoot.INDEX)
	public static class ARoot extends Root {
		private static final String INDEX = "ARoot";

		public static Root create(Long id) {
			Root e = new ARoot();
			e.id = id;
			return e;
		}
	}

	@Entity(name = BRoot.INDEX)
	@Indexed(enabled = false)
	public static class BRoot extends Root {
		private static final String INDEX = "BRoot";

		public static Root create(Long id) {
			Root e = new BRoot();
			e.id = id;
			return e;
		}
	}

	@Entity(name = RootTablePerClass.INDEX)
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@Indexed(index = RootTablePerClass.INDEX)
	public static class RootTablePerClass implements Serializable {
		private static final String INDEX = "RootTablePerClass";
		@Id
		@DocumentId
		@GenericField
		public Long id;

		public static RootTablePerClass create(Long id) {
			RootTablePerClass e = new RootTablePerClass();
			e.id = id;
			return e;
		}
	}

	@Entity(name = ARootTablePerClass.INDEX)
	@Indexed(index = ARootTablePerClass.INDEX)
	public static class ARootTablePerClass extends RootTablePerClass {
		private static final String INDEX = "ARootTablePerClass";

		public static ARootTablePerClass create(Long id) {
			ARootTablePerClass e = new ARootTablePerClass();
			e.id = id;
			return e;
		}
	}

	@Entity(name = BRootTablePerClass.INDEX)
	@Indexed(index = BRootTablePerClass.INDEX)
	public static class BRootTablePerClass extends RootTablePerClass {
		private static final String INDEX = "BRootTablePerClass";

		public static BRootTablePerClass create(Long id) {
			BRootTablePerClass e = new BRootTablePerClass();
			e.id = id;
			return e;
		}
	}

	@Entity(name = RootSingleTable.INDEX)
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@Indexed(index = RootSingleTable.INDEX)
	public static class RootSingleTable implements Serializable {
		private static final String INDEX = "RootSingleTable";
		@Id
		@DocumentId
		@GenericField
		public Long id;

		public static RootSingleTable create(Long id) {
			RootSingleTable e = new RootSingleTable();
			e.id = id;
			return e;
		}
	}

	@Entity(name = ARootSingleTable.INDEX)
	@Indexed(index = ARootSingleTable.INDEX)
	public static class ARootSingleTable extends RootSingleTable {
		private static final String INDEX = "ARootSingleTable";

		public static ARootSingleTable create(Long id) {
			ARootSingleTable e = new ARootSingleTable();
			e.id = id;
			return e;
		}
	}

	@Entity(name = BRootSingleTable.INDEX)
	@Indexed(enabled = false)
	public static class BRootSingleTable extends RootSingleTable {
		private static final String INDEX = "BRootSingleTable";

		public static BRootSingleTable create(Long id) {
			BRootSingleTable e = new BRootSingleTable();
			e.id = id;
			return e;
		}
	}

	public static class MassIndexerStatementInspector implements StatementInspector {

		private Set<String> selects;
		private ByteArrayOutputStream outputStream;

		@Override
		public String inspect(String sql) {
			if ( sql.toLowerCase( Locale.ROOT ).contains( "select" ) ) {
				selects.add( sql );
			}
			return sql;
		}

		public void reset(ByteArrayOutputStream outputStream) throws IOException {
			close();
			this.outputStream = outputStream;
		}

		public MassIndexerStatementInspector hasSelects(int size) {
			assertThat( selects() ).hasSize( size );
			return this;
		}

		public MassIndexerStatementInspector anyMatch(String pattern) {
			assertThat( selects() ).anyMatch( statement -> statement.matches( pattern ) );
			return this;
		}

		private Set<String> selects() {
			if ( selects == null ) {
				String loggedQueries = outputStream.toString( StandardCharsets.UTF_8 );
				selects = loggedQueries.lines()
						.filter( log -> log.contains( "select" ) )
						.map( log -> log.replace( "Hibernate: ", "" ).toLowerCase( Locale.ROOT ) )
						.collect( Collectors.toSet() );
			}
			return selects;
		}

		public void close() throws IOException {
			if ( selects != null ) {
				selects.clear();
				selects = null;
			}
			if ( outputStream != null ) {
				outputStream.close();
			}
		}
	}

}
