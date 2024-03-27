/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.session;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test usage of the session indexing plan with an entity type whose document ID is not the entity ID.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchIndexingPlanNonEntityIdDocumentIdIT {

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );
	private SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		backendMock.expectAnySchema( IndexedEntity.INDEX_NAME );

		sessionFactory = ormSetupHelper.start().withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false )
				.withAnnotatedTypes( IndexedEntity.class )
				.setup();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3203")
	void simple() {
		with( sessionFactory ).runInTransaction( session -> {
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
