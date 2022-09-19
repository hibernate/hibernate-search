/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.session;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
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

@TestForIssue(jiraKey = "HSEARCH-3068")
public class AutomaticIndexingOutOfTransactionIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectAnySchema( IndexedEntity.INDEX_NAME );
		setupContext.withProperty( AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION, true )
				.withAnnotatedTypes( IndexedEntity.class );
	}

	@Test
	public void add() {
		setupHolder.runNoTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "number1" );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX_NAME )
					.add( "1", b -> b.field( "text", "number1" ) );

			// Flush without a transaction acts as a commit.
			// So all the involved works here are supposed to be both prepared and executed,
			// during the flush.
			session.flush();
			backendMock.verifyExpectationsMet();
		} );
	}

	@Test
	public void clear() {
		setupHolder.runNoTransaction( session -> {
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
					.addOrUpdate( "2", b -> b.field( "text", "number2" ) );

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

		protected IndexedEntity() { // For ORM
		}

		IndexedEntity(int id, String text) {
			this.id = id;
			this.text = text;
		}
	}
}
