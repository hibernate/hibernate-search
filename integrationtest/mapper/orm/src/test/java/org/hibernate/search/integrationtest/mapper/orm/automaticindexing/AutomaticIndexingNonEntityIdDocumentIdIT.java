/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * Test automatic indexing of an entity type whose document ID is not the entity ID.
 * <p>
 * Note we also test collection events because they fetch the entity ID differently.
 */
public class AutomaticIndexingNonEntityIdDocumentIdIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "indexedField", String.class )
				.field( "indexedElementCollectionField", String.class, b2 -> b2.multiValued( true ) )
		);

		setupContext.withAnnotatedTypes( IndexedEntity.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3203")
	public void directPersistUpdateDelete() {
		setupHolder.runInTransaction( session -> {
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

		setupHolder.runInTransaction( session -> {
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

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			session.remove( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.delete( "42" );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3203")
	public void directValueUpdate_indexedElementCollectionField() {
		setupHolder.runInTransaction( session -> {
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
		setupHolder.runInTransaction( session -> {
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
		setupHolder.runInTransaction( session -> {
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
	public void directValueReplace_indexedElementCollectionField() {
		setupHolder.runInTransaction( session -> {
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

		setupHolder.runInTransaction( session -> {
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
