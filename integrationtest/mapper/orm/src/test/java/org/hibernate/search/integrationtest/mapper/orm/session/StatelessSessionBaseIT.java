/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
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
		defaultBackendMock.expectAnySchema( IndexedEntity.INDEX_NAME );
		defaultBackendMock.expectAnySchema( ContainingEntity.INDEX_NAME );

		sessionFactory = ormSetupHelper.start()
				.withAnnotatedTypes( IndexedEntity.class, NonIndexedEntity.class,
						ContainingEntity.class, ContainedEntity.class )
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
	void insert_notAllowedWithListenerTriggeredIndexing() {
		assertThatThrownBy( () -> {
			sessionFactory.inStatelessTransaction( session -> {
				session.insert( new IndexedEntity( 1, "number1" ) );
			} );
		} ).hasMessageContaining( "Listener-triggered indexing is not supported with a stateless session" );

		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	void update_notAllowedWithListenerTriggeredIndexing() {
		assertThatThrownBy( () -> {
			sessionFactory.inStatelessTransaction( session -> {
				IndexedEntity entity = new IndexedEntity( 1, "initial" );
				session.insert( entity );
			} );
		} ).hasMessageContaining( "Listener-triggered indexing is not supported with a stateless session" );

		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	void delete_notAllowedWithListenerTriggeredIndexing() {
		assertThatThrownBy( () -> {
			sessionFactory.inStatelessTransaction( session -> {
				session.insert( new IndexedEntity( 1, "toDelete" ) );
			} );
		} ).hasMessageContaining( "Listener-triggered indexing is not supported with a stateless session" );

		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	void noListener() {
		listenerEnabled( false );
		sessionFactory.inStatelessTransaction( session -> {
			session.insert( new IndexedEntity( 1, "number1" ) );
			session.insert( new IndexedEntity( 2, "number2" ) );
			session.insert( new IndexedEntity( 3, "number3" ) );
		} );

		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	void nonSearchEntity() {
		sessionFactory.inStatelessTransaction( session -> {
			session.insert( new NonIndexedEntity( 1, "number1" ) );
			session.insert( new NonIndexedEntity( 2, "number2" ) );
			session.insert( new NonIndexedEntity( 3, "number3" ) );
		} );

		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	void searchFromStatelessSession() {
		defaultBackendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.add( "1", b -> b
						.field( "text", "hello" ) );

		sessionFactory.inTransaction( session -> {
			session.persist( new IndexedEntity( 1, "hello" ) );
		} );
		defaultBackendMock.verifyExpectationsMet();

		try ( StatelessSession session = sessionFactory.openStatelessSession() ) {
			SearchSession searchSession = Search.session( session );
			defaultBackendMock.expectCount(
					Arrays.asList( IndexedEntity.INDEX_NAME ), 1L );

			long count = searchSession.search( IndexedEntity.class )
					.where( f -> f.matchAll() )
					.fetchTotalHitCount();

			assertThat( count ).isEqualTo( 1L );
		}

		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	void indexedEmbedded_notAllowedWithListenerTriggeredIndexing() {
		assertThatThrownBy( () -> {
			sessionFactory.inStatelessTransaction( session -> {
				ContainedEntity contained = new ContainedEntity( 10, "embedded" );
				session.insert( contained );

				ContainingEntity containing = new ContainingEntity( 1, "container" );
				containing.setContained( contained );
				session.insert( containing );
			} );
		} ).hasMessageContaining( "Listener-triggered indexing is not supported with a stateless session" );

		defaultBackendMock.verifyExpectationsMet();
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(index = IndexedEntity.INDEX_NAME)
	public static class IndexedEntity {

		static final String NAME = "indexed1";
		static final String INDEX_NAME = "index1Name";

		@Id
		private Integer id;

		@GenericField
		private String text;

		private String nonIndexedText;

		protected IndexedEntity() {
		}

		IndexedEntity(int id, String text) {
			this.id = id;
			this.text = text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public void setNonIndexedText(String nonIndexedText) {
			this.nonIndexedText = nonIndexedText;
		}
	}

	@Entity(name = NonIndexedEntity.NAME)
	public static class NonIndexedEntity {

		static final String NAME = "NonIndexedEntity";

		@Id
		private Integer id;

		private String text;

		protected NonIndexedEntity() {
		}

		NonIndexedEntity(int id, String text) {
			this.id = id;
			this.text = text;
		}
	}

	@Entity(name = ContainingEntity.NAME)
	@Indexed(index = ContainingEntity.INDEX_NAME)
	public static class ContainingEntity {

		static final String NAME = "ContainingEntity";
		static final String INDEX_NAME = "ContainingEntity";

		@Id
		private Integer id;

		@GenericField
		private String name;

		@ManyToOne(fetch = FetchType.EAGER)
		@IndexedEmbedded
		private ContainedEntity contained;

		protected ContainingEntity() {
		}

		ContainingEntity(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setContained(ContainedEntity contained) {
			this.contained = contained;
		}
	}

	@Entity(name = ContainedEntity.NAME)
	public static class ContainedEntity {

		static final String NAME = "ContainedEntity";

		@Id
		private Integer id;

		@GenericField
		private String detail;

		@OneToMany(mappedBy = "contained")
		private List<ContainingEntity> containingEntities = new ArrayList<>();

		protected ContainedEntity() {
		}

		ContainedEntity(int id, String detail) {
			this.id = id;
			this.detail = detail;
		}

		public void setDetail(String detail) {
			this.detail = detail;
		}

		public List<ContainingEntity> getContainingEntities() {
			return containingEntities;
		}
	}
}
