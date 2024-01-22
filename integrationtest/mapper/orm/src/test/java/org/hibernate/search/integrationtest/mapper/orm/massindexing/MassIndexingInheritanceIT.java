/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
						BaseEntity.class );

		// We add the schema expectation as a part of a configuration, and as a last configuration.
		// this way we will only set the expectation only when the entire config was a success:
		setupContext.withConfiguration(
				ignored -> backendMock.expectAnySchema( Car.INDEX ).expectAnySchema( Truck.INDEX )
						.expectAnySchema( BedVehicle.INDEX )
		);
		sessionFactory = setupContext.setup();

		with( sessionFactory ).runInTransaction( session -> {
			session.persist( Car.create( 1L ) );
			session.persist( Car.create( 2L ) );
			session.persist( Car.create( 3L ) );
			session.persist( Truck.create( 10L ) );
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
				.anyMatch( "select count\\([a-z0-9_.]+\\) from car [a-z0-9_.]+" )
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
				.anyMatch( "select count\\([a-z0-9_.]+\\) from car [a-z0-9_.]+" )
				// select c1_0.id from Car c1_0
				.anyMatch( "select [a-z0-9_.]+ from car [a-z0-9_.]+" )
				// select c1_0.id,c1_1.bodyType,c1_2.doorType,c1_0.carHood from Car c1_0 join BaseVehicle c1_1 on c1_0.id=c1_1.id join DooredVehicle c1_2 on c1_0.id=c1_2.id where c1_0.id in (?,?,?)
				.anyMatch( "select [a-z0-9_.,]+ from car [a-z0-9_.]+ "
						+ "join basevehicle [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "join dooredvehicle [a-z0-9_.]+ on [a-z0-9_.=]+ "
						+ "where .+" )

				// count(t1_0.id) from truck t1_0
				.anyMatch( "select count\\([a-z0-9_.]+\\) from truck [a-z0-9_.]+" )
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
				.anyMatch( "select count\\([a-z0-9_.]+\\) from bedvehicle [a-z0-9_.]+" )
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
