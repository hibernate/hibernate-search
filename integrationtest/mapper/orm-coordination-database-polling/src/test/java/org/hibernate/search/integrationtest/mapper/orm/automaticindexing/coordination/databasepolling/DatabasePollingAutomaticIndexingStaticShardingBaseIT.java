/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.coordination.databasepolling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.StaticCounters;
import org.hibernate.tool.schema.Action;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.data.Percentage;

/**
 * Base tests for static sharding with the database-polling coordination strategy:
 * checks that all events are handled by one and only one node
 * (if they were not, we would see missing or duplicate indexing work executions).
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-4141")
public class DatabasePollingAutomaticIndexingStaticShardingBaseIT {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Parameterized.Parameters(name = "totalShardCount = {0}")
	public static List<Integer> params() {
		return Arrays.asList( 2, 4, 10 );
	}

	@Parameterized.Parameter
	public int totalShardCount;

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	@Rule
	public StaticCounters counters = new StaticCounters();

	private final List<StaticCounters.Key> sessionFactoryCounterKeys = new ArrayList<>();
	private final List<SessionFactory> sessionFactories = new ArrayList<>();

	@Before
	public void setup() {
		for ( int i = 0; i < totalShardCount; i++ ) {
			sessionFactories.add( setup(
					// Avoid session factories getting in each other's feet.
					i == 0 ? Action.CREATE_DROP : Action.NONE,
					i
			) );
		}

		backendMock.verifyExpectationsMet();
	}

	private SessionFactory setup(Action action, int assignedShardIndex) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
				.field( PerSessionFactoryIndexingTracingBridge.FAKE_FIELD_NAME, PerSessionFactoryIndexingTracingBridge.FAKE_FIELD_TYPE )
		);
		backendMock.expectSchema( IndexedAndContainingEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
				.field( PerSessionFactoryIndexingTracingBridge.FAKE_FIELD_NAME, PerSessionFactoryIndexingTracingBridge.FAKE_FIELD_TYPE )
				.objectField( "contained", b2 -> b2
						.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) ) )
		);

		StaticCounters.Key counterKey = StaticCounters.createKey();
		sessionFactoryCounterKeys.add( counterKey );

		return ormSetupHelper.start()
				.withProperty( org.hibernate.cfg.Environment.HBM2DDL_AUTO, action )
				.withProperty( PerSessionFactoryIndexingTracingBridge.SESSION_FACTORY_COUNTER_KEY_PROPERTY, counterKey )
				.withProperty( "hibernate.search.coordination.shards.static", "true" )
				.withProperty( "hibernate.search.coordination.shards.total_count", totalShardCount )
				.withProperty( "hibernate.search.coordination.shards.assigned", String.valueOf( assignedShardIndex ) )
				.setup( IndexedEntity.class, IndexedAndContainingEntity.class, ContainedEntity.class );
	}

	@Test
	public void uniqueWorkAcrossSessionFactories_insertUpdateDelete_indexed() {
		SessionFactory sessionFactory = sessionFactories.get( 0 );

		withinTransaction( sessionFactory, session -> {
			IndexedEntity entity = new IndexedEntity( 1, "initial" );
			session.save( entity );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b.field( "text", "initial" ) );
		} );
		backendMock.verifyExpectationsMet();

		withinTransaction( sessionFactory, session -> {
			IndexedEntity entity = session.load( IndexedEntity.class, 1 );
			entity.setText( "updated" );

			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( "1", b -> b.field( "text", "updated" ) );
		} );
		backendMock.verifyExpectationsMet();

		withinTransaction( sessionFactory, session -> {
			IndexedEntity entity = session.load( IndexedEntity.class, 1 );
			session.delete( entity );

			backendMock.expectWorks( IndexedEntity.NAME )
					.delete( "1" );
		} );
		backendMock.verifyExpectationsMet();

		assertIndexingCountAcrossAllSessionFactories().isEqualTo( 2 );
	}

	@Test
	public void uniqueWorkAcrossSessionFactories_insertUpdateDelete_contained() {
		SessionFactory sessionFactory = sessionFactories.get( 0 );

		withinTransaction( sessionFactory, session -> {
			IndexedAndContainingEntity containing = new IndexedAndContainingEntity( 1, "initial" );
			ContainedEntity contained = new ContainedEntity( 2, "initial" );
			containing.setContained( contained );
			contained.setContaining( containing );
			session.persist( containing );
			session.persist( contained );

			backendMock.expectWorks( IndexedAndContainingEntity.NAME )
					.add( "1", b -> b.field( "text", "initial" )
							.objectField( "contained", b2 -> b2
									.field( "text", "initial" ) ) );
		} );
		backendMock.verifyExpectationsMet();

		withinTransaction( sessionFactory, session -> {
			ContainedEntity contained = session.load( ContainedEntity.class, 2 );
			contained.setText( "updated" );

			backendMock.expectWorks( IndexedAndContainingEntity.NAME )
					.addOrUpdate( "1", b -> b.field( "text", "initial" )
							.objectField( "contained", b2 -> b2
									.field( "text", "updated" ) ) );
		} );
		backendMock.verifyExpectationsMet();

		withinTransaction( sessionFactory, session -> {
			IndexedAndContainingEntity containing = session.load( IndexedAndContainingEntity.class, 1 );
			ContainedEntity contained = containing.getContained();
			containing.setContained( null );
			session.delete( contained );

			backendMock.expectWorks( IndexedAndContainingEntity.NAME )
					.addOrUpdate( "1", b -> b.field( "text", "initial" ) );
		} );
		backendMock.verifyExpectationsMet();

		assertIndexingCountAcrossAllSessionFactories().isEqualTo( 3 );
	}

	@Test
	public void uniformWorkDistribution_insertUpdateDelete_indexed() {
		SessionFactory sessionFactory = sessionFactories.get( 0 );

		int entityCount = 1000;

		// A single big insert transaction
		withinTransaction( sessionFactory, session -> {
			for ( int i = 0; i < entityCount; i++ ) {
				IndexedEntity entity = new IndexedEntity( i, "initial" );
				session.save( entity );

				backendMock.expectWorks( IndexedEntity.NAME )
						.add( String.valueOf( i ), b -> b.field( "text", "initial" ) );
			}
		} );
		backendMock.verifyExpectationsMet();
		// All works must be executed exactly once
		assertIndexingCountAcrossAllSessionFactories().isEqualTo( entityCount );
		// The workload must be spread uniformly (with some tolerance)
		assertIndexingCountForEachSessionFactory()
				.allSatisfy( count -> assertThat( count )
						.isCloseTo( entityCount / totalShardCount, Percentage.withPercentage( 25 ) ) );

		counters.clear();

		// Many small update transactions
		int batchSize = 100;
		for ( int i = 0; i < entityCount; i += batchSize ) {
			int idStart = i;
			int idEnd = Math.min( i + batchSize, entityCount );
			withinTransaction( sessionFactory, session -> {
				for ( int j = idStart; j < idEnd ; j++ ) {
					IndexedEntity entity = session.load( IndexedEntity.class, j );
					entity.setText( "updated" );

					backendMock.expectWorks( IndexedEntity.NAME )
							.addOrUpdate( String.valueOf( j ), b -> b.field( "text", "updated" ) );
				}
			} );
		}
		backendMock.verifyExpectationsMet();
		// All works must be executed exactly once
		assertIndexingCountAcrossAllSessionFactories().isEqualTo( entityCount );
		// The workload must be spread uniformly (with some tolerance)
		assertIndexingCountForEachSessionFactory()
				.allSatisfy( count -> assertThat( count )
						.isCloseTo( entityCount / totalShardCount, Percentage.withPercentage( 25 ) ) );
	}

	private AbstractIntegerAssert<?> assertIndexingCountAcrossAllSessionFactories() {
		int sum = 0;
		for ( StaticCounters.Key counterKey : sessionFactoryCounterKeys ) {
			sum += counters.get( counterKey );
		}
		log.debugf( "Count of indexing operations across all session factories: %s", sum );
		return assertThat( sum )
				.as( "Count of indexing operations across all session factories" );
	}

	private ListAssert<Integer> assertIndexingCountForEachSessionFactory() {
		List<Integer> counts = sessionFactoryCounterKeys.stream().map( counters::get ).collect( Collectors.toList() );
		log.debugf( "Count of indexing operations for each session factory: %s", counts );
		return assertThat( counts )
				.as( "Count of indexing operations for each session factory" );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	@TypeBinding(binder = @TypeBinderRef(type = PerSessionFactoryIndexingTracingBridge.Binder.class))
	public static class IndexedEntity {

		static final String NAME = "IndexedEntity";

		@Id
		private Integer id;
		@FullTextField
		private String text;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	@Entity(name = IndexedAndContainingEntity.NAME)
	@Indexed
	@TypeBinding(binder = @TypeBinderRef(type = PerSessionFactoryIndexingTracingBridge.Binder.class))
	public static class IndexedAndContainingEntity {

		static final String NAME = "IndexedAndContainingEntity";

		@Id
		private Integer id;
		@FullTextField
		private String text;
		@OneToOne(mappedBy = "containing")
		@IndexedEmbedded(includePaths = "text")
		private ContainedEntity contained;

		public IndexedAndContainingEntity() {
		}

		public IndexedAndContainingEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public ContainedEntity getContained() {
			return contained;
		}

		public void setContained(
				ContainedEntity contained) {
			this.contained = contained;
		}

	}

	@Entity(name = ContainedEntity.NAME)
	public static class ContainedEntity {

		static final String NAME = "ContainedEntity";

		@Id
		private Integer id;
		@FullTextField
		private String text;
		@OneToOne
		private IndexedAndContainingEntity containing;

		public ContainedEntity() {
		}

		public ContainedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public IndexedAndContainingEntity getContaining() {
			return containing;
		}

		public void setContaining(
				IndexedAndContainingEntity containing) {
			this.containing = containing;
		}
	}

}
