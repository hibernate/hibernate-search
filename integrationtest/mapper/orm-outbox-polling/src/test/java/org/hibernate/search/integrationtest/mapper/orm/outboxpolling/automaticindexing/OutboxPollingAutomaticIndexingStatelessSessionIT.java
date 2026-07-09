/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.OutboxEventFilter;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.TestingOutboxPollingInternalConfigurer;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.impl.HibernateOrmMapperOutboxPollingImplSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OutboxPollingAutomaticIndexingStatelessSessionIT {

	private static final OutboxEventFilter eventFilter = new OutboxEventFilter();

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper =
			OrmSetupHelper.withCoordinationStrategy( CoordinationStrategyExpectations.outboxPolling() )
					.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		backendMock.expectAnySchema( IndexedEntity.INDEX_NAME );
		backendMock.expectAnySchema( ContainingEntity.INDEX_NAME );

		sessionFactory = ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperOutboxPollingImplSettings.COORDINATION_INTERNAL_CONFIGURER,
						new TestingOutboxPollingInternalConfigurer().outboxEventFilter( eventFilter )
				)
				.withAnnotatedTypes( IndexedEntity.class, NonIndexedEntity.class,
						ContainingEntity.class, ContainedEntity.class )
				.setup();
	}

	@BeforeEach
	void resetFilter() {
		eventFilter.reset();
		eventFilter.showAllEvents();
	}

	@Test
	void insert() {
		backendMock.expectWorks( IndexedEntity.INDEX_NAME )
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

		backendMock.verifyExpectationsMet();
	}

	@Test
	void update() {
		backendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.add( "1", b -> b
						.field( "text", "initial" ) );

		sessionFactory.inStatelessTransaction( session -> {
			session.insert( new IndexedEntity( 1, "initial" ) );
		} );
		backendMock.verifyExpectationsMet();

		backendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.addOrUpdate( "1", b -> b
						.field( "text", "updated" ) );

		sessionFactory.inStatelessTransaction( session -> {
			IndexedEntity entity = session.get( IndexedEntity.class, 1 );
			entity.setText( "updated" );
			session.update( entity );
		} );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void delete() {
		backendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.add( "1", b -> b
						.field( "text", "toDelete" ) );

		sessionFactory.inStatelessTransaction( session -> {
			session.insert( new IndexedEntity( 1, "toDelete" ) );
		} );
		backendMock.verifyExpectationsMet();

		backendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.delete( "1" );

		sessionFactory.inStatelessTransaction( session -> {
			IndexedEntity entity = session.get( IndexedEntity.class, 1 );
			session.delete( entity );
		} );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void mutationAfterInsert() {
		backendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.add( "1", b -> b
						.field( "text", "originalValue" ) );

		sessionFactory.inStatelessTransaction( session -> {
			IndexedEntity entity = new IndexedEntity( 1, "originalValue" );
			session.insert( entity );
			entity.setText( "mutatedValue" );
		} );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void insertAndUpdateInSameTransaction() {
		backendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.addOrUpdate( "1", b -> b
						.field( "text", "updated" ) );

		sessionFactory.inStatelessTransaction( session -> {
			IndexedEntity entity = new IndexedEntity( 1, "initial" );
			session.insert( entity );

			entity.setText( "updated" );
			session.update( entity );
		} );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void noDirtyChecking_nonIndexedFieldChange_stillReindexes() {
		backendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.add( "1", b -> b
						.field( "text", "value" ) );

		sessionFactory.inStatelessTransaction( session -> {
			session.insert( new IndexedEntity( 1, "value" ) );
		} );
		backendMock.verifyExpectationsMet();

		backendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.addOrUpdate( "1", b -> b
						.field( "text", "value" ) );

		sessionFactory.inStatelessTransaction( session -> {
			IndexedEntity entity = session.get( IndexedEntity.class, 1 );
			entity.setNonIndexedText( "changed" );
			session.update( entity );
		} );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void nonSearchEntity() {
		sessionFactory.inStatelessTransaction( session -> {
			session.insert( new NonIndexedEntity( 1, "number1" ) );
			session.insert( new NonIndexedEntity( 2, "number2" ) );
			session.insert( new NonIndexedEntity( 3, "number3" ) );
		} );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void searchFromStatelessSession() {
		backendMock.expectWorks( IndexedEntity.INDEX_NAME )
				.add( "1", b -> b
						.field( "text", "hello" ) );

		sessionFactory.inStatelessTransaction( session -> {
			session.insert( new IndexedEntity( 1, "hello" ) );
		} );
		backendMock.verifyExpectationsMet();

		try ( StatelessSession session = sessionFactory.openStatelessSession() ) {
			SearchSession searchSession = Search.session( session );
			backendMock.expectCount(
					Arrays.asList( IndexedEntity.INDEX_NAME ), 1L );

			long count = searchSession.search( IndexedEntity.class )
					.where( f -> f.matchAll() )
					.fetchTotalHitCount();

			assertThat( count ).isEqualTo( 1L );
		}

		backendMock.verifyExpectationsMet();
	}

	@Test
	void indexedEmbedded_insertContainingWithContained() {
		backendMock.expectWorks( ContainingEntity.INDEX_NAME )
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

		backendMock.verifyExpectationsMet();
	}

	@Test
	void indexedEmbedded_updateContainingDirectly() {
		backendMock.expectWorks( ContainingEntity.INDEX_NAME )
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
		backendMock.verifyExpectationsMet();

		backendMock.expectWorks( ContainingEntity.INDEX_NAME )
				.addOrUpdate( "1", b -> b
						.field( "name", "updated" )
						.objectField( "contained", b2 -> b2
								.field( "detail", "embedded" ) ) );

		sessionFactory.inStatelessTransaction( session -> {
			ContainingEntity containing = session.get( ContainingEntity.class, 1 );
			containing.setName( "updated" );
			session.update( containing );
		} );

		backendMock.verifyExpectationsMet();
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
