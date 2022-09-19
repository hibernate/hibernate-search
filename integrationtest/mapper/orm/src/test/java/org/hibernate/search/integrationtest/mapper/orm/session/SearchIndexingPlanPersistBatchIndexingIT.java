/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.session;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * Test batch indexing relying not on the {@link org.hibernate.search.mapper.orm.massindexing.MassIndexer},
 * but on automatic indexing and on the {@link SearchIndexingPlan},
 * flushing and clearing the session periodically.
 * <p>
 * This is mostly useful when inserting lots of data into the database using JPA.
 */
@TestForIssue(jiraKey = "HSEARCH-3049")
public class SearchIndexingPlanPersistBatchIndexingIT {

	private static final int BATCH_SIZE = 100;
	// Make sure that entity count is not a multiple of batch size, to test for corner cases
	private static final int ENTITY_COUNT = BATCH_SIZE * 200 + BATCH_SIZE / 2;

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectAnySchema( IndexedEntity.INDEX_NAME );

		setupContext.withAnnotatedTypes( IndexedEntity.class );
	}

	/**
	 * Batch-index by simply processing entities before the session is cleared.
	 * This allows delaying indexing until after the transaction is committed,
	 * but requires a lot of memory (to store the indexing buffer).
	 */
	@Test
	public void processPerBatch() {
		setupHolder.runInTransaction( session -> {
			// This is for test only and wouldn't be present in real code
			int firstIdOfThisBatch = 0;

			for ( int i = 0; i < ENTITY_COUNT; i++ ) {
				if ( i > 0 && i % BATCH_SIZE == 0 ) {
					// For test only: register expectations regarding processing
					expectCreateAddWorks( firstIdOfThisBatch, i );
					firstIdOfThisBatch = i;

					session.flush();

					// For test only: check processing happens before the clear()
					backendMock.verifyExpectationsMet();

					session.clear();
				}

				IndexedEntity entity = new IndexedEntity( i, "number" + i );
				session.persist( entity );
			}

			// For test only: check on-commit creations/execution of works
			// If the last batch was smaller, there are still some works created on commit
			expectCreateAddWorks( firstIdOfThisBatch, ENTITY_COUNT );
			// Finally, the works will be executed on commit
			expectExecuteAddWorks( 0, ENTITY_COUNT );
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
		setupHolder.runInTransaction( session -> {
			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();

			// This is for test only and wouldn't be present in real code
			int firstIdOfThisBatch = 0;

			for ( int i = 0; i < ENTITY_COUNT; i++ ) {
				if ( i > 0 && i % BATCH_SIZE == 0 ) {
					expectCreateAddWorks( firstIdOfThisBatch, i );
					session.flush();
					backendMock.verifyExpectationsMet();

					// For test only: register expectations regarding execution
					expectExecuteAddWorks( firstIdOfThisBatch, i );
					firstIdOfThisBatch = i;

					indexingPlan.execute();

					// For test only: check execution happens before the clear()
					backendMock.verifyExpectationsMet();

					session.clear();
				}

				IndexedEntity entity = new IndexedEntity( i, "number" + i );
				session.persist( entity );
			}

			// For test only: check on-commit processing/execution of works
			// If the last batch was smaller, there is still some indexing that will occur on commit
			expectCreateAddWorks( firstIdOfThisBatch, ENTITY_COUNT );
			expectExecuteAddWorks( firstIdOfThisBatch, ENTITY_COUNT );
		} );
		// This is for test only and wouldn't be present in real code
		backendMock.verifyExpectationsMet();
	}

	private void expectCreateAddWorks(int firstId, int afterLastId) {
		BackendMock.DocumentWorkCallListContext expectations = backendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.createFollowingWorks();
		for ( int i = firstId; i < afterLastId; ++i ) {
			final int id = i;
			expectations.add( String.valueOf( id ), b -> b.field( "text", "number" + id ) );
		}
	}

	private void expectExecuteAddWorks(int firstId, int afterLastId) {
		BackendMock.DocumentWorkCallListContext expectations = backendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.executeFollowingWorks();
		for ( int i = firstId; i < afterLastId; ++i ) {
			final int id = i;
			expectations.add( String.valueOf( id ), b -> b.field( "text", "number" + id ) );
		}
	}

	@Entity(name = "indexed1")
	@Indexed(index = IndexedEntity.INDEX_NAME)
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
