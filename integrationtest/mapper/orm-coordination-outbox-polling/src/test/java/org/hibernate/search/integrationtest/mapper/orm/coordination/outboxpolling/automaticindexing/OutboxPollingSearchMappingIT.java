/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;

import java.util.concurrent.CompletableFuture;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.OutboxPollingExtension;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.mapping.OutboxPollingSearchMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class OutboxPollingSearchMappingIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	private SessionFactory sessionFactory;
	private OutboxPollingSearchMapping searchMapping;

	@Before
	public void before() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b.field( "indexedField", String.class ) );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.event_processor.retry_delay", 0 )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
		searchMapping = Search.mapping( sessionFactory ).extension( OutboxPollingExtension.get() );
	}

	@Test
	public void clearAllAbortedEvents() {
		assertThat( searchMapping.countAbortedEvents() ).isZero();

		generateThreeAbortedEvents();

		assertThat( searchMapping.countAbortedEvents() ).isEqualTo( 3 );

		assertThat( searchMapping.clearAllAbortedEvents() ).isEqualTo( 3 );

		assertThat( searchMapping.countAbortedEvents() ).isZero();
	}

	@Test
	public void reprocessAbortedEvents() {
		assertThat( searchMapping.countAbortedEvents() ).isZero();

		generateThreeAbortedEvents();

		assertThat( searchMapping.countAbortedEvents() ).isEqualTo( 3 );

		backendMock.expectWorks( IndexedEntity.INDEX )
				.createAndExecuteFollowingWorks()
				.addOrUpdate( "1", b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( "2", b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( "3", b -> b
						.field( "indexedField", "initialValue" )
				);
		assertThat( searchMapping.reprocessAbortedEvents() ).isEqualTo( 3 );
		backendMock.verifyExpectationsMet();

		assertThat( searchMapping.countAbortedEvents() ).isZero();
	}

	private void generateThreeAbortedEvents() {
		withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );
			session.persist( entity1 );

			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setIndexedField( "initialValue" );
			session.persist( entity2 );

			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setIndexedField( "initialValue" );
			session.persist( entity3 );

			CompletableFuture<?> failingFuture = new CompletableFuture<>();
			failingFuture.completeExceptionally( new SimulatedFailure( "Indexing work #2 failed!" ) );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.createAndExecuteFollowingWorks( failingFuture )
					.add( "1", b -> b
							.field( "indexedField", "initialValue" )
					)
					.add( "2", b -> b
							.field( "indexedField", "initialValue" )
					)
					.add( "3", b -> b
							.field( "indexedField", "initialValue" )
					)
					// retry (fails too):
					.createAndExecuteFollowingWorks( failingFuture )
					.addOrUpdate( "1", b -> b
							.field( "indexedField", "initialValue" )
					)
					.addOrUpdate( "2", b -> b
							.field( "indexedField", "initialValue" )
					)
					.addOrUpdate( "3", b -> b
							.field( "indexedField", "initialValue" )
					)
					// retry (fails too):
					.createAndExecuteFollowingWorks( failingFuture )
					.addOrUpdate( "1", b -> b
							.field( "indexedField", "initialValue" )
					)
					.addOrUpdate( "2", b -> b
							.field( "indexedField", "initialValue" )
					)
					.addOrUpdate( "3", b -> b
							.field( "indexedField", "initialValue" )
					);
			// no more retry
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = IndexedEntity.INDEX)
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {
		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String indexedField;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String indexedField) {
			this.id = id;
			this.indexedField = indexedField;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}
	}

	private static class SimulatedFailure extends RuntimeException {
		SimulatedFailure(String message) {
			super( message );
		}
	}
}
