/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Simple test to check that automatic indexing works correctly when multi-tenancy is enabled.
 * <p>
 * This is especially relevant with coordination strategies that involve performing indexing
 * from a background thread, since they will have to remember the tenant ID somehow.
 */
@TestForIssue(jiraKey = "HSEARCH-4316")
class AutomaticIndexingMultiTenancyIT {

	private static final String TENANT_1_ID = "tenant1";
	private static final String TENANT_2_ID = "tenant2";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper setupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	void test() throws InterruptedException {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) ) );

		SessionFactory sessionFactory = setupHelper.start()
				.tenantsWithHelperEnabled( TENANT_1_ID, TENANT_2_ID )
				.setup( IndexedEntity.class );

		with( sessionFactory, TENANT_1_ID ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity( 1, "value for tenant 1" );
			session.persist( entity );

			backendMock.expectWorks( IndexedEntity.NAME, TENANT_1_ID )
					.add( String.valueOf( 1 ), b -> b.field( "text", "value for tenant 1" ) );
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory, TENANT_2_ID ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity( 1, "value for tenant 2" );
			session.persist( entity );

			backendMock.expectWorks( IndexedEntity.NAME, TENANT_2_ID )
					.add( String.valueOf( 1 ), b -> b.field( "text", "value for tenant 2" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {

		static final String NAME = "IndexedEntity";

		static volatile AtomicReference<Runnable> getTextConcurrentOperation = new AtomicReference<>( () -> {} );

		private Integer id;
		private String text;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@FullTextField
		public String getText() {
			getTextConcurrentOperation.get().run();
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

}
