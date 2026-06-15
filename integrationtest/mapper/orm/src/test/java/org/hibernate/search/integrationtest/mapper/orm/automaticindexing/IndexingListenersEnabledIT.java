/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test enabling/disabling indexing listeners.
 */
@TestForIssue(jiraKey = { "HSEARCH-4268", "HSEARCH-4616" })
class IndexingListenersEnabledIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory setup(Boolean enabled) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class )
		);

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, enabled )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
		return sessionFactory;
	}

	@Test
	void enabled_default() {
		SessionFactory sessionFactory = setup( null );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.field( "text", entity1.getText() )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void enabled_explicit() {
		SessionFactory sessionFactory = setup( true );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.field( "text", entity1.getText() )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void disabled() {
		SessionFactory sessionFactory = setup( false );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "initialValue" );

			session.persist( entity1 );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {

		static final String NAME = "Indexed";

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String text;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

	}

}
