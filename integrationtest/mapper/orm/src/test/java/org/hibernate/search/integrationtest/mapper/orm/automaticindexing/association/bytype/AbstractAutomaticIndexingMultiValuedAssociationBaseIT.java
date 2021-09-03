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
 * with a multi-valued association.
 */
public abstract class AbstractAutomaticIndexingMultiValuedAssociationBaseIT<
				TIndexed extends TContaining, TContaining, TContained, TContainedAssociation
		>
		extends AbstractAutomaticIndexingAssociationBaseIT<
								TIndexed, TContaining, TContained
						> {

	/*
	 * Make sure that the values are in lexicographical order, so that SortedMap tests
	 * using these values as keys work correctly.
	 */
	private final String VALUE_1 = "1 - firstValue";
	private final String VALUE_2 = "2 - secondValue";

	private final MultiValuedModelPrimitives<TIndexed, TContaining, TContained, TContainedAssociation> primitives;

	public AbstractAutomaticIndexingMultiValuedAssociationBaseIT(
			MultiValuedModelPrimitives<TIndexed, TContaining, TContained, TContainedAssociation> primitives) {
		super( primitives );
		this.primitives = primitives;
	}

	@Test
	public void directMultiValuedAssociationUpdate_indexedEmbedded() {
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

			TContained contained = primitives.newContained( 2 );
			primitives.indexedField().set( contained, VALUE_1 );

			primitives.containedIndexedEmbedded().add( entity1 , contained );
			primitives.containingAsIndexedEmbedded().set( contained , entity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.indexedField().set( contained, VALUE_2 );

			primitives.containedIndexedEmbedded().add( entity1 , contained );
			primitives.containingAsIndexedEmbedded().set( contained , entity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_2 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = session.get( primitives.getContainedClass(), 2 );

			primitives.containingAsIndexedEmbedded().clear( contained );
			primitives.containedIndexedEmbedded().remove( entity1 , contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_2 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing an IndexedEmbedded association in an indexed entity
	 * does trigger reindexing of the entity.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void directMultiValuedAssociationReplace_indexedEmbedded() {
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContained contained = primitives.newContained( 2 );
			primitives.indexedField().set( contained, VALUE_1 );

			primitives.containedIndexedEmbedded().add( entity1 , contained );
			primitives.containingAsIndexedEmbedded().set( contained , entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.indexedField().set( contained, VALUE_2 );

			TContainedAssociation newAssociation = primitives.newContainedAssociation(
					primitives.containedIndexedEmbedded().getContainer( entity1 )
			);
			primitives.containedIndexedEmbedded().setContainer( entity1, newAssociation );
			primitives.containedIndexedEmbedded().add( entity1, contained );
			primitives.containingAsIndexedEmbedded().set( contained , entity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_2 )
							)
					);
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
	public void directMultiValuedAssociationMultiValuedUpdate_nonIndexedEmbedded() {
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

			TContained contained = primitives.newContained( 2 );
			primitives.indexedField().set( contained, VALUE_1 );

			primitives.containedNonIndexedEmbedded().add( entity1 , contained );
			primitives.containingAsNonIndexedEmbedded().set( contained , entity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.indexedField().set( contained, VALUE_2 );

			primitives.containedNonIndexedEmbedded().add( entity1 , contained );
			primitives.containingAsNonIndexedEmbedded().set( contained , entity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = session.get( primitives.getContainedClass(), 2 );

			primitives.containingAsNonIndexedEmbedded().clear( contained );
			primitives.containedNonIndexedEmbedded().remove( entity1 , contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing a non-IndexedEmbedded association in an entity
	 * whose other properties are indexed
	 * does not trigger reindexing of the entity.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3204")
	public void directMultiValuedAssociationReplace_nonIndexedEmbedded() {
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContained contained = primitives.newContained( 2 );
			primitives.indexedField().set( contained, VALUE_1 );

			primitives.containedNonIndexedEmbedded().add( entity1 , contained );
			primitives.containingAsNonIndexedEmbedded().set( contained , entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.indexedField().set( contained, VALUE_2 );

			TContainedAssociation newAssociation = primitives.newContainedAssociation(
					primitives.containedNonIndexedEmbedded().getContainer( entity1 )
			);
			primitives.containedNonIndexedEmbedded().setContainer( entity1, newAssociation );
			primitives.containedNonIndexedEmbedded().add( entity1 , contained );
			primitives.containingAsNonIndexedEmbedded().set( contained , entity1 );

			session.persist( contained );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> { } );
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
	public void directMultiValuedAssociationUpdate_indexedEmbeddedShallowReindexOnUpdate() {
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

			TContained contained = primitives.newContained( 2 );
			primitives.indexedField().set( contained, VALUE_1 );

			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().add( entity1 , contained );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().set( contained, entity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.indexedField().set( contained, VALUE_2 );

			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().add( entity1 , contained );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().set( contained, entity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
							.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b2 -> b2
									.field( "indexedField", VALUE_2 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = session.get( primitives.getContainedClass(), 2 );

			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().remove( entity1 , contained );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().clear( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b2 -> b2
									.field( "indexedField", VALUE_2 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing an IndexedEmbeddedNoReindexOnUpdate association in an indexed entity
	 * does trigger reindexing of the entity
	 * if the association is marked with ReindexOnUpdate = SHALLOW.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4001")
	public void directMultiValuedAssociationReplace_indexedEmbeddedShallowReindexOnUpdate() {
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContained contained = primitives.newContained( 2 );
			primitives.indexedField().set( contained, VALUE_1 );

			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().add( entity1 , contained );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().set( contained, entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.indexedField().set( contained, VALUE_2 );

			TContainedAssociation newAssociation = primitives.newContainedAssociation(
					primitives.containedIndexedEmbeddedShallowReindexOnUpdate().getContainer( entity1 )
			);
			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().setContainer( entity1, newAssociation );
			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().add( entity1 , contained );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().set( contained, entity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
							.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b2 -> b2
									.field( "indexedField", VALUE_2 )
							)
					);
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
	public void directMultiValuedAssociationUpdate_indexedEmbeddedNoReindexOnUpdate() {
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

			TContained contained = primitives.newContained( 2 );
			primitives.indexedField().set( contained, VALUE_1 );

			primitives.containedIndexedEmbeddedNoReindexOnUpdate().add( entity1 , contained );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().set( contained, entity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.indexedField().set( contained, VALUE_2 );

			primitives.containedIndexedEmbeddedNoReindexOnUpdate().add( entity1 , contained );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().set( contained, entity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = session.get( primitives.getContainedClass(), 2 );

			primitives.containedIndexedEmbeddedNoReindexOnUpdate().remove( entity1 , contained );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().clear( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing an IndexedEmbeddedNoReindexOnUpdate association in an indexed entity
	 * does not trigger reindexing of the entity
	 * if the association is marked with ReindexOnUpdate = NO.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3204")
	public void directMultiValuedAssociationReplace_indexedEmbeddedNoReindexOnUpdate() {
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContained contained = primitives.newContained( 2 );
			primitives.indexedField().set( contained, VALUE_1 );

			primitives.containedIndexedEmbeddedNoReindexOnUpdate().add( entity1 , contained );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().set( contained, entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.indexedField().set( contained, VALUE_2 );

			TContainedAssociation newAssociation = primitives.newContainedAssociation(
					primitives.containedIndexedEmbeddedNoReindexOnUpdate().getContainer( entity1 )
			);
			primitives.containedIndexedEmbeddedNoReindexOnUpdate().setContainer( entity1, newAssociation );
			primitives.containedIndexedEmbeddedNoReindexOnUpdate().add( entity1 , contained );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().set( contained, entity1 );

			session.persist( contained );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
							.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b2 -> b2
									.field( "indexedField", VALUE_2 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectMultiValuedAssociationUpdate_indexedEmbedded() {
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

			TContained contained = primitives.newContained( 4 );
			primitives.indexedField().set( contained, VALUE_1 );

			primitives.containedIndexedEmbedded().add( containingEntity1 , contained );
			primitives.containingAsIndexedEmbedded().set( contained , containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 5 );
			primitives.indexedField().set( contained, VALUE_2 );

			primitives.containedIndexedEmbedded().add( containingEntity1 , contained );
			primitives.containingAsIndexedEmbedded().set( contained , containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_2 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		withinTransaction( sessionFactory, session -> {
			TContaining deeplyNestedContainingEntity1 = session.get( primitives.getContainingClass(), 3 );

			TContained contained = primitives.newContained( 6 );
			primitives.indexedField().set( contained, "outOfScopeValue" );

			primitives.containedIndexedEmbedded().add( deeplyNestedContainingEntity1 , contained );
			primitives.containingAsIndexedEmbedded().set( contained , deeplyNestedContainingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = session.get( primitives.getContainedClass(), 4 );

			primitives.containingAsIndexedEmbedded().clear( contained );
			primitives.containedIndexedEmbedded().remove( containingEntity1 , contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_2 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing an IndexedEmbedded association in an entity
	 * that is IndexedEmbedded in an indexed entity
	 * does trigger reindexing of the indexed entity.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void indirectMultiValuedAssociationReplace_indexedEmbedded() {
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained = primitives.newContained( 3 );
			primitives.indexedField().set( contained, VALUE_1 );
			primitives.containedIndexedEmbedded().add( containingEntity1 , contained );
			primitives.containingAsIndexedEmbedded().set( contained , containingEntity1 );

			session.persist( contained );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 4 );
			primitives.indexedField().set( contained, VALUE_2 );

			TContainedAssociation newAssociation = primitives.newContainedAssociation(
					primitives.containedIndexedEmbedded().getContainer( containingEntity1 )
			);
			primitives.containedIndexedEmbedded().setContainer( containingEntity1, newAssociation );
			primitives.containedIndexedEmbedded().add( containingEntity1 , contained );
			primitives.containingAsIndexedEmbedded().set( contained , containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_2 )
									)
							)
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
	public void indirectMultiValuedAssociationUpdate_nonIndexedEmbedded() {
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

			TContained contained = primitives.newContained( 4 );
			primitives.indexedField().set( contained, VALUE_1 );

			primitives.containedNonIndexedEmbedded().add( containingEntity1 , contained );
			primitives.containingAsNonIndexedEmbedded().set( contained , containingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 5 );
			primitives.indexedField().set( contained, VALUE_2 );

			primitives.containedNonIndexedEmbedded().add( containingEntity1 , contained );
			primitives.containingAsNonIndexedEmbedded().set( contained , containingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = session.get( primitives.getContainedClass(), 4 );

			primitives.containingAsNonIndexedEmbedded().clear( contained );
			primitives.containedNonIndexedEmbedded().remove( containingEntity1 , contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing a non-IndexedEmbedded association in an entity
	 * whose properties are otherwise used in an IndexedEmbedded from an indexed entity
	 * does not trigger reindexing of the indexed entity.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3204")
	public void indirectMultiValuedAssociationReplace_nonIndexedEmbedded() {
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained = primitives.newContained( 3 );
			primitives.indexedField().set( contained, VALUE_1 );
			primitives.containedNonIndexedEmbedded().add( containingEntity1 , contained );
			primitives.containingAsNonIndexedEmbedded().set( contained , containingEntity1 );

			session.persist( contained );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 4 );
			primitives.indexedField().set( contained, VALUE_2 );

			TContainedAssociation newAssociation = primitives.newContainedAssociation(
					primitives.containedNonIndexedEmbedded().getContainer( containingEntity1 )
			);
			primitives.containedNonIndexedEmbedded().setContainer( containingEntity1, newAssociation );
			primitives.containedNonIndexedEmbedded().add( containingEntity1 , contained );
			primitives.containingAsNonIndexedEmbedded().set( contained , containingEntity1 );

			session.persist( contained );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
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
	 * does trigger reindexing of the indexed entity
	 * if the first association is marked with ReindexOnUpdate = SHALLOW.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4001")
	public void indirectMultiValuedAssociationUpdate_indexedEmbeddedShallowReindexOnUpdate() {
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

			TContained contained = primitives.newContained( 4 );
			primitives.indexedField().set( contained, VALUE_1 );

			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().add( containingEntity1 , contained );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().set( contained, containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 5 );
			primitives.indexedField().set( contained, VALUE_2 );

			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().add( containingEntity1 , contained );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().set( contained, containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
									.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b3 -> b3
											.field( "indexedField", VALUE_2 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = session.get( primitives.getContainedClass(), 4 );

			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().remove( containingEntity1 , contained );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().clear( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b3 -> b3
											.field( "indexedField", VALUE_2 )
									)
							)
					);

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing an IndexedEmbedded association in an entity
	 * that is IndexedEmbedded in an indexed entity
	 * does trigger reindexing of the indexed entity
	 * if the first association is marked with ReindexOnUpdate = SHALLOW.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4001")
	public void indirectMultiValuedAssociationReplace_indexedEmbeddedShallowReindexOnUpdate() {
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained = primitives.newContained( 3 );
			primitives.indexedField().set( contained, VALUE_1 );
			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().add( containingEntity1 , contained );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().set( contained, containingEntity1 );

			session.persist( contained );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 4 );
			primitives.indexedField().set( contained, VALUE_2 );

			TContainedAssociation newAssociation = primitives.newContainedAssociation(
					primitives.containedIndexedEmbeddedShallowReindexOnUpdate().getContainer( containingEntity1 )
			);
			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().setContainer( containingEntity1, newAssociation );
			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().add( containingEntity1 , contained );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().set( contained, containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
									.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b3 -> b3
											.field( "indexedField", VALUE_2 )
									)
							)
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
	public void indirectMultiValuedAssociationUpdate_indexedEmbeddedNoReindexOnUpdate() {
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

			TContained contained = primitives.newContained( 4 );
			primitives.indexedField().set( contained, VALUE_1 );

			primitives.containedIndexedEmbeddedNoReindexOnUpdate().add( containingEntity1 , contained );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().set( contained, containingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 5 );
			primitives.indexedField().set( contained, VALUE_2 );

			primitives.containedIndexedEmbeddedNoReindexOnUpdate().add( containingEntity1 , contained );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().set( contained, containingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = session.get( primitives.getContainedClass(), 4 );

			primitives.containedIndexedEmbeddedNoReindexOnUpdate().remove( containingEntity1 , contained );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().clear( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing an IndexedEmbedded association in an entity
	 * that is IndexedEmbedded in an indexed entity
	 * does not trigger reindexing of the indexed entity
	 * if the first association is marked with ReindexOnUpdate = NO.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3204")
	public void indirectMultiValuedAssociationReplace_indexedEmbeddedNoReindexOnUpdate() {
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained = primitives.newContained( 3 );
			primitives.indexedField().set( contained, VALUE_1 );
			primitives.containedIndexedEmbeddedNoReindexOnUpdate().add( containingEntity1 , contained );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().set( contained, containingEntity1 );

			session.persist( contained );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 4 );
			primitives.indexedField().set( contained, VALUE_2 );

			TContainedAssociation newAssociation = primitives.newContainedAssociation(
					primitives.containedIndexedEmbeddedNoReindexOnUpdate().getContainer( containingEntity1 )
			);
			primitives.containedIndexedEmbeddedNoReindexOnUpdate().setContainer( containingEntity1, newAssociation );
			primitives.containedIndexedEmbeddedNoReindexOnUpdate().add( containingEntity1, contained );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().set( contained, containingEntity1 );

			session.persist( contained );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
									.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b3 -> b3
											.field( "indexedField", VALUE_2 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	public interface MultiValuedModelPrimitives<
					TIndexed extends TContaining,
					TContaining,
					TContained,
					TContainedAssociation
			> extends ModelPrimitives<TIndexed, TContaining, TContained> {

		@Override
		default boolean isMultiValuedAssociation() {
			return true;
		}

		@Override
		default boolean isAssociationLazyOnContainingSide() {
			return true;
		}

		TContainedAssociation newContainedAssociation(TContainedAssociation original);

		@Override
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedIndexedEmbedded();

		@Override
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedNonIndexedEmbedded();

		@Override
		PropertyAccessor<TContained, TContaining> containingAsNonIndexedEmbedded();

		@Override
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedIndexedEmbeddedShallowReindexOnUpdate();

		@Override
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedIndexedEmbeddedNoReindexOnUpdate();

		@Override
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedUsedInCrossEntityDerivedProperty();

		@Override
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedIndexedEmbeddedWithCast();

	}
}
