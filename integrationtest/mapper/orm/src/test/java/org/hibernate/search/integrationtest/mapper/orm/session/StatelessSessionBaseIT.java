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
import org.hibernate.Transaction;
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
	void insert() {
		defaultBackendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.add( "1", b -> b
						.field( "text", "number1" ) )
				.add( "2", b -> b
						.field( "text", "number2" ) )
				.add( "3", b -> b
						.field( "text", "number3" ) );

		sessionFactory.inStatelessTransaction( session -> {
			session.insert( new IndexedEntity( 1, "number1" ) );
			session.insert( new IndexedEntity( 2, "number2" ) );
			session.insert( new IndexedEntity( 3, "number3" ) );
		} );

		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	void update() {
		defaultBackendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.add( "1", b -> b
						.field( "text", "initial" ) );

		sessionFactory.inStatelessTransaction( session -> {
			session.insert( new IndexedEntity( 1, "initial" ) );
		} );
		defaultBackendMock.verifyExpectationsMet();

		defaultBackendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.addOrUpdate( "1", b -> b
						.field( "text", "updated" ) );

		sessionFactory.inStatelessTransaction( session -> {
			IndexedEntity entity = session.get( IndexedEntity.class, 1 );
			entity.setText( "updated" );
			session.update( entity );
		} );

		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	void delete() {
		defaultBackendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.add( "1", b -> b
						.field( "text", "toDelete" ) );

		sessionFactory.inStatelessTransaction( session -> {
			session.insert( new IndexedEntity( 1, "toDelete" ) );
		} );
		defaultBackendMock.verifyExpectationsMet();

		defaultBackendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.delete( "1" );

		sessionFactory.inStatelessTransaction( session -> {
			IndexedEntity entity = session.get( IndexedEntity.class, 1 );
			session.delete( entity );
		} );

		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	void mutationAfterInsert() {
		defaultBackendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.add( "1", b -> b
						.field( "text", "originalValue" ) );

		sessionFactory.inStatelessTransaction( session -> {
			IndexedEntity entity = new IndexedEntity( 1, "originalValue" );
			session.insert( entity );
			// nothing will pick up this change and we will only sync the state that the insert had:
			entity.setText( "mutatedValue" );
		} );

		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	void insertAndUpdateInSameTransaction() {
		defaultBackendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.add( "1", b -> b
						.field( "text", "initial" ) )
				.addOrUpdate( "1", b -> b
						.field( "text", "updated" ) );

		sessionFactory.inStatelessTransaction( session -> {
			IndexedEntity entity = new IndexedEntity( 1, "initial" );
			session.insert( entity );

			entity.setText( "updated" );
			session.update( entity );
		} );

		defaultBackendMock.verifyExpectationsMet();
	}

	/**
	 * With StatelessSession, dirty checking information is not available.
	 * As a result, even if only a non-indexed field is changed,
	 * the entity will still be reindexed.
	 */
	@Test
	void noDirtyChecking_nonIndexedFieldChange_stillReindexes() {
		defaultBackendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.add( "1", b -> b
						.field( "text", "value" ) );

		sessionFactory.inStatelessTransaction( session -> {
			session.insert( new IndexedEntity( 1, "value" ) );
		} );
		defaultBackendMock.verifyExpectationsMet();

		// we are going to change a non-indexed field, but since we don't do
		// dirty checking -- everything will be considered dirty, so an update should happen:
		defaultBackendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.addOrUpdate( "1", b -> b
						.field( "text", "value" ) );

		sessionFactory.inStatelessTransaction( session -> {
			IndexedEntity entity = session.get( IndexedEntity.class, 1 );
			entity.setNonIndexedText( "changed" );
			session.update( entity );
		} );

		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	void rollback() {
		defaultBackendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.createFollowingWorks()
				.add( "1", b -> b
						.field( "text", "shouldBeDiscarded" ) );

		defaultBackendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.discardFollowingWorks()
				.add( "1", b -> b
						.field( "text", "shouldBeDiscarded" ) );

		try ( StatelessSession session = sessionFactory.openStatelessSession() ) {
			Transaction trx = session.beginTransaction();
			session.insert( new IndexedEntity( 1, "shouldBeDiscarded" ) );
			trx.rollback();
		}

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

		sessionFactory.inStatelessTransaction( session -> {
			session.insert( new IndexedEntity( 1, "hello" ) );
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
	void indexedEmbedded_insertContainingWithContained() {
		defaultBackendMock.expectWorks( ContainingEntity.INDEX_NAME )
				.add( "1", b -> b
						.field( "name", "container" )
						.objectField( "contained", b2 -> b2
								.field( "detail", "embedded" ) ) );

		sessionFactory.inStatelessTransaction( session -> {
			ContainedEntity contained = new ContainedEntity( 10, "embedded" );
			session.insert( contained );

			ContainingEntity containing = new ContainingEntity( 1, "container" );
			containing.setContained( contained );
			session.insert( containing );
		} );

		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	void indexedEmbedded_updateContainingDirectly() {
		defaultBackendMock.expectWorks( ContainingEntity.INDEX_NAME )
				.add( "1", b -> b
						.field( "name", "initial" )
						.objectField( "contained", b2 -> b2
								.field( "detail", "embedded" ) ) );

		sessionFactory.inStatelessTransaction( session -> {
			ContainedEntity contained = new ContainedEntity( 10, "embedded" );
			session.insert( contained );

			ContainingEntity containing = new ContainingEntity( 1, "initial" );
			containing.setContained( contained );
			session.insert( containing );
		} );
		defaultBackendMock.verifyExpectationsMet();

		defaultBackendMock.expectWorks( ContainingEntity.INDEX_NAME )
				.addOrUpdate( "1", b -> b
						.field( "name", "updated" )
						.objectField( "contained", b2 -> b2
								.field( "detail", "embedded" ) ) );

		sessionFactory.inStatelessTransaction( session -> {
			ContainingEntity containing = session.get( ContainingEntity.class, 1 );
			containing.setName( "updated" );
			session.update( containing );
		} );

		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	void indexedEmbedded_updateContainedEntity_throws() {
		defaultBackendMock.expectWorks( ContainingEntity.INDEX_NAME )
				.add( "1", b -> b
						.field( "name", "container" )
						.objectField( "contained", b2 -> b2
								.field( "detail", "original" ) ) );

		sessionFactory.inStatelessTransaction( session -> {
			ContainedEntity contained = new ContainedEntity( 10, "original" );
			session.insert( contained );

			ContainingEntity containing = new ContainingEntity( 1, "container" );
			containing.setContained( contained );
			session.insert( containing );
		} );
		defaultBackendMock.verifyExpectationsMet();

		assertThatThrownBy( () -> {
			sessionFactory.inStatelessTransaction( session -> {
				ContainedEntity contained = session.get( ContainedEntity.class, 10 );
				contained.setDetail( "updated" );
				session.update( contained );
			} );
		} ).hasMessageContaining( "ContainedEntity" );

		assertThatThrownBy( () -> {
			sessionFactory.inStatelessTransaction( session -> {
				ContainedEntity contained = session.get( ContainedEntity.class, 10 );
				session.fetch( contained.getContainingEntities() );
				contained.setDetail( "updated" );
				session.update( contained );
			} );
		} ).hasMessageContaining( "ContainedEntity" );

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
