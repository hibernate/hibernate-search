/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;

import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

/**
 * Abstract base for tests of automatic indexing caused by association updates
 * or by updates of associated (contained) entities,
 * with a single-valued association.
 */
public abstract class AbstractAutomaticIndexingSingleValuedAssociationBaseIT<
				TIndexed extends TContaining, TContaining, TContained
		>
		extends AbstractAutomaticIndexingAssociationBaseIT<TIndexed, TContaining, TContained> {

	private final SingleValuedAssociationModelPrimitives<TIndexed, TContaining, TContained> primitives;

	public AbstractAutomaticIndexingSingleValuedAssociationBaseIT(
			SingleValuedAssociationModelPrimitives<TIndexed, TContaining, TContained> primitives) {
		super( primitives );
		this.primitives = primitives;
	}

	@Test
	public void directAssociationUpdate_indexedEmbedded() {
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained containedEntity = primitives.newContained( 2 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			primitives.containedIndexedEmbedded().set( entity1, containedEntity );
			primitives.containingAsIndexedEmbedded().set( containedEntity, entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", "initialValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained containedEntity = primitives.newContained( 3 );
			primitives.indexedField().set( containedEntity, "updatedValue" );

			primitives.containingAsIndexedEmbedded().clear( primitives.containedIndexedEmbedded().get( entity1 ) );
			primitives.containedIndexedEmbedded().set( entity1, containedEntity );
			primitives.containingAsIndexedEmbedded().set( containedEntity, entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", "updatedValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			primitives.containingAsIndexedEmbedded().clear( primitives.containedIndexedEmbedded().get( entity1 ) );
			primitives.containedIndexedEmbedded().clear( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> { } );
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
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained containedEntity = primitives.newContained( 2 );
			primitives.nonIndexedField().set( containedEntity, "initialValue" );

			primitives.containedNonIndexedEmbedded().set( entity1, containedEntity );
			primitives.containingAsNonIndexedEmbedded().set( containedEntity, entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained containedEntity = primitives.newContained( 3 );
			primitives.nonIndexedField().set( containedEntity, "updatedValue" );

			primitives.containingAsNonIndexedEmbedded().clear( primitives.containedNonIndexedEmbedded().get( entity1 ) );
			primitives.containedNonIndexedEmbedded().set( entity1, containedEntity );
			primitives.containingAsNonIndexedEmbedded().set( containedEntity, entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			primitives.containingAsNonIndexedEmbedded().clear( primitives.containedNonIndexedEmbedded().get( entity1 ) );
			primitives.containedNonIndexedEmbedded().clear( entity1 );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating an IndexedEmbedded association in an indexed entity
	 * does trigger reindexing of the entity
	 * if the association is marked with ReindexOnUpdate = SHALLOW.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4001")
	public void directAssociationUpdate_indexedEmbeddedShallowReindexOnUpdate() {
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained containedEntity = primitives.newContained( 2 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().set( entity1, containedEntity );

			session.persist( containedEntity );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b2 -> b2
									.field( "indexedField", "initialValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained containedEntity = primitives.newContained( 3 );
			primitives.indexedField().set( containedEntity, "updatedValue" );

			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().set( entity1, containedEntity );

			session.persist( containedEntity );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b2 -> b2
									.field( "indexedField", "updatedValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().clear( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> { } );
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
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained containedEntity = primitives.newContained( 2 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			primitives.containedIndexedEmbeddedNoReindexOnUpdate().set( entity1, containedEntity );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained containedEntity = primitives.newContained( 3 );
			primitives.indexedField().set( containedEntity, "updatedValue" );

			primitives.containedIndexedEmbeddedNoReindexOnUpdate().set( entity1, containedEntity );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			primitives.containedIndexedEmbeddedNoReindexOnUpdate().clear( entity1 );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_indexedEmbedded() {
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = primitives.newContaining( 3 );
			primitives.child().set( containingEntity1, deeplyNestedContainingEntity );
			primitives.parent().set( deeplyNestedContainingEntity, containingEntity1 );

			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 4 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			primitives.containedIndexedEmbedded().set( containingEntity1, containedEntity );
			primitives.containingAsIndexedEmbedded().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "initialValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 5 );
			primitives.indexedField().set( containedEntity, "updatedValue" );

			primitives.containingAsIndexedEmbedded().clear( primitives.containedIndexedEmbedded().get( containingEntity1 ) );
			primitives.containedIndexedEmbedded().set( containingEntity1, containedEntity );
			primitives.containingAsIndexedEmbedded().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "updatedValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		withinTransaction( sessionFactory, session -> {
			TContaining deeplyNestedContainingEntity1 = session.get( primitives.getContainingClass(), 3 );

			TContained containedEntity = primitives.newContained( 6 );
			primitives.indexedField().set( containedEntity, "outOfScopeValue" );

			primitives.containedIndexedEmbedded().set( deeplyNestedContainingEntity1, containedEntity );
			primitives.containingAsIndexedEmbedded().set( containedEntity, deeplyNestedContainingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			primitives.containingAsIndexedEmbedded().clear( primitives.containedIndexedEmbedded().get( containingEntity1 ) );
			primitives.containedIndexedEmbedded().clear( containingEntity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
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
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 4 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			primitives.containedNonIndexedEmbedded().set( containingEntity1, containedEntity );
			primitives.containingAsNonIndexedEmbedded().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 5 );
			primitives.indexedField().set( containedEntity, "updatedValue" );

			primitives.containingAsNonIndexedEmbedded().clear( primitives.containedNonIndexedEmbedded().get( containingEntity1 ) );
			primitives.containedNonIndexedEmbedded().set( containingEntity1, containedEntity );
			primitives.containingAsNonIndexedEmbedded().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			primitives.containingAsNonIndexedEmbedded().clear( primitives.containedNonIndexedEmbedded().get( containingEntity1 ) );
			primitives.containedNonIndexedEmbedded().clear( containingEntity1 );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating an IndexedEmbedded association in an entity
	 * that is IndexedEmbedded in an indexed entity
	 * does trigger reindexing of the indexed entity
	 * if the first association is marked with ReindexOnUpdate = SHALLOW.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4001")
	public void indirectAssociationUpdate_indexedEmbeddedShallowReindexOnUpdate() {
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 4 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().set( containingEntity1, containedEntity );

			session.persist( containedEntity );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b3 -> b3
											.field( "indexedField", "initialValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 5 );
			primitives.indexedField().set( containedEntity, "updatedValue" );

			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().set( containingEntity1, containedEntity );

			session.persist( containedEntity );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b3 -> b3
											.field( "indexedField", "updatedValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().clear( containingEntity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
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
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 4 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			primitives.containedIndexedEmbeddedNoReindexOnUpdate().set( containingEntity1, containedEntity );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 5 );
			primitives.indexedField().set( containedEntity, "updatedValue" );

			primitives.containedIndexedEmbeddedNoReindexOnUpdate().set( containingEntity1, containedEntity );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			primitives.containedIndexedEmbeddedNoReindexOnUpdate().clear( containingEntity1 );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating an association in an entity
	 * when this association is a component of a path in a @IndexingDependency.derivedFrom attribute
	 * does trigger reindexing of the indexed entity.
	 */
	@Test
	public void indirectAssociationUpdate_usedInCrossEntityDerivedProperty() {
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = primitives.newContaining( 3 );
			primitives.child().set( containingEntity1, deeplyNestedContainingEntity );
			primitives.parent().set( deeplyNestedContainingEntity, containingEntity1 );

			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 4 );
			primitives.fieldUsedInCrossEntityDerivedField1().set( containedEntity, "field1_initialValue" );
			primitives.fieldUsedInCrossEntityDerivedField2().set( containedEntity, "field2_initialValue" );

			primitives.containedUsedInCrossEntityDerivedProperty().set( containingEntity1, containedEntity );
			primitives.containingAsUsedInCrossEntityDerivedProperty().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.field(
											"crossEntityDerivedField",
											"field1_initialValue field2_initialValue"
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 5 );
			primitives.fieldUsedInCrossEntityDerivedField1().set( containedEntity, "field1_updatedValue" );
			primitives.fieldUsedInCrossEntityDerivedField2().set( containedEntity, "field2_updatedValue" );

			primitives.containingAsUsedInCrossEntityDerivedProperty().clear( primitives.containedUsedInCrossEntityDerivedProperty().get( containingEntity1 ) );
			primitives.containedUsedInCrossEntityDerivedProperty().set( containingEntity1, containedEntity );
			primitives.containingAsUsedInCrossEntityDerivedProperty().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.field(
											"crossEntityDerivedField",
											"field1_updatedValue field2_updatedValue"
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value that is too deeply nested to matter (it's out of the path)
		withinTransaction( sessionFactory, session -> {
			TContaining deeplyNestedContainingEntity1 = session.get( primitives.getContainingClass(), 3 );

			TContained containedEntity = primitives.newContained( 6 );
			primitives.fieldUsedInCrossEntityDerivedField1().set( containedEntity, "field1_outOfScopeValue" );
			primitives.fieldUsedInCrossEntityDerivedField2().set( containedEntity, "field2_outOfScopeValue" );

			primitives.containedUsedInCrossEntityDerivedProperty().set( deeplyNestedContainingEntity1, containedEntity );
			primitives.containingAsUsedInCrossEntityDerivedProperty().set( containedEntity, deeplyNestedContainingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			primitives.containingAsUsedInCrossEntityDerivedProperty().clear( primitives.containedUsedInCrossEntityDerivedProperty().get( containingEntity1 ) );
			primitives.containedUsedInCrossEntityDerivedProperty().clear( containingEntity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	public interface SingleValuedAssociationModelPrimitives<TIndexed extends TContaining, TContaining, TContained>
			extends ModelPrimitives<TIndexed, TContaining, TContained> {

		PropertyAccessor<TContaining, TContained> containedNonIndexedEmbedded();

		PropertyAccessor<TContained, TContaining> containingAsNonIndexedEmbedded();

	}
}
