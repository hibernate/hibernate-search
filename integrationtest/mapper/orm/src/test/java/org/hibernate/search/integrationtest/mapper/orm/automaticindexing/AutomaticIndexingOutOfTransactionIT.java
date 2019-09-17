/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.hibernate.search.util.impl.integrationtest.orm.OrmUtils.withinSession;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-3068")
public class AutomaticIndexingOutOfTransactionIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "myBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMocks( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void before() {
		backendMock.expectAnySchema( IndexedEntity.INDEX_NAME );
		sessionFactory = ormSetupHelper
				.start()
				.withProperty( AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION, true )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void add() {
		withinSession( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "number1" );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX_NAME )
					.add( "1", b -> b.field( "text", "number1" ) )
					.processedThenExecuted();

			// Flush without a transaction acts as a commit.
			// So all the involved works here are supposed to be both prepared and executed,
			// during the flush.
			session.flush();
			backendMock.verifyExpectationsMet();
		} );
	}

	@Test
	public void clear() {
		withinSession( sessionFactory, session -> {
			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();

			IndexedEntity entity1 = new IndexedEntity( 1, "number1" );
			// working directly on the indexing plan to add works immediately (not at flush time!)
			indexingPlan.addOrUpdate( entity1 );

			// clearing the update of entity 1
			session.clear();

			IndexedEntity entity2 = new IndexedEntity( 2, "number2" );
			indexingPlan.addOrUpdate( entity2 );

			// only entity 2 is supposed to be flushed here
			backendMock.expectWorks( IndexedEntity.INDEX_NAME )
					.update( "2", b -> b.field( "text", "number2" ) )
					.processedThenExecuted();

			session.flush();
		} );
	}

	@Entity(name = "IndexedEntity")
	@Indexed(index = IndexedEntity.INDEX_NAME)
	public static class IndexedEntity {
		static final String INDEX_NAME = "indexName";

		@Id
		private Integer id;
		@GenericField
		private String text;

		IndexedEntity(int id, String text) {
			this.id = id;
			this.text = text;
		}
	}
}
