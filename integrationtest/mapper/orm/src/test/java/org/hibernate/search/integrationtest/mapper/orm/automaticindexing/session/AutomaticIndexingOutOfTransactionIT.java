/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.session;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-3068")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutomaticIndexingOutOfTransactionIT {

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );
	private SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		backendMock.expectAnySchema( IndexedEntity.INDEX_NAME );
		sessionFactory = ormSetupHelper.start()
				.withProperty( AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION, true )
				.withAnnotatedTypes( IndexedEntity.class )
				.setup();
	}

	@Test
	void add() {
		with( sessionFactory ).runNoTransaction( session -> {
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
	void clear() {
		with( sessionFactory ).runNoTransaction( session -> {
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
