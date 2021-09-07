/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.coordination.databasepolling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Collections;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.coordination.CoordinationStrategyNames;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Extensive tests with edge cases for automatic indexing with {@link CoordinationStrategyNames#DATABASE_POLLING}.
 */
public class DatabasePollingAutomaticIndexingEdgeCasesIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	private final FilteringOutboxEventFinder outboxEventFinder = new FilteringOutboxEventFinder()
			// Disable the filter by default: only some of the tests actually need it.
			.enableFilter( false );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class )
				.objectField( "contained", b2 -> b2
						.field( "text", String.class ) ) );
		backendMock.expectSchema( IndexedAndContainedEntity.NAME, b -> b
				.field( "text", String.class ) );
		sessionFactory = ormSetupHelper.start()
				.withProperty(
						"hibernate.search.coordination.processors.indexing.outbox_event_finder.provider",
						outboxEventFinder.provider()
				)
				.setup( IndexedEntity.class, IndexedAndContainedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void multipleChangesSameTransaction() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "initialValue I" );
			session.persist( entity1 );

			IndexedEntity entity2 = new IndexedEntity( 2, "initialValue I" );
			session.persist( entity2 );

			IndexedEntity entity3 = new IndexedEntity( 3, "initialValue I" );
			session.persist( entity3 );

			entity1.setText( "initialValue II" );
			session.update( entity1 );
			entity2.setText( "initialValue II" );
			session.update( entity2 );
			entity3.setText( "initialValue II" );
			session.update( entity3 );

			entity1.setText( "initialValue III" );
			session.update( entity1 );
			entity2.setText( "initialValue III" );
			session.update( entity2 );
			entity3.setText( "initialValue III" );
			session.update( entity3 );

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
			OrmUtils.withinTransaction( sessionFactory, session -> {
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
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity containing = new IndexedEntity( 1, "initialValue" );
			session.persist( containing );
			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.field( "text", "initialValue" ) );
		} );
		backendMock.verifyExpectationsMet();

		outboxEventFinder.enableFilter( true );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity containing = session.load( IndexedEntity.class, 1 );
			IndexedAndContainedEntity contained = new IndexedAndContainedEntity( 2, "initialValue" );
			containing.setContained( contained );
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
		List<Long> eventIds = with( sessionFactory ).applyInTransaction( outboxEventFinder::findOutboxEventIdsNoFilter );
		assertThat( eventIds ).hasSize( 2 );
		for ( Long eventId : eventIds ) {
			outboxEventFinder.showOnlyEvents( Collections.singletonList( eventId ) );
			outboxEventFinder.awaitUntilNoMoreVisibleEvents( sessionFactory );
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
		@OneToOne(mappedBy = "containing")
		@IndexedEmbedded
		private IndexedAndContainedEntity contained;

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

		public IndexedAndContainedEntity getContained() {
			return contained;
		}

		public void setContained(IndexedAndContainedEntity contained) {
			this.contained = contained;
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
