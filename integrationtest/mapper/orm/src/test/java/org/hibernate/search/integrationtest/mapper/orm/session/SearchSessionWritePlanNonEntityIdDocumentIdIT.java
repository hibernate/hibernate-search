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
import org.hibernate.search.mapper.orm.cfg.HibernateOrmAutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSessionWritePlan;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test usage of the session write plan with an entity type whose document ID is not the entity ID.
 */
public class SearchSessionWritePlanNonEntityIdDocumentIdIT {

	private static final String BACKEND_NAME = "stubBackend";

	@Rule
	public BackendMock backendMock = new BackendMock( BACKEND_NAME );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3203")
	public void simple() {
		SessionFactory sessionFactory = setup();

		withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, 41, "number1" );
			IndexedEntity entity2 = new IndexedEntity( 2, 42, "number2" );
			IndexedEntity entity3 = new IndexedEntity( 3, 43, "number3" );

			session.persist( entity1 );
			session.persist( entity2 );
			session.persist( entity3 );

			SearchSessionWritePlan writePlan = Search.session( session ).writePlan();
			writePlan.addOrUpdate( entity1 );
			writePlan.addOrUpdate( entity2 );
			writePlan.delete( entity3 );
			writePlan.purge( IndexedEntity.class, 47 ); // Does not exist in database, but may exist in the index

			backendMock.expectWorks( IndexedEntity.INDEX_NAME )
					.update( "41", b -> b.field( "text", "number1" ) )
					.update( "42", b -> b.field( "text", "number2" ) )
					.delete( "43" )
					.delete( "47" )
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	private SessionFactory setup() {
		backendMock.expectAnySchema( IndexedEntity.INDEX_NAME );

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_STRATEGY,
						HibernateOrmAutomaticIndexingStrategyName.NONE
				)
				.setup( IndexedEntity.class );

		backendMock.verifyExpectationsMet();

		return sessionFactory;
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
