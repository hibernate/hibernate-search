/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test automatic indexing based on Hibernate ORM entity events for element collections.
 *
 * This test only checks basic, direct updates to the entity state.
 * Other tests in the same package check more complex updates involving associations.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutomaticIndexingElementCollectionIT {

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );
	private SessionFactory sessionFactory;

	@BeforeAll
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "indexedElementCollectionField", String.class, b2 -> b2.multiValued( true ) )
				.field( "shallowReindexOnUpdateElementCollectionField", String.class, b2 -> b2.multiValued( true ) )
				.field( "noReindexOnUpdateElementCollectionField", String.class, b2 -> b2.multiValued( true ) )
		);

		sessionFactory = ormSetupHelper.start().withAnnotatedTypes( IndexedEntity.class ).setup();
	}

	@Test
	void directValueUpdate_indexedElementCollectionField() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.getIndexedElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getIndexedElementCollectionField().add( "secondValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 ),
									entity1.getIndexedElementCollectionField().get( 1 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getIndexedElementCollectionField().remove( 1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing an indexed element collection
	 * does trigger reindexing of the indexed entity owning the collection.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	void directValueReplace_indexedElementCollectionField() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.getIndexedElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setIndexedElementCollectionField( new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 ),
									entity1.getIndexedElementCollectionField().get( 1 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating a non-indexed element collection
	 * does not trigger reindexing of the indexed entity owning the collection.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	void directValueUpdate_nonIndexedElementCollectionField() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.getNonIndexedElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> {} );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getNonIndexedElementCollectionField().add( "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getNonIndexedElementCollectionField().remove( 1 );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing a non-indexed element collection
	 * does not trigger reindexing of the indexed entity owning the collection.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3204")
	void directValueReplace_nonIndexedElementCollectionField() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.getNonIndexedElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> {} );
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setNonIndexedElementCollectionField( new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> {} );
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating an indexed element collection configured with reindexOnUpdate = SHALLOW
	 * does trigger reindexing of the indexed entity owning the collection.
	 * <p>
	 * SHALLOW isn't really useful in this case, since there's no "depth" to speak of,
	 * but we're testing this anyway, for the sake of completeness.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4001")
	void directValueUpdate_shallowReindexOnUpdateElementCollectionField() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.getShallowReindexOnUpdateElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "shallowReindexOnUpdateElementCollectionField", "firstValue" )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getShallowReindexOnUpdateElementCollectionField().add( "secondValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.field( "shallowReindexOnUpdateElementCollectionField",
									"firstValue", "secondValue" )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getShallowReindexOnUpdateElementCollectionField().remove( 1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.field( "shallowReindexOnUpdateElementCollectionField", "firstValue" )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing an indexed element collection configured with reindexOnUpdate = SHALLOW
	 * does trigger reindexing of the indexed entity owning the collection.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 * <p>
	 * SHALLOW isn't really useful in this case, since there's no "depth" to speak of,
	 * but we're testing this anyway, for the sake of completeness.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4001")
	void directValueReplace_shallowReindexOnUpdateElementCollectionField() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.getShallowReindexOnUpdateElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "shallowReindexOnUpdateElementCollectionField", "firstValue" )
					);
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setShallowReindexOnUpdateElementCollectionField( new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.field( "shallowReindexOnUpdateElementCollectionField",
									"newFirstValue", "newSecondValue" )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating an indexed element collection configured with reindexOnUpdate = NO
	 * does not trigger reindexing of the indexed entity owning the collection.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3206")
	void directValueUpdate_noReindexOnUpdateElementCollectionField() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.getNoReindexOnUpdateElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "noReindexOnUpdateElementCollectionField", "firstValue" )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getNoReindexOnUpdateElementCollectionField().add( "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getNoReindexOnUpdateElementCollectionField().remove( 1 );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing an indexed element collection configured with reindexOnUpdate = NO
	 * does not trigger reindexing of the indexed entity owning the collection.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3204")
	void directValueReplace_noReindexOnUpdateElementCollectionField() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.getNoReindexOnUpdateElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "noReindexOnUpdateElementCollectionField", "firstValue" )
					);
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setNoReindexOnUpdateElementCollectionField( new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.field(
									"noReindexOnUpdateElementCollectionField",
									"newFirstValue", "newSecondValue"
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@ElementCollection
		@CollectionTable(name = "indexedColl")
		@Column(name = "indexed")
		@OrderColumn(name = "idx")
		@GenericField
		private List<String> indexedElementCollectionField = new ArrayList<>();

		@ElementCollection
		@CollectionTable(name = "nonIndexedColl")
		@Column(name = "nonIndexed")
		@OrderColumn(name = "idx")
		private List<String> nonIndexedElementCollectionField = new ArrayList<>();

		@ElementCollection
		@CollectionTable(name = "shallowReindexOnUpdateColl")
		@Column(name = "shallowReindexOnUpdate")
		@OrderColumn(name = "idx")
		@GenericField
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
		private List<String> shallowReindexOnUpdateElementCollectionField = new ArrayList<>();

		@ElementCollection
		@CollectionTable(name = "noReindexOnUpdateColl")
		@Column(name = "noReindexOnUpdate")
		@OrderColumn(name = "idx")
		@GenericField
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
		private List<String> noReindexOnUpdateElementCollectionField = new ArrayList<>();

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<String> getIndexedElementCollectionField() {
			return indexedElementCollectionField;
		}

		public void setIndexedElementCollectionField(List<String> indexedElementCollectionField) {
			this.indexedElementCollectionField = indexedElementCollectionField;
		}

		public List<String> getNonIndexedElementCollectionField() {
			return nonIndexedElementCollectionField;
		}

		public void setNonIndexedElementCollectionField(List<String> nonIndexedElementCollectionField) {
			this.nonIndexedElementCollectionField = nonIndexedElementCollectionField;
		}

		public List<String> getShallowReindexOnUpdateElementCollectionField() {
			return shallowReindexOnUpdateElementCollectionField;
		}

		public void setShallowReindexOnUpdateElementCollectionField(List<String> shallowReindexOnUpdateElementCollectionField) {
			this.shallowReindexOnUpdateElementCollectionField = shallowReindexOnUpdateElementCollectionField;
		}

		public List<String> getNoReindexOnUpdateElementCollectionField() {
			return noReindexOnUpdateElementCollectionField;
		}

		public void setNoReindexOnUpdateElementCollectionField(List<String> noReindexOnUpdateElementCollectionField) {
			this.noReindexOnUpdateElementCollectionField = noReindexOnUpdateElementCollectionField;
		}
	}

}
