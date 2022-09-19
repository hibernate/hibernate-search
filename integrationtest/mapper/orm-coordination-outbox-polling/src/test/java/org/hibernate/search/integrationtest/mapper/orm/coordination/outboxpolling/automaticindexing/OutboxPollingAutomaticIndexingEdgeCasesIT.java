/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util.OutboxEventFilter;
import org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util.TestingOutboxPollingInternalConfigurer;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.impl.HibernateOrmMapperOutboxPollingImplSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * Extensive tests with edge cases for automatic indexing with the outbox-polling strategy.
 */
public class OutboxPollingAutomaticIndexingEdgeCasesIT {

	private static final OutboxEventFilter eventFilter = new OutboxEventFilter();

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class )
				.objectField( "contained", b2 -> b2
						.multiValued( true )
						.field( "text", String.class ) ) );
		backendMock.expectSchema( IndexedAndContainedEntity.NAME, b -> b
				.field( "text", String.class ) );
		setupContext
				.withProperty( HibernateOrmMapperOutboxPollingImplSettings.COORDINATION_INTERNAL_CONFIGURER,
						new TestingOutboxPollingInternalConfigurer().outboxEventFilter( eventFilter ) )
				.withAnnotatedTypes( IndexedEntity.class, IndexedAndContainedEntity.class );
	}

	@Before
	public void resetFilter() {
		eventFilter.reset();
		// Disable the filter by default: only some of the tests actually need it.
		eventFilter.showAllEvents();
	}

	@Test
	public void multipleChangesSameTransaction() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "initialValue I" );
			session.persist( entity1 );

			IndexedEntity entity2 = new IndexedEntity( 2, "initialValue I" );
			session.persist( entity2 );

			IndexedEntity entity3 = new IndexedEntity( 3, "initialValue I" );
			session.persist( entity3 );

			entity1.setText( "initialValue II" );
			entity2.setText( "initialValue II" );
			entity3.setText( "initialValue II" );
			session.flush();

			entity1.setText( "initialValue III" );
			entity2.setText( "initialValue III" );
			entity3.setText( "initialValue III" );
			session.flush();

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.field( "text", "initialValue III" )
					)
					.add( "2", b -> b
							.field( "text", "initialValue III" )
					)
					.add( "3", b -> b
							.field( "text", "initialValue III" )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void massiveInsert() {
		for ( int i = 0; i < 5; i++ ) {
			int finalI = i;
			setupHolder.runInTransaction( session -> {
				BackendMock.DocumentWorkCallListContext context = backendMock.expectWorks( IndexedEntity.NAME );

				for ( int j = 0; j < 500; j++ ) {
					int id = finalI * 500 + j;

					IndexedEntity entity = new IndexedEntity( id, "indexed value: " + id );
					session.persist( entity );

					if ( j % 25 == 0 ) {
						session.flush();
						session.clear();
					}

					context.add( id + "", b -> b.field( "text", "indexed value: " + id ) );
				}
			} );
			backendMock.verifyExpectationsMet();
		}
	}

	@Test
	public void addIndexedAndContained_addAndUpdateEventsProcessedInDifferentBatches() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity containing = new IndexedEntity( 1, "initialValue" );
			session.persist( containing );
			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.field( "text", "initialValue" ) );
		} );
		backendMock.verifyExpectationsMet();

		eventFilter.hideAllEvents();

		setupHolder.runInTransaction( session -> {
			IndexedEntity containing = session.getReference( IndexedEntity.class, 1 );
			IndexedAndContainedEntity contained = new IndexedAndContainedEntity( 2, "initialValue" );
			containing.getContained().add( contained );
			contained.setContaining( containing );
			session.persist( contained );

			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( "1", b -> b
							.field( "text", "initialValue" )
							.objectField( "contained", b2 -> b2
									.field( "text", "initialValue" ) ) );
			backendMock.expectWorks( IndexedAndContainedEntity.NAME )
					.add( "2", b -> b
							.field( "text", "initialValue" ) );
		} );

		// Make events visible one by one, so that they are processed in separate batches.
		List<UUID> eventIds = setupHolder.applyInTransaction( eventFilter::findOutboxEventIdsNoFilter );
		assertThat( eventIds ).hasSize( 2 );
		for ( UUID eventId : eventIds ) {
			eventFilter.showOnlyEvents( Collections.singletonList( eventId ) );
			eventFilter.awaitUntilNoMoreVisibleEvents( setupHolder.sessionFactory() );
		}

		// If everything goes well, the above will have executed exactly one add work for "contained"
		// and one addOrUpdate work for "containing".
		// What we want to avoid here is that the "contained created" event triggers
		// reindexing of "containing", which would be wrong because "containing"
		// will already be reindexed when the "containing updated" event is processed.
		// If that happens, there will be a duplicate "addOrUpdate" event and this assertion will fail.
		backendMock.verifyExpectationsMet();
	}


	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {
		static final String NAME = "Indexed";

		@Id
		private Integer id;
		@KeywordField
		private String text;
		@OneToMany(mappedBy = "containing")
		@IndexedEmbedded
		private List<IndexedAndContainedEntity> contained;

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

		public List<IndexedAndContainedEntity> getContained() {
			return contained;
		}
	}

	@Entity(name = IndexedAndContainedEntity.NAME)
	@Indexed
	public static class IndexedAndContainedEntity {
		static final String NAME = "IndexedAndContained";

		@Id
		private Integer id;
		@KeywordField
		private String text;
		@OneToOne
		private IndexedEntity containing;

		public IndexedAndContainedEntity() {
		}

		public IndexedAndContainedEntity(Integer id, String text) {
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

		public IndexedEntity getContaining() {
			return containing;
		}

		public void setContaining(IndexedEntity containing) {
			this.containing = containing;
		}
	}

}
