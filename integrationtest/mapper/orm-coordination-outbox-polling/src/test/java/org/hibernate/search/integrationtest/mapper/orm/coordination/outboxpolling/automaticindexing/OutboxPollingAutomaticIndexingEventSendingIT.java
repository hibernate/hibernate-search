/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.FilteringOutboxEventFinder;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.avro.impl.EventPayloadSerializationUtils;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

public class OutboxPollingAutomaticIndexingEventSendingIT {

	private static final FilteringOutboxEventFinder outboxEventFinder = new FilteringOutboxEventFinder();

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext, ReusableOrmSetupHolder.DataClearConfig dataClearConfig) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
		);
		backendMock.expectSchema( AnotherIndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
		);
		backendMock.expectSchema( RoutedIndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
		);
		backendMock.expectSchema( IndexedAndContainingEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
				.objectField( "contained", b2 -> b2
						.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) ) )
				.objectField( "indexedAndContained", b2 -> b2
						.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) ) )
		);
		backendMock.expectSchema( IndexedAndContainedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
				.field( "nonIndexedEmbeddedText", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
		);

		setupContext.withProperty( "hibernate.search.coordination.outbox_event_finder.provider", outboxEventFinder.provider() )
				// use timebase uuids to get predictable sorting order
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.uuid_gen_strategy", "time" )
				.withAnnotatedTypes( IndexedEntity.class, AnotherIndexedEntity.class, RoutedIndexedEntity.class,
						IndexedAndContainingEntity.class, ContainedEntity.class, IndexedAndContainedEntity.class );

		dataClearConfig.clearOrder( ContainedEntity.class, IndexedAndContainedEntity.class,
				IndexedAndContainingEntity.class );
	}

	@Before
	public void resetFilter() {
		outboxEventFinder.reset();
	}

	@Test
	public void insertUpdateDelete_indexed() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity indexedPojo = new IndexedEntity( 1, "Using some text here" );
			session.persist( indexedPojo );
		} );

		setupHolder.runInTransaction( session -> {
			List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );

			assertThat( outboxEntries ).hasSize( 1 );
			verifyOutboxEntry( outboxEntries.get( 0 ), IndexedEntity.NAME, "1", null );
		} );

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity = session.getReference( IndexedEntity.class, 1 );
			entity.setText( "Change the test of this entity!" );
		} );

		setupHolder.runInTransaction( session -> {
			List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );

			assertThat( outboxEntries ).hasSize( 2 );
			verifyOutboxEntry( outboxEntries.get( 1 ), IndexedEntity.NAME, "1", null );
		} );

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity = session.getReference( IndexedEntity.class, 1 );
			session.remove( entity );
		} );

		setupHolder.runInTransaction( session -> {
			List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );

			assertThat( outboxEntries ).hasSize( 3 );
			verifyOutboxEntry( outboxEntries.get( 2 ), IndexedEntity.NAME, "1", null );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4141")
	public void insertUpdateDelete_contained() {
		setupHolder.runInTransaction( session -> {
			IndexedAndContainingEntity containing = new IndexedAndContainingEntity( 1, "initial" );
			ContainedEntity contained = new ContainedEntity( 2, "initial" );
			containing.setContained( contained );
			contained.setContaining( containing );
			session.persist( containing );
			session.persist( contained );
		} );

		setupHolder.runInTransaction( session -> {
			List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );

			// There *is* an event when a contained entity is created,
			// in order to support implicit association updates (see HSEARCH-4303).
			assertThat( outboxEntries ).hasSize( 2 );
			verifyOutboxEntry( outboxEntries.get( 0 ), ContainedEntity.NAME, "2", null );
			verifyOutboxEntry( outboxEntries.get( 1 ), IndexedAndContainingEntity.NAME, "1", null );
		} );

		setupHolder.runInTransaction( session -> {
			ContainedEntity entity = session.getReference( ContainedEntity.class, 2 );
			entity.setText( "updated" );
		} );

		setupHolder.runInTransaction( session -> {
			List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );

			assertThat( outboxEntries ).hasSize( 3 );
			verifyOutboxEntry( outboxEntries.get( 2 ), ContainedEntity.NAME, "2", null );
		} );

		setupHolder.runInTransaction( session -> {
			ContainedEntity entity = session.getReference( ContainedEntity.class, 2 );
			session.remove( entity );
		} );

		setupHolder.runInTransaction( session -> {
			List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );

			// When a contained entity is deleted,
			// reindexing resolution is performed in the original session,
			// resulting in events for containing entities.
			assertThat( outboxEntries ).hasSize( 4 );
			verifyOutboxEntry( outboxEntries.get( 3 ), IndexedAndContainingEntity.NAME, "1", null );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4141")
	public void insertUpdateDelete_indexedAndContained() {
		setupHolder.runInTransaction( session -> {
			IndexedAndContainingEntity containing = new IndexedAndContainingEntity( 1, "initial" );
			IndexedAndContainedEntity indexedAndContained = new IndexedAndContainedEntity( 2, "initial" );
			containing.setIndexedAndContained( indexedAndContained );
			indexedAndContained.setContaining( containing );
			session.persist( containing );
			session.persist( indexedAndContained );
		} );

		setupHolder.runInTransaction( session -> {
			List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );

			assertThat( outboxEntries ).hasSize( 2 );
			verifyOutboxEntry( outboxEntries.get( 0 ), IndexedAndContainingEntity.NAME, "1", null );
			verifyOutboxEntry( outboxEntries.get( 1 ), IndexedAndContainedEntity.NAME, "2", null );
		} );

		setupHolder.runInTransaction( session -> {
			IndexedAndContainedEntity entity = session.getReference( IndexedAndContainedEntity.class, 2 );
			entity.setText( "updated" );
		} );

		setupHolder.runInTransaction( session -> {
			List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );

			assertThat( outboxEntries ).hasSize( 3 );
			verifyOutboxEntry( outboxEntries.get( 2 ), IndexedAndContainedEntity.NAME, "2", null );
		} );

		setupHolder.runInTransaction( session -> {
			IndexedAndContainedEntity entity = session.getReference( IndexedAndContainedEntity.class, 2 );
			session.remove( entity );
		} );

		setupHolder.runInTransaction( session -> {
			List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );

			assertThat( outboxEntries ).hasSize( 5 );
			verifyOutboxEntry( outboxEntries.get( 3 ), IndexedAndContainedEntity.NAME, "2", null );
			// Since HSEARCH-4303, we resolve reindexing in the original session for deleted entities,
			// in order to handle implicit association updates through deletions.
			// This leads to creating reindexing events for containing entities.
			verifyOutboxEntry( outboxEntries.get( 4 ), IndexedAndContainingEntity.NAME, "1", null );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4141")
	public void updateIndexedEmbeddedField_contained() {
		setupHolder.runInTransaction( session -> {
			IndexedAndContainingEntity containing = new IndexedAndContainingEntity( 1, "initial" );
			ContainedEntity contained = new ContainedEntity( 2, "initial" );
			containing.setContained( contained );
			contained.setContaining( containing );
			session.persist( containing );
			session.persist( contained );
		} );

		// Wait for insert processing to happen
		backendMock.expectWorks( IndexedAndContainingEntity.NAME )
				.add( "1", b -> b
						.field( "text", "initial" )
						.objectField( "contained", b2 -> b2
								.field( "text", "initial" ) ) );
		outboxEventFinder.showAllEventsUpToNow( setupHolder.sessionFactory() );
		backendMock.verifyExpectationsMet();
		// Processing the insert events shouldn't yield more events
		backendMock.indexingWorkExpectations().awaitIndexingAssertions( () -> {
			setupHolder.runInTransaction( session -> {
				List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );
				assertThat( outboxEntries ).isEmpty();
			} );
		} );

		setupHolder.runInTransaction( session -> {
			ContainedEntity entity = session.getReference( ContainedEntity.class, 2 );
			entity.setText( "updated" );
		} );

		// Processing the update event should yield more events for containing entities
		outboxEventFinder.showAllEventsUpToNow( setupHolder.sessionFactory() );
		backendMock.indexingWorkExpectations().awaitIndexingAssertions( () -> {
			setupHolder.runInTransaction( session -> {
				List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );
				assertThat( outboxEntries ).hasSize( 1 );
				verifyOutboxEntry( outboxEntries.get( 0 ), IndexedAndContainingEntity.NAME, "1", null );
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4141")
	public void updateIndexedEmbeddedField_indexedAndContained() {
		setupHolder.runInTransaction( session -> {
			IndexedAndContainingEntity containing = new IndexedAndContainingEntity( 1, "initial" );
			IndexedAndContainedEntity contained = new IndexedAndContainedEntity( 2, "initial" );
			containing.setIndexedAndContained( contained );
			contained.setContaining( containing );
			session.persist( containing );
			session.persist( contained );
		} );

		// Wait for insert processing to happen
		backendMock.expectWorks( IndexedAndContainingEntity.NAME )
				.add( "1", b -> b
						.field( "text", "initial" )
						.objectField( "indexedAndContained", b2 -> b2
								.field( "text", "initial" ) ) );
		backendMock.expectWorks( IndexedAndContainedEntity.NAME )
				.add( "2", b -> b
						.field( "text", "initial" )
						.field( "nonIndexedEmbeddedText", "initial" ) );
		outboxEventFinder.showAllEventsUpToNow( setupHolder.sessionFactory() );
		backendMock.verifyExpectationsMet();
		// Processing the insert events shouldn't yield more events
		backendMock.indexingWorkExpectations().awaitIndexingAssertions( () -> {
			setupHolder.runInTransaction( session -> {
				List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );
				assertThat( outboxEntries ).isEmpty();
			} );
		} );

		setupHolder.runInTransaction( session -> {
			IndexedAndContainedEntity entity = session.getReference( IndexedAndContainedEntity.class, 2 );
			entity.setText( "updated" );
		} );

		backendMock.expectWorks( IndexedAndContainedEntity.NAME )
				.addOrUpdate( "2", b -> b
						.field( "text", "updated" )
						.field( "nonIndexedEmbeddedText", "initial" ) );
		outboxEventFinder.showAllEventsUpToNow( setupHolder.sessionFactory() );
		backendMock.verifyExpectationsMet();
		// Processing the update event should yield more events for containing entities
		backendMock.indexingWorkExpectations().awaitIndexingAssertions( () -> {
			setupHolder.runInTransaction( session -> {
				List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );
				assertThat( outboxEntries ).hasSize( 1 );
				verifyOutboxEntry( outboxEntries.get( 0 ), IndexedAndContainingEntity.NAME, "1", null );
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4141")
	public void updateNonIndexedEmbeddedField_contained() {
		setupHolder.runInTransaction( session -> {
			IndexedAndContainingEntity containing = new IndexedAndContainingEntity( 1, "initial" );
			ContainedEntity contained = new ContainedEntity( 2, "initial" );
			containing.setContained( contained );
			contained.setContaining( containing );
			session.persist( containing );
			session.persist( contained );
		} );

		// Wait for insert processing to happen
		backendMock.expectWorks( IndexedAndContainingEntity.NAME )
				.add( "1", b -> b
						.field( "text", "initial" )
						.objectField( "contained", b2 -> b2
								.field( "text", "initial" ) ) );
		outboxEventFinder.showAllEventsUpToNow( setupHolder.sessionFactory() );
		backendMock.verifyExpectationsMet();
		// Processing the insert events shouldn't yield more events
		backendMock.indexingWorkExpectations().awaitIndexingAssertions( () -> {
			setupHolder.runInTransaction( session -> {
				List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );
				assertThat( outboxEntries ).isEmpty();
			} );
		} );

		setupHolder.runInTransaction( session -> {
			ContainedEntity entity = session.getReference( ContainedEntity.class, 2 );
			entity.setNonIndexedEmbeddedText( "updated" );
		} );

		// Processing this update event shouldn't yield more events,
		// because the changed field is not indexed-embedded.
		outboxEventFinder.showAllEventsUpToNow( setupHolder.sessionFactory() );
		backendMock.indexingWorkExpectations().awaitIndexingAssertions( () -> {
			setupHolder.runInTransaction( session -> {
				List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );
				assertThat( outboxEntries ).isEmpty();
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4141")
	public void updateNonIndexedEmbeddedField_indexedAndContained() {
		setupHolder.runInTransaction( session -> {
			IndexedAndContainingEntity containing = new IndexedAndContainingEntity( 1, "initial" );
			IndexedAndContainedEntity contained = new IndexedAndContainedEntity( 2, "initial" );
			containing.setIndexedAndContained( contained );
			contained.setContaining( containing );
			session.persist( containing );
			session.persist( contained );
		} );

		// Wait for insert processing to happen
		backendMock.expectWorks( IndexedAndContainingEntity.NAME )
				.add( "1", b -> b
						.field( "text", "initial" )
						.objectField( "indexedAndContained", b2 -> b2
								.field( "text", "initial" ) ) );
		backendMock.expectWorks( IndexedAndContainedEntity.NAME )
				.add( "2", b -> b
						.field( "text", "initial" )
						.field( "nonIndexedEmbeddedText", "initial" ) );
		outboxEventFinder.showAllEventsUpToNow( setupHolder.sessionFactory() );
		backendMock.verifyExpectationsMet();
		// Processing the insert events shouldn't yield more events
		backendMock.indexingWorkExpectations().awaitIndexingAssertions( () -> {
			setupHolder.runInTransaction( session -> {
				List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );
				assertThat( outboxEntries ).isEmpty();
			} );
		} );

		setupHolder.runInTransaction( session -> {
			IndexedAndContainedEntity entity = session.getReference( IndexedAndContainedEntity.class, 2 );
			entity.setNonIndexedEmbeddedText( "updated" );
		} );

		backendMock.expectWorks( IndexedAndContainedEntity.NAME )
				.addOrUpdate( "2", b -> b
						.field( "text", "initial" )
						.field( "nonIndexedEmbeddedText", "updated" ) );
		outboxEventFinder.showAllEventsUpToNow( setupHolder.sessionFactory() );
		backendMock.verifyExpectationsMet();
		// Processing this update event shouldn't yield more events,
		// because the changed field is not indexed-embedded.
		backendMock.indexingWorkExpectations().awaitIndexingAssertions( () -> {
			setupHolder.runInTransaction( session -> {
				List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );
				assertThat( outboxEntries ).isEmpty();
			} );
		} );
	}

	@Test
	public void multipleInstances() {
		for ( int i = 1; i <= 7; i++ ) {
			IndexedEntity indexedPojo = new IndexedEntity( i, "Using some text here" );
			setupHolder.runInTransaction( session -> {
				session.persist( indexedPojo );
			} );
		}

		setupHolder.runInTransaction( session -> {
			List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );

			assertThat( outboxEntries ).hasSize( 7 );
			for ( int i = 0; i < 7; i++ ) {
				verifyOutboxEntry( outboxEntries.get( i ), IndexedEntity.NAME, ( i + 1 ) + "", null );
			}
		} );
	}

	@Test
	public void multipleTypes() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity indexedPojo = new IndexedEntity( 1, "Using some text here" );
			session.persist( indexedPojo );
		} );

		setupHolder.runInTransaction( session -> {
			AnotherIndexedEntity indexedPojo = new AnotherIndexedEntity( 1, "Using some text here" );
			session.persist( indexedPojo );
		} );

		setupHolder.runInTransaction( session -> {
			List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );

			assertThat( outboxEntries ).hasSize( 2 );
			verifyOutboxEntry( outboxEntries.get( 0 ), IndexedEntity.NAME, "1", null );
			verifyOutboxEntry( outboxEntries.get( 1 ), AnotherIndexedEntity.NAME, "1", null );
		} );
	}

	@Test
	public void routingKeys() {
		setupHolder.runInTransaction( session -> {
			RoutedIndexedEntity indexedPojo = new RoutedIndexedEntity( 1, "Using some text here",
					RoutedIndexedEntity.Status.FIRST );
			session.persist( indexedPojo );
		} );

		setupHolder.runInTransaction( session -> {
			List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );

			assertThat( outboxEntries ).hasSize( 1 );
			verifyOutboxEntry(
					outboxEntries.get( 0 ), RoutedIndexedEntity.NAME, "1",
					"FIRST" );
		} );

		setupHolder.runInTransaction( session -> {
			RoutedIndexedEntity entity = session.getReference( RoutedIndexedEntity.class, 1 );
			entity.setText( "Change the test of this entity!" );
			entity.setStatus( RoutedIndexedEntity.Status.SECOND );
		} );

		setupHolder.runInTransaction( session -> {
			List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );

			assertThat( outboxEntries ).hasSize( 2 );
			verifyOutboxEntry(
					outboxEntries.get( 1 ), RoutedIndexedEntity.NAME, "1",
					"SECOND",
					"FIRST" ); // previous routing keys
		} );
	}

	static void verifyOutboxEntry(OutboxEvent outboxEvent, String entityName, String entityId,
			String currentRouteRoutingKey, String... previousRoutesRoutingKeys) {
		assertSoftly( softly -> {
			softly.assertThat( outboxEvent.getEntityName() ).isEqualTo( entityName );
			softly.assertThat( outboxEvent.getEntityId() ).isEqualTo( entityId );

			PojoIndexingQueueEventPayload payload = EventPayloadSerializationUtils.deserialize( outboxEvent.getPayload() );
			DocumentRoutesDescriptor routesDescriptor = payload.routes;

			softly.assertThat( routesDescriptor ).isNotNull();
			softly.assertThat( routesDescriptor.currentRoute().routingKey() ).isEqualTo( currentRouteRoutingKey );

			Collection<DocumentRouteDescriptor> descriptors = Arrays.stream( previousRoutesRoutingKeys )
					.map( DocumentRouteDescriptor::of )
					.collect( Collectors.toCollection( LinkedHashSet::new ) );

			softly.assertThat( routesDescriptor.previousRoutes() ).isEqualTo( descriptors );
		} );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
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

	@Entity(name = AnotherIndexedEntity.NAME)
	@Indexed
	public static class AnotherIndexedEntity {

		static final String NAME = "AnotherIndexedEntity";

		@Id
		private Integer id;
		@FullTextField
		private String text;

		public AnotherIndexedEntity() {
		}

		public AnotherIndexedEntity(Integer id, String text) {
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
	public static class IndexedAndContainingEntity {

		static final String NAME = "IndexedAndContainingEntity";

		@Id
		private Integer id;
		@FullTextField
		private String text;
		@OneToOne(mappedBy = "containing")
		@IndexedEmbedded(includePaths = "text")
		private ContainedEntity contained;
		@OneToOne(mappedBy = "containing")
		@IndexedEmbedded(includePaths = "text")
		private IndexedAndContainedEntity indexedAndContained;

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

		public IndexedAndContainedEntity getIndexedAndContained() {
			return indexedAndContained;
		}

		public void setIndexedAndContained(
				IndexedAndContainedEntity indexedAndContained) {
			this.indexedAndContained = indexedAndContained;
		}
	}

	@Entity(name = ContainedEntity.NAME)
	public static class ContainedEntity {

		static final String NAME = "ContainedEntity";

		@Id
		private Integer id;
		@FullTextField
		private String text;
		@FullTextField
		private String nonIndexedEmbeddedText;
		@OneToOne
		private IndexedAndContainingEntity containing;

		public ContainedEntity() {
		}

		public ContainedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
			this.nonIndexedEmbeddedText = text;
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

		public String getNonIndexedEmbeddedText() {
			return nonIndexedEmbeddedText;
		}

		public void setNonIndexedEmbeddedText(String nonIndexedEmbeddedText) {
			this.nonIndexedEmbeddedText = nonIndexedEmbeddedText;
		}

		public IndexedAndContainingEntity getContaining() {
			return containing;
		}

		public void setContaining(
				IndexedAndContainingEntity containing) {
			this.containing = containing;
		}
	}

	@Entity(name = IndexedAndContainedEntity.NAME)
	@Indexed
	public static class IndexedAndContainedEntity {

		static final String NAME = "IndexedAndContainedEntity";

		@Id
		private Integer id;
		@FullTextField
		private String text;
		@FullTextField
		private String nonIndexedEmbeddedText;
		@OneToOne
		private IndexedAndContainingEntity containing;

		public IndexedAndContainedEntity() {
		}

		public IndexedAndContainedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
			this.nonIndexedEmbeddedText = text;
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

		public String getNonIndexedEmbeddedText() {
			return nonIndexedEmbeddedText;
		}

		public void setNonIndexedEmbeddedText(String nonIndexedEmbeddedText) {
			this.nonIndexedEmbeddedText = nonIndexedEmbeddedText;
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
