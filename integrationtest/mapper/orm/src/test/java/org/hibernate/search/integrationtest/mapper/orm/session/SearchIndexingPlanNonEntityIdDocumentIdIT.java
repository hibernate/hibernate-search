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
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
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
 * Test usage of the session indexing plan with an entity type whose document ID is not the entity ID.
 */
public class SearchIndexingPlanNonEntityIdDocumentIdIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectAnySchema( IndexedEntity.INDEX_NAME );

		setupContext.withProperty( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLED, false )
				.withAnnotatedTypes( IndexedEntity.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3203")
	public void simple() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, 41, "number1" );
			IndexedEntity entity2 = new IndexedEntity( 2, 42, "number2" );
			IndexedEntity entity3 = new IndexedEntity( 3, 43, "number3" );

			session.persist( entity1 );
			session.persist( entity2 );
			session.persist( entity3 );

			SearchIndexingPlan indexingPlan = Search.session( session ).indexingPlan();
			indexingPlan.addOrUpdate( entity1 );
			indexingPlan.addOrUpdate( entity2 );
			indexingPlan.delete( entity3 );
			indexingPlan.purge( IndexedEntity.class, 47, null ); // Does not exist in database, but may exist in the index

			backendMock.expectWorks( IndexedEntity.INDEX_NAME )
					.addOrUpdate( "41", b -> b.field( "text", "number1" ) )
					.addOrUpdate( "42", b -> b.field( "text", "number2" ) )
					.delete( "43" )
					.delete( "47" );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX_NAME)
	public static class IndexedEntity {
		static final String INDEX_NAME = "IndexName";

		@Id
		private Integer id;

		@DocumentId
		private Integer documentId;

		@GenericField
		private String text;

		protected IndexedEntity() {
			// For ORM
		}

		IndexedEntity(int id, int documentId, String text) {
			this.id = id;
			this.documentId = documentId;
			this.text = text;
		}
	}
}
