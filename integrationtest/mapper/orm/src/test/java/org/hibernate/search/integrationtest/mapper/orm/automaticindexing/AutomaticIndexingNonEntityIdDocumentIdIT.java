/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
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
 * Test automatic indexing of an entity type whose document ID is not the entity ID.
 * <p>
 * Note we also test collection events because they fetch the entity ID differently.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutomaticIndexingNonEntityIdDocumentIdIT {

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );
	private SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "indexedField", String.class )
				.field( "indexedElementCollectionField", String.class, b2 -> b2.multiValued( true ) )
		);

		sessionFactory = ormSetupHelper.start().withAnnotatedTypes( IndexedEntity.class ).setup();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3203")
	void directPersistUpdateDelete() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setDocumentId( 42 );
			entity1.setIndexedField( "initialValue" );
			entity1.getIndexedElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "42", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setIndexedField( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "42", b -> b
							.field( "indexedField", entity1.getIndexedField() )
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			session.remove( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.delete( "42" );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3203")
	void directValueUpdate_indexedElementCollectionField() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setDocumentId( 42 );
			entity1.getIndexedElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "42", b -> b
							.field( "indexedField", null )
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
					.addOrUpdate( "42", b -> b
							.field( "indexedField", null )
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
					.addOrUpdate( "42", b -> b
							.field( "indexedField", null )
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
	@TestForIssue(jiraKey = { "HSEARCH-3199", "HSEARCH-3203" })
	void directValueReplace_indexedElementCollectionField() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setDocumentId( 42 );
			entity1.getIndexedElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "42", b -> b
							.field( "indexedField", null )
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
					.addOrUpdate( "42", b -> b
							.field( "indexedField", null )
							.field(
									"indexedElementCollectionField",
									entity1.getIndexedElementCollectionField().get( 0 ),
									entity1.getIndexedElementCollectionField().get( 1 )
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

		@DocumentId
		private Integer documentId;

		@Basic
		@GenericField
		private String indexedField;

		@ElementCollection
		@CollectionTable(name = "indexedColl")
		@GenericField
		private List<String> indexedElementCollectionField = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getDocumentId() {
			return documentId;
		}

		public void setDocumentId(Integer documentId) {
			this.documentId = documentId;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}

		public List<String> getIndexedElementCollectionField() {
			return indexedElementCollectionField;
		}

		public void setIndexedElementCollectionField(List<String> indexedElementCollectionField) {
			this.indexedElementCollectionField = indexedElementCollectionField;
		}
	}

}
