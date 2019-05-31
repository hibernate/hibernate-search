/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.session;

import static org.hibernate.search.util.impl.integrationtest.orm.OrmUtils.withinTransaction;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSessionWritePlan;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test batch indexing relying not on the {@link org.hibernate.search.mapper.orm.massindexing.MassIndexer},
 * but on automatic indexing and on the {@link SearchSessionWritePlan},
 * flushing and clearing the session periodically.
 * <p>
 * This is mostly useful when inserting lots of data into the database using JPA.
 */
@TestForIssue(jiraKey = "HSEARCH-3049")
public class SearchSessionWritePlanPersistBatchIndexingIT {

	private static final String BACKEND_NAME = "stubBackend";

	private static int BATCH_SIZE = 100;
	// Make sure that entity count is not a multiple of batch size, to test for corner cases
	private static int ENTITY_COUNT = BATCH_SIZE * 200 + BATCH_SIZE / 2;

	@Rule
	public BackendMock backendMock = new BackendMock( BACKEND_NAME );

	@Rule
	public OrmSetupHelper ormSetupHelper = new OrmSetupHelper();

	/**
	 * Batch-index by simply processing entities before the session is cleared.
	 * This allows delaying indexing until after the transaction is committed,
	 * but requires a lot of memory (to store the indexing buffer).
	 */
	@Test
	public void processPerBatch() {
		SessionFactory sessionFactory = setup();

		withinTransaction( sessionFactory, session -> {
			SearchSessionWritePlan writePlan = Search.getSearchSession( session ).writePlan();

			// This is for test only and wouldn't be present in real code
			int firstIdOfThisBatch = 0;

			for ( int i = 0; i < ENTITY_COUNT; i++ ) {
				if ( i > 0 && i % BATCH_SIZE == 0 ) {
					session.flush();

					// For test only: register expectations regarding processing
					expectAddWorks( firstIdOfThisBatch, i ).prepared();
					firstIdOfThisBatch = i;

					writePlan.process();

					// For test only: check processing happens before the clear()
					backendMock.verifyExpectationsMet();

					session.clear();
				}

				IndexedEntity entity = new IndexedEntity( i, "number" + i );
				session.persist( entity );
			}

			// For test only: check on-commit processing/execution of works
			// If the last batch was smaller, there is still some preparing that will occur on commit
			expectAddWorks( firstIdOfThisBatch, ENTITY_COUNT ).prepared();
			// Finally, the works will be executed on commit
			expectAddWorks( 0, ENTITY_COUNT ).executed();
		} );
		// This is for test only and wouldn't be present in real code
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Batch-index by simply indexing entities before the session is cleared.
	 * This allows keeping the memory footprint low,
	 * but a rollback of the transaction will leave the index out of sync.
	 */
	@Test
	public void executePerBatch() {
		SessionFactory sessionFactory = setup();

		withinTransaction( sessionFactory, session -> {
			SearchSessionWritePlan writePlan = Search.getSearchSession( session ).writePlan();

			// This is for test only and wouldn't be present in real code
			int firstIdOfThisBatch = 0;

			for ( int i = 0; i < ENTITY_COUNT; i++ ) {
				if ( i > 0 && i % BATCH_SIZE == 0 ) {
					session.flush();

					// For test only: register expectations regarding execution
					expectAddWorks( firstIdOfThisBatch, i ).preparedThenExecuted();
					firstIdOfThisBatch = i;

					writePlan.execute();

					// For test only: check execution happens before the clear()
					backendMock.verifyExpectationsMet();

					session.clear();
				}

				IndexedEntity entity = new IndexedEntity( i, "number" + i );
				session.persist( entity );
			}

			// For test only: check on-commit processing/execution of works
			// If the last batch was smaller, there is still some indexing that will occur on commit
			expectAddWorks( firstIdOfThisBatch, ENTITY_COUNT ).preparedThenExecuted();
		} );
		// This is for test only and wouldn't be present in real code
		backendMock.verifyExpectationsMet();
	}

	private BackendMock.DocumentWorkCallListContext expectAddWorks(int firstId, int afterLastId) {
		BackendMock.DocumentWorkCallListContext expectations = backendMock.expectWorks( IndexedEntity.INDEX_NAME );
		for ( int i = firstId; i < afterLastId; ++i ) {
			final int id = i;
			expectations.add( String.valueOf( id ), b -> b.field( "text", "number" + id ) );
		}
		return expectations;
	}

	private SessionFactory setup() {
		backendMock.expectAnySchema( IndexedEntity.INDEX_NAME );

		SessionFactory sessionFactory = ormSetupHelper.startSetup()
				.withBackendMock( backendMock )
				.setup( IndexedEntity.class );

		backendMock.verifyExpectationsMet();

		return sessionFactory;
	}

	@Entity(name = "indexed1")
	@Indexed(backend = BACKEND_NAME, index = IndexedEntity.INDEX_NAME)
	public static class IndexedEntity {

		static final String INDEX_NAME = "index1Name";

		@Id
		private Integer id;

		@GenericField
		private String text;

		protected IndexedEntity() {
			// For ORM
		}

		IndexedEntity(int id, String text) {
			this.id = id;
			this.text = text;
		}
	}
}
