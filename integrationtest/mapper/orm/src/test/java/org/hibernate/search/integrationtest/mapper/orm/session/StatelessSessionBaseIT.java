/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.session;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatelessSessionBaseIT {

	@RegisterExtension
	public static BackendMock defaultBackendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper =
			OrmSetupHelper.withBackendMock( defaultBackendMock );
	private SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		defaultBackendMock.expectAnySchema( IndexedEntity1.INDEX_NAME );

		sessionFactory = ormSetupHelper.start()
				.withAnnotatedTypes( IndexedEntity1.class, RandomEntity.class )
				.setup();
	}

	private void listenerEnabled(boolean enabled) {
		HibernateOrmMapping mapping = ( (HibernateOrmMapping) Search.mapping( sessionFactory ) );
		mapping.listenerEnabled( enabled );
	}

	@BeforeEach
	void setUp() {
		listenerEnabled( true );
	}

	@Test
	void searchEntity() {
		assertThatThrownBy( () -> {
			sessionFactory.inStatelessTransaction( session -> {
				IndexedEntity1 entity1 = new IndexedEntity1( 1, "number1" );
				IndexedEntity1 entity2 = new IndexedEntity1( 2, "number2" );
				IndexedEntity1 entity3 = new IndexedEntity1( 3, "number3" );

				session.insert( entity1 );
				session.insert( entity2 );
				session.insert( entity3 );

				// should've been:
				//			defaultBackendMock.expectWorks( IndexedEntity1.INDEX_NAME )
				//					.addOrUpdate( "1", b -> b.field( "text", "number1" ) )
				//					.addOrUpdate( "2", b -> b.field( "text", "number2" ) )
				//					.addOrUpdate( "3", b -> b.field( "text", "number3" ) );
			} );
		} ).hasMessageContaining(
				"Hibernate Search does not support working with org.hibernate.internal.StatelessSessionImpl session type" );

		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	void searchEntityNoListener() {
		listenerEnabled( false );
		sessionFactory.inStatelessTransaction( session -> {
			IndexedEntity1 entity1 = new IndexedEntity1( 1, "number1" );
			IndexedEntity1 entity2 = new IndexedEntity1( 2, "number2" );
			IndexedEntity1 entity3 = new IndexedEntity1( 3, "number3" );

			session.insert( entity1 );
			session.insert( entity2 );
			session.insert( entity3 );
		} );

		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	void nonSearchEntity() {
		sessionFactory.inStatelessTransaction( session -> {
			RandomEntity entity1 = new RandomEntity( 1, "number1" );
			RandomEntity entity2 = new RandomEntity( 2, "number2" );
			RandomEntity entity3 = new RandomEntity( 3, "number3" );

			session.insert( entity1 );
			session.insert( entity2 );
			session.insert( entity3 );
		} );

		defaultBackendMock.verifyExpectationsMet();
	}

	@Entity(name = IndexedEntity1.NAME)
	@Indexed(index = IndexedEntity1.INDEX_NAME)
	public static class IndexedEntity1 {

		static final String NAME = "indexed1";

		static final String INDEX_NAME = "index1Name";

		@Id
		private Integer id;

		@GenericField
		private String text;

		protected IndexedEntity1() {
			// For ORM
		}

		IndexedEntity1(int id, String text) {
			this.id = id;
			this.text = text;
		}
	}

	@Entity(name = RandomEntity.NAME)
	public static class RandomEntity {

		static final String NAME = "RandomEntity";

		@Id
		private Integer id;

		private String text;

		protected RandomEntity() {
			// For ORM
		}

		RandomEntity(int id, String text) {
			this.id = id;
			this.text = text;
		}
	}
}
