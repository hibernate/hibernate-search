/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.hibernate.search.mapper.pojo.dirtiness.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

/**
 * Test automatic indexing based on Hibernate ORM entity events
 * when a ToOne association is involved.
 * <p>
 * See {@link AbstractOrmAutomaticIndexingAssociationIT} for more details on how this test is designed.
 */
public class OrmAutomaticIndexingSingleAssociationIT extends AbstractOrmAutomaticIndexingAssociationIT<
		OrmAutomaticIndexingSingleAssociationIT.IndexedEntity,
		OrmAutomaticIndexingSingleAssociationIT.ContainingEntity,
		OrmAutomaticIndexingSingleAssociationIT.ContainedEntity
		> {

	public OrmAutomaticIndexingSingleAssociationIT() {
		super( new SingleAssociationModelPrimitives() );
	}

	@Test
	public void directAssociationUpdate_indexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> { } )
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 2 );
			containedEntity.setIndexedField( "initialValue" );

			entity1.setContainedIndexedEmbedded( containedEntity );
			containedEntity.setContainingAsIndexedEmbedded( entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", "initialValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 3 );
			containedEntity.setIndexedField( "updatedValue" );

			entity1.getContainedIndexedEmbedded().setContainingAsIndexedEmbedded( null );
			entity1.setContainedIndexedEmbedded( containedEntity );
			containedEntity.setContainingAsIndexedEmbedded( entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", "updatedValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			entity1.getContainedIndexedEmbedded().setContainingAsIndexedEmbedded( null );
			entity1.setContainedIndexedEmbedded( null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> { } )
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating a non-IndexedEmbedded association in an entity
	 * whose other properties are indexed
	 * does not trigger reindexing of the entity.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void directAssociationUpdate_nonIndexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> { } )
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 2 );
			containedEntity.setNonIndexedField( "initialValue" );

			entity1.setContainedNonIndexedEmbedded( containedEntity );
			containedEntity.setContainingAsNonIndexedEmbedded( entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 3 );
			containedEntity.setNonIndexedField( "updatedValue" );

			entity1.getContainedNonIndexedEmbedded().setContainingAsNonIndexedEmbedded( null );
			entity1.setContainedNonIndexedEmbedded( containedEntity );
			containedEntity.setContainingAsNonIndexedEmbedded( entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			entity1.getContainedNonIndexedEmbedded().setContainingAsNonIndexedEmbedded( null );
			entity1.setContainedNonIndexedEmbedded( null );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating an IndexedEmbedded association in an indexed entity
	 * does not trigger reindexing of the entity
	 * if the association is marked with ReindexOnUpdate = NO.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3206")
	public void directAssociationUpdate_indexedEmbeddedNoReindexOnUpdate() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> { } )
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 2 );
			containedEntity.setIndexedField( "initialValue" );

			entity1.setContainedIndexedEmbeddedNoReindexOnUpdate( containedEntity );
			containedEntity.setContainingAsIndexedEmbeddedNoReindexOnUpdate( entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 3 );
			containedEntity.setIndexedField( "updatedValue" );

			entity1.getContainedIndexedEmbeddedNoReindexOnUpdate().setContainingAsIndexedEmbeddedNoReindexOnUpdate( null );
			entity1.setContainedIndexedEmbeddedNoReindexOnUpdate( containedEntity );
			containedEntity.setContainingAsIndexedEmbeddedNoReindexOnUpdate( entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			entity1.getContainedIndexedEmbeddedNoReindexOnUpdate().setContainingAsIndexedEmbeddedNoReindexOnUpdate( null );
			entity1.setContainedIndexedEmbeddedNoReindexOnUpdate( null );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_indexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setChild( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setParent( containingEntity1 );

			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIndexedField( "initialValue" );

			containingEntity1.setContainedIndexedEmbedded( containedEntity );
			containedEntity.setContainingAsIndexedEmbedded( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "initialValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIndexedField( "updatedValue" );

			containingEntity1.getContainedIndexedEmbedded().setContainingAsIndexedEmbedded( null );
			containingEntity1.setContainedIndexedEmbedded( containedEntity );
			containedEntity.setContainingAsIndexedEmbedded( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "updatedValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity deeplyNestedContainingEntity1 = session.get( ContainingEntity.class, 3 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 6 );
			containedEntity.setIndexedField( "outOfScopeValue" );

			deeplyNestedContainingEntity1.setContainedIndexedEmbedded( containedEntity );
			containedEntity.setContainingAsIndexedEmbedded( deeplyNestedContainingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );

			containingEntity1.getContainedIndexedEmbedded().setContainingAsIndexedEmbedded( null );
			containingEntity1.setContainedIndexedEmbedded( null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "child", b2 -> { } )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating a non-IndexedEmbedded association in an entity
	 * whose properties are otherwise used in an IndexedEmbedded from an indexed entity
	 * does not trigger reindexing of the indexed entity.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void indirectAssociationUpdate_nonIndexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIndexedField( "initialValue" );

			containingEntity1.setContainedNonIndexedEmbedded( containedEntity );
			containedEntity.setContainingAsNonIndexedEmbedded( containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIndexedField( "updatedValue" );

			containingEntity1.getContainedNonIndexedEmbedded().setContainingAsNonIndexedEmbedded( null );
			containingEntity1.setContainedNonIndexedEmbedded( containedEntity );
			containedEntity.setContainingAsNonIndexedEmbedded( containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );

			containingEntity1.getContainedNonIndexedEmbedded().setContainingAsNonIndexedEmbedded( null );
			containingEntity1.setContainedNonIndexedEmbedded( null );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating an IndexedEmbedded association in an entity
	 * that is IndexedEmbedded in an indexed entity
	 * does not trigger reindexing of the indexed entity
	 * if the first association is marked with ReindexOnUpdate = NO.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3206")
	public void indirectAssociationUpdate_indexedEmbeddedNoReindexOnUpdate() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIndexedField( "initialValue" );

			containingEntity1.setContainedIndexedEmbeddedNoReindexOnUpdate( containedEntity );
			containedEntity.setContainingAsIndexedEmbeddedNoReindexOnUpdate( containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIndexedField( "updatedValue" );

			containingEntity1.getContainedIndexedEmbeddedNoReindexOnUpdate().setContainingAsIndexedEmbeddedNoReindexOnUpdate( null );
			containingEntity1.setContainedIndexedEmbeddedNoReindexOnUpdate( containedEntity );
			containedEntity.setContainingAsIndexedEmbeddedNoReindexOnUpdate( containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );

			containingEntity1.getContainedIndexedEmbeddedNoReindexOnUpdate().setContainingAsIndexedEmbeddedNoReindexOnUpdate( null );
			containingEntity1.setContainedIndexedEmbeddedNoReindexOnUpdate( null );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	private static class SingleAssociationModelPrimitives
			implements AssociationModelPrimitives<IndexedEntity, ContainingEntity, ContainedEntity> {

		@Override
		public String getIndexName() {
			return IndexedEntity.INDEX;
		}

		@Override
		public Class<IndexedEntity> getIndexedClass() {
			return IndexedEntity.class;
		}

		@Override
		public Class<ContainingEntity> getContainingClass() {
			return ContainingEntity.class;
		}

		@Override
		public Class<ContainedEntity> getContainedClass() {
			return ContainedEntity.class;
		}

		@Override
		public IndexedEntity newIndexed(int id) {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			return entity;
		}

		@Override
		public ContainingEntity newContaining(int id) {
			ContainingEntity entity = new ContainingEntity();
			entity.setId( id );
			return entity;
		}

		@Override
		public ContainedEntity newContained(int id) {
			ContainedEntity entity = new ContainedEntity();
			entity.setId( id );
			return entity;
		}

		@Override
		public void setChild(ContainingEntity parent, ContainingEntity child) {
			parent.setChild( child );
		}

		@Override
		public void setParent(ContainingEntity child, ContainingEntity parent) {
			child.setParent( parent );
		}

		@Override
		public void setContainedIndexedEmbeddedSingle(ContainingEntity containingEntity, ContainedEntity containedEntity) {
			containingEntity.setContainedIndexedEmbedded( containedEntity );
		}

		@Override
		public void setContainingAsIndexedEmbeddedSingle(ContainedEntity containedEntity, ContainingEntity containingEntity) {
			containedEntity.setContainingAsIndexedEmbedded( containingEntity );
		}

		@Override
		public void setContainedIndexedEmbeddedNoReindexOnUpdateSingle(ContainingEntity containingEntity,
				ContainedEntity containedEntity) {
			containingEntity.setContainedIndexedEmbeddedNoReindexOnUpdate( containedEntity );
		}

		@Override
		public void setContainingAsIndexedEmbeddedNoReindexOnUpdateSingle(ContainedEntity containedEntity,
				ContainingEntity containingEntity) {
			containedEntity.setContainingAsIndexedEmbeddedNoReindexOnUpdate( containingEntity );
		}

		@Override
		public void setIndexedField(ContainedEntity containedEntity, String value) {
			containedEntity.setIndexedField( value );
		}

		@Override
		public void setNonIndexedField(ContainedEntity containedEntity, String value) {
			containedEntity.setNonIndexedField( value );
		}

		@Override
		public List<String> getIndexedElementCollectionField(ContainedEntity containedEntity) {
			return containedEntity.getIndexedElementCollectionField();
		}

		@Override
		public void setIndexedElementCollectionField(ContainedEntity containedEntity, List<String> value) {
			containedEntity.setIndexedElementCollectionField( value );
		}

		@Override
		public List<String> getNonIndexedElementCollectionField(ContainedEntity containedEntity) {
			return containedEntity.getNonIndexedElementCollectionField();
		}

		@Override
		public void setNonIndexedElementCollectionField(ContainedEntity containedEntity, List<String> value) {
			containedEntity.setNonIndexedElementCollectionField( value );
		}

		@Override
		public void setFieldUsedInContainedDerivedField1(ContainedEntity containedEntity, String value) {
			containedEntity.setFieldUsedInContainedDerivedField1( value );
		}

		@Override
		public void setFieldUsedInContainedDerivedField2(ContainedEntity containedEntity, String value) {
			containedEntity.setFieldUsedInContainedDerivedField2( value );
		}

		@Override
		public void setFieldUsedInCrossEntityDerivedField1(ContainedEntity containedEntity, String value) {
			containedEntity.setFieldUsedInCrossEntityDerivedField1( value );
		}

		@Override
		public void setFieldUsedInCrossEntityDerivedField2(ContainedEntity containedEntity, String value) {
			containedEntity.setFieldUsedInCrossEntityDerivedField2( value );
		}
	}

	@Entity(name = "containing")
	public static class ContainingEntity {

		@Id
		private Integer id;

		@OneToOne
		private ContainingEntity parent;

		@OneToOne(mappedBy = "parent")
		@IndexedEmbedded(includePaths = {
				"containedIndexedEmbedded.indexedField",
				"containedIndexedEmbedded.indexedElementCollectionField",
				"containedIndexedEmbedded.containedDerivedField",
				"containedIndexedEmbeddedNoReindexOnUpdate.indexedField",
				"containedIndexedEmbeddedNoReindexOnUpdate.indexedElementCollectionField",
				"containedIndexedEmbeddedNoReindexOnUpdate.containedDerivedField",
				"crossEntityDerivedField"
		})
		private ContainingEntity child;

		@OneToOne
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		private ContainedEntity containedIndexedEmbedded;

		@OneToOne
		private ContainedEntity containedNonIndexedEmbedded;

		@OneToOne
		@IndexedEmbedded(includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" })
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
		private ContainedEntity containedIndexedEmbeddedNoReindexOnUpdate;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainingEntity getParent() {
			return parent;
		}

		public void setParent(ContainingEntity parent) {
			this.parent = parent;
		}

		public ContainingEntity getChild() {
			return child;
		}

		public void setChild(ContainingEntity child) {
			this.child = child;
		}

		public ContainedEntity getContainedIndexedEmbedded() {
			return containedIndexedEmbedded;
		}

		public void setContainedIndexedEmbedded(ContainedEntity containedIndexedEmbedded) {
			this.containedIndexedEmbedded = containedIndexedEmbedded;
		}

		public ContainedEntity getContainedNonIndexedEmbedded() {
			return containedNonIndexedEmbedded;
		}

		public void setContainedNonIndexedEmbedded(ContainedEntity containedNonIndexedEmbedded) {
			this.containedNonIndexedEmbedded = containedNonIndexedEmbedded;
		}

		public ContainedEntity getContainedIndexedEmbeddedNoReindexOnUpdate() {
			return containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedNoReindexOnUpdate(
				ContainedEntity containedIndexedEmbeddedNoReindexOnUpdate) {
			this.containedIndexedEmbeddedNoReindexOnUpdate = containedIndexedEmbeddedNoReindexOnUpdate;
		}

		@Transient
		@Field
		@IndexingDependency(derivedFrom = {
				@ObjectPath({
						@PropertyValue(propertyName = "containedIndexedEmbedded"),
						@PropertyValue(propertyName = "fieldUsedInCrossEntityDerivedField1")
				}),
				@ObjectPath({
						@PropertyValue(propertyName = "containedIndexedEmbedded"),
						@PropertyValue(propertyName = "fieldUsedInCrossEntityDerivedField2")
				})
		})
		public Optional<String> getCrossEntityDerivedField() {
			return containedIndexedEmbedded == null
					? Optional.empty()
					: computeDerived( Stream.of(
							containedIndexedEmbedded.getFieldUsedInCrossEntityDerivedField1(),
							containedIndexedEmbedded.getFieldUsedInCrossEntityDerivedField2()
					) );
		}
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity extends ContainingEntity {

		static final String INDEX = "IndexedEntity";

	}

	@Entity(name = "contained")
	public static class ContainedEntity {
		@Id
		private Integer id;

		@OneToOne(mappedBy = "containedIndexedEmbedded")
		private ContainingEntity containingAsIndexedEmbedded;

		@OneToOne(mappedBy = "containedNonIndexedEmbedded")
		private ContainingEntity containingAsNonIndexedEmbedded;

		@OneToOne(mappedBy = "containedIndexedEmbeddedNoReindexOnUpdate")
		private ContainingEntity containingAsIndexedEmbeddedNoReindexOnUpdate;

		@Basic
		@Field
		private String indexedField;

		@ElementCollection
		@Field
		private List<String> indexedElementCollectionField = new ArrayList<>();

		@Basic
		@Field // Keep this annotation, it should be ignored because the field is not included in the @IndexedEmbedded
		private String nonIndexedField;

		@ElementCollection
		@Field // Keep this annotation, it should be ignored because the field is not included in the @IndexedEmbedded
		private List<String> nonIndexedElementCollectionField = new ArrayList<>();

		@Basic // Do not annotate with @Field, this would make the test pointless
		private String fieldUsedInContainedDerivedField1;

		@Basic // Do not annotate with @Field, this would make the test pointless
		private String fieldUsedInContainedDerivedField2;

		@Basic // Do not annotate with @Field, this would make the test pointless
		private String fieldUsedInCrossEntityDerivedField1;

		@Basic // Do not annotate with @Field, this would make the test pointless
		private String fieldUsedInCrossEntityDerivedField2;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainingEntity getContainingAsIndexedEmbedded() {
			return containingAsIndexedEmbedded;
		}

		public void setContainingAsIndexedEmbedded(ContainingEntity containingAsIndexedEmbedded) {
			this.containingAsIndexedEmbedded = containingAsIndexedEmbedded;
		}

		public ContainingEntity getContainingAsNonIndexedEmbedded() {
			return containingAsNonIndexedEmbedded;
		}

		public void setContainingAsNonIndexedEmbedded(ContainingEntity containingAsNonIndexedEmbedded) {
			this.containingAsNonIndexedEmbedded = containingAsNonIndexedEmbedded;
		}

		public ContainingEntity getContainingAsIndexedEmbeddedNoReindexOnUpdate() {
			return containingAsIndexedEmbeddedNoReindexOnUpdate;
		}

		public void setContainingAsIndexedEmbeddedNoReindexOnUpdate(
				ContainingEntity containingAsIndexedEmbeddedNoReindexOnUpdate) {
			this.containingAsIndexedEmbeddedNoReindexOnUpdate = containingAsIndexedEmbeddedNoReindexOnUpdate;
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

		public String getNonIndexedField() {
			return nonIndexedField;
		}

		public void setNonIndexedField(String nonIndexedField) {
			this.nonIndexedField = nonIndexedField;
		}

		public List<String> getNonIndexedElementCollectionField() {
			return nonIndexedElementCollectionField;
		}

		public void setNonIndexedElementCollectionField(List<String> nonIndexedElementCollectionField) {
			this.nonIndexedElementCollectionField = nonIndexedElementCollectionField;
		}

		public String getFieldUsedInContainedDerivedField1() {
			return fieldUsedInContainedDerivedField1;
		}

		public void setFieldUsedInContainedDerivedField1(String fieldUsedInContainedDerivedField1) {
			this.fieldUsedInContainedDerivedField1 = fieldUsedInContainedDerivedField1;
		}

		public String getFieldUsedInContainedDerivedField2() {
			return fieldUsedInContainedDerivedField2;
		}

		public void setFieldUsedInContainedDerivedField2(String fieldUsedInContainedDerivedField2) {
			this.fieldUsedInContainedDerivedField2 = fieldUsedInContainedDerivedField2;
		}

		public String getFieldUsedInCrossEntityDerivedField1() {
			return fieldUsedInCrossEntityDerivedField1;
		}

		public void setFieldUsedInCrossEntityDerivedField1(String fieldUsedInCrossEntityDerivedField1) {
			this.fieldUsedInCrossEntityDerivedField1 = fieldUsedInCrossEntityDerivedField1;
		}

		public String getFieldUsedInCrossEntityDerivedField2() {
			return fieldUsedInCrossEntityDerivedField2;
		}

		public void setFieldUsedInCrossEntityDerivedField2(String fieldUsedInCrossEntityDerivedField2) {
			this.fieldUsedInCrossEntityDerivedField2 = fieldUsedInCrossEntityDerivedField2;
		}

		@Transient
		@Field
		@IndexingDependency(derivedFrom = {
				@ObjectPath(@PropertyValue(propertyName = "fieldUsedInContainedDerivedField1")),
				@ObjectPath(@PropertyValue(propertyName = "fieldUsedInContainedDerivedField2"))
		})
		public Optional<String> getContainedDerivedField() {
			return computeDerived( Stream.of( fieldUsedInContainedDerivedField1, fieldUsedInContainedDerivedField2 ) );
		}
	}

}
