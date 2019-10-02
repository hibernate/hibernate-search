/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

/**
 * An abstract base for tests dealing with automatic indexing based on Hibernate ORM entity events
 * and involving a multi-valued association.
 * <p>
 * See {@link AbstractAutomaticIndexingAssociationIT} for more details on how this test is designed.
 */
public abstract class AbstractAutomaticIndexingMultiAssociationIT<
		TIndexed extends TContaining, TContaining, TContained,
		TContainedAssociation, TContainingAssociation
		>
		extends AbstractAutomaticIndexingAssociationIT<
				TIndexed, TContaining, TContained
				> {

	/*
	 * Make sure that the values are in lexicographical order, so that SortedMap tests
	 * using these values as keys work correctly.
	 */
	private final String VALUE_1 = "1 - firstValue";
	private final String VALUE_2 = "2 - secondValue";

	private final MultiAssociationModelPrimitives<TIndexed, TContaining, TContained,
			TContainedAssociation, TContainingAssociation> primitives;

	AbstractAutomaticIndexingMultiAssociationIT(
			MultiAssociationModelPrimitives<TIndexed, TContaining, TContained,
					TContainedAssociation, TContainingAssociation> primitives) {
		super( primitives );
		this.primitives = primitives;
	}

	@Test
	public void directAssociationUpdate_indexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> { } )
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 2 );
			primitives.setIndexedField( contained, VALUE_1 );

			primitives.addContained( primitives.getContainedIndexedEmbedded( entity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained ), entity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.setIndexedField( contained, VALUE_2 );

			primitives.addContained( primitives.getContainedIndexedEmbedded( entity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained ), entity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_2 )
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = session.get( primitives.getContainedClass(), 2 );

			primitives.removeContaining( primitives.getContainingAsIndexedEmbedded( contained ), entity1 );
			primitives.removeContained( primitives.getContainedIndexedEmbedded( entity1 ), contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_2 )
							)
					)
					.processedThenExecuted();
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
	public void directAssociationReplace_indexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContained contained = primitives.newContained( 2 );
			primitives.setIndexedField( contained, VALUE_1 );

			primitives.addContained( primitives.getContainedIndexedEmbedded( entity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained ), entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.setIndexedField( contained, VALUE_2 );

			TContainedAssociation newAssociation = primitives.newContainedAssociation(
					primitives.getContainedIndexedEmbedded( entity1 )
			);
			primitives.addContained( newAssociation, contained );
			primitives.setContainedIndexedEmbedded( entity1, newAssociation );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained ), entity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_2 )
							)
					)
					.processedThenExecuted();
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
			TIndexed entity1 = primitives.newIndexed( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> { } )
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 2 );
			primitives.setIndexedField( contained, VALUE_1 );

			primitives.addContained( primitives.getContainedNonIndexedEmbedded( entity1 ), contained );
			primitives.addContaining( primitives.getContainingAsNonIndexedEmbedded( contained ), entity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.setIndexedField( contained, VALUE_2 );

			primitives.addContained( primitives.getContainedNonIndexedEmbedded( entity1 ), contained );
			primitives.addContaining( primitives.getContainingAsNonIndexedEmbedded( contained ), entity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = session.get( primitives.getContainedClass(), 2 );

			primitives.removeContaining( primitives.getContainingAsNonIndexedEmbedded( contained ), entity1 );
			primitives.removeContained( primitives.getContainedNonIndexedEmbedded( entity1 ), contained );

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
	public void directAssociationReplace_nonIndexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContained contained = primitives.newContained( 2 );
			primitives.setIndexedField( contained, VALUE_1 );

			primitives.addContained( primitives.getContainedNonIndexedEmbedded( entity1 ), contained );
			primitives.addContaining( primitives.getContainingAsNonIndexedEmbedded( contained ), entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> { } )
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.setIndexedField( contained, VALUE_2 );

			TContainedAssociation newAssociation = primitives.newContainedAssociation(
					primitives.getContainedNonIndexedEmbedded( entity1 )
			);
			primitives.addContained( newAssociation, contained );
			primitives.setContainedNonIndexedEmbedded( entity1, newAssociation );
			primitives.addContaining( primitives.getContainingAsNonIndexedEmbedded( contained ), entity1 );

			session.persist( contained );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> { } )
					.processedThenExecuted();
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
			TIndexed entity1 = primitives.newIndexed( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> { } )
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 2 );
			primitives.setIndexedField( contained, VALUE_1 );

			primitives.addContained( primitives.getContainedIndexedEmbeddedNoReindexOnUpdate( entity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbeddedNoReindexOnUpdate( contained ), entity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.setIndexedField( contained, VALUE_2 );

			primitives.addContained( primitives.getContainedIndexedEmbeddedNoReindexOnUpdate( entity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbeddedNoReindexOnUpdate( contained ), entity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = session.get( primitives.getContainedClass(), 2 );

			primitives.removeContaining( primitives.getContainingAsIndexedEmbeddedNoReindexOnUpdate( contained ), entity1 );
			primitives.removeContained( primitives.getContainedIndexedEmbeddedNoReindexOnUpdate( entity1 ), contained );

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
	public void directAssociationReplace_indexedEmbeddedNoReindexOnUpdate() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContained contained = primitives.newContained( 2 );
			primitives.setIndexedField( contained, VALUE_1 );

			primitives.addContained( primitives.getContainedIndexedEmbeddedNoReindexOnUpdate( entity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbeddedNoReindexOnUpdate( contained ), entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.setIndexedField( contained, VALUE_2 );

			TContainedAssociation newAssociation = primitives.newContainedAssociation(
					primitives.getContainedIndexedEmbeddedNoReindexOnUpdate( entity1 )
			);
			primitives.addContained( newAssociation, contained );
			primitives.setContainedIndexedEmbeddedNoReindexOnUpdate( entity1, newAssociation );
			primitives.addContaining( primitives.getContainingAsIndexedEmbeddedNoReindexOnUpdate( contained ), entity1 );

			session.persist( contained );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
							.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b2 -> b2
									.field( "indexedField", VALUE_2 )
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_indexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = primitives.newContaining( 3 );
			primitives.setChild( containingEntity1, deeplyNestedContainingEntity );
			primitives.setParent( deeplyNestedContainingEntity, containingEntity1 );

			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 4 );
			primitives.setIndexedField( contained, VALUE_1 );

			primitives.addContained( primitives.getContainedIndexedEmbedded( containingEntity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained ), containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 5 );
			primitives.setIndexedField( contained, VALUE_2 );

			primitives.addContained( primitives.getContainedIndexedEmbedded( containingEntity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained ), containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_2 )
									)
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining deeplyNestedContainingEntity1 = session.get( primitives.getContainingClass(), 3 );

			TContained contained = primitives.newContained( 6 );
			primitives.setIndexedField( contained, "outOfScopeValue" );

			primitives.addContained( primitives.getContainedIndexedEmbedded( deeplyNestedContainingEntity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained ), deeplyNestedContainingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = session.get( primitives.getContainedClass(), 4 );

			primitives.removeContaining( primitives.getContainingAsIndexedEmbedded( contained ), containingEntity1 );
			primitives.removeContained( primitives.getContainedIndexedEmbedded( containingEntity1 ), contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_2 )
									)
							)
					)
					.processedThenExecuted();
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
	public void indirectAssociationReplace_indexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			TContained contained = primitives.newContained( 3 );
			primitives.setIndexedField( contained, VALUE_1 );
			primitives.addContained( primitives.getContainedIndexedEmbedded( containingEntity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained ), containingEntity1 );

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
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 4 );
			primitives.setIndexedField( contained, VALUE_2 );

			TContainedAssociation newAssociation = primitives.newContainedAssociation(
					primitives.getContainedIndexedEmbedded( containingEntity1 )
			);
			primitives.addContained( newAssociation, contained );
			primitives.setContainedIndexedEmbedded( containingEntity1, newAssociation );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained ), containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_2 )
									)
							)
					)
					.processedThenExecuted();
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
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 4 );
			primitives.setIndexedField( contained, VALUE_1 );

			primitives.addContained( primitives.getContainedNonIndexedEmbedded( containingEntity1 ), contained );
			primitives.addContaining( primitives.getContainingAsNonIndexedEmbedded( contained ), containingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 5 );
			primitives.setIndexedField( contained, VALUE_2 );

			primitives.addContained( primitives.getContainedNonIndexedEmbedded( containingEntity1 ), contained );
			primitives.addContaining( primitives.getContainingAsNonIndexedEmbedded( contained ), containingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = session.get( primitives.getContainedClass(), 4 );

			primitives.removeContaining( primitives.getContainingAsNonIndexedEmbedded( contained ), containingEntity1 );
			primitives.removeContained( primitives.getContainedNonIndexedEmbedded( containingEntity1 ), contained );

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
	public void indirectAssociationReplace_nonIndexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			TContained contained = primitives.newContained( 3 );
			primitives.setIndexedField( contained, VALUE_1 );
			primitives.addContained( primitives.getContainedNonIndexedEmbedded( containingEntity1 ), contained );
			primitives.addContaining( primitives.getContainingAsNonIndexedEmbedded( contained ), containingEntity1 );

			session.persist( contained );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 4 );
			primitives.setIndexedField( contained, VALUE_2 );

			TContainedAssociation newAssociation = primitives.newContainedAssociation(
					primitives.getContainedNonIndexedEmbedded( containingEntity1 )
			);
			primitives.addContained( newAssociation, contained );
			primitives.setContainedNonIndexedEmbedded( containingEntity1, newAssociation );
			primitives.addContaining( primitives.getContainingAsNonIndexedEmbedded( contained ), containingEntity1 );

			session.persist( contained );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> { } )
					)
					.processedThenExecuted();
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
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 4 );
			primitives.setIndexedField( contained, VALUE_1 );

			primitives.addContained( primitives.getContainedIndexedEmbeddedNoReindexOnUpdate( containingEntity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbeddedNoReindexOnUpdate( contained ), containingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 5 );
			primitives.setIndexedField( contained, VALUE_2 );

			primitives.addContained( primitives.getContainedIndexedEmbeddedNoReindexOnUpdate( containingEntity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbeddedNoReindexOnUpdate( contained ), containingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = session.get( primitives.getContainedClass(), 4 );

			primitives.removeContaining( primitives.getContainingAsIndexedEmbeddedNoReindexOnUpdate( contained ), containingEntity1 );
			primitives.removeContained( primitives.getContainedIndexedEmbeddedNoReindexOnUpdate( containingEntity1 ), contained );

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
	public void indirectAssociationReplace_indexedEmbeddedNoReindexOnUpdate() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			TContained contained = primitives.newContained( 3 );
			primitives.setIndexedField( contained, VALUE_1 );
			primitives.addContained( primitives.getContainedIndexedEmbeddedNoReindexOnUpdate( containingEntity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbeddedNoReindexOnUpdate( contained ), containingEntity1 );

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
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 4 );
			primitives.setIndexedField( contained, VALUE_2 );

			TContainedAssociation newAssociation = primitives.newContainedAssociation(
					primitives.getContainedIndexedEmbeddedNoReindexOnUpdate( containingEntity1 )
			);
			primitives.addContained( newAssociation, contained );
			primitives.setContainedIndexedEmbeddedNoReindexOnUpdate( containingEntity1, newAssociation );
			primitives.addContaining( primitives.getContainingAsIndexedEmbeddedNoReindexOnUpdate( contained ), containingEntity1 );

			session.persist( contained );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
									.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b3 -> b3
											.field( "indexedField", VALUE_2 )
									)
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_indexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = primitives.newContaining( 3 );
			primitives.setChild( containingEntity1, deeplyNestedContainingEntity );
			primitives.setParent( deeplyNestedContainingEntity, containingEntity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.setIndexedField( contained1, "initialValue" );
			primitives.addContained( primitives.getContainedIndexedEmbedded( containingEntity1 ), contained1 );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained1 ), containingEntity1 );

			TContained contained2 = primitives.newContained( 5 );
			primitives.setIndexedField( contained2, "initialOutOfScopeValue" );
			primitives.addContained( primitives.getContainedIndexedEmbedded( deeplyNestedContainingEntity ), contained2 );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained2 ), deeplyNestedContainingEntity );

			session.persist( contained1 );
			session.persist( contained2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "initialValue" )
									)
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.setIndexedField( contained, "updatedValue" );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "updatedValue" )
									)
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 5 );
			primitives.setIndexedField( contained, "updatedOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3206")
	public void indirectValueUpdate_indexedEmbeddedNoReindexOnUpdate() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.setIndexedField( contained1, "initialValue" );
			primitives.addContained( primitives.getContainedIndexedEmbeddedNoReindexOnUpdate( containingEntity1 ), contained1 );
			primitives.addContaining( primitives.getContainingAsIndexedEmbeddedNoReindexOnUpdate( contained1 ), containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b3 -> b3
											.field( "indexedField", "initialValue" )
									)
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.setIndexedField( contained, "updatedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	interface MultiAssociationModelPrimitives<
			TIndexed extends TContaining,
			TContaining,
			TContained,
			TContainedAssociation,
			TContainingAssociation
			> extends AssociationModelPrimitives<TIndexed, TContaining, TContained> {

		@Override
		default boolean isMultiValuedAssociation() {
			return true;
		}

		@Override
		default void setContainedIndexedEmbeddedSingle(TContaining containing, TContained contained) {
			TContainedAssociation containedAssociation = getContainedIndexedEmbedded( containing );
			clearContained( containedAssociation );
			addContained( containedAssociation, contained );
		}

		@Override
		default void setContainingAsIndexedEmbeddedSingle(TContained contained, TContaining containing) {
			TContainingAssociation containingAssociation = getContainingAsIndexedEmbedded( contained );
			clearContaining( containingAssociation );
			addContaining( containingAssociation, containing );
		}

		@Override
		default void setContainedIndexedEmbeddedNoReindexOnUpdateSingle(TContaining containing, TContained contained) {
			TContainedAssociation containedAssociation = getContainedIndexedEmbeddedNoReindexOnUpdate( containing );
			clearContained( containedAssociation );
			addContained( containedAssociation, contained );
		}

		@Override
		default void setContainingAsIndexedEmbeddedNoReindexOnUpdateSingle(TContained contained, TContaining containing) {
			TContainingAssociation containingAssociation = getContainingAsIndexedEmbeddedNoReindexOnUpdate( contained );
			clearContaining( containingAssociation );
			addContaining( containingAssociation, containing );
		}

		@Override
		default void setContainedUsedInCrossEntityDerivedPropertySingle(TContaining containing,
				TContained contained) {
			TContainedAssociation containedAssociation = getContainedUsedInCrossEntityDerivedProperty( containing );
			clearContained( containedAssociation );
			addContained( containedAssociation, contained );
		}

		@Override
		default void setContainingAsUsedInCrossEntityDerivedPropertySingle(TContained contained,
				TContaining containing) {
			TContainingAssociation containingAssociation = getContainingAsUsedInCrossEntityDerivedProperty( contained );
			clearContaining( containingAssociation );
			addContaining( containingAssociation, containing );
		}

		TContainedAssociation newContainedAssociation(TContainedAssociation original);

		void addContained(TContainedAssociation association, TContained contained);

		void removeContained(TContainedAssociation association, TContained contained);

		void clearContained(TContainedAssociation association);

		void addContaining(TContainingAssociation association, TContaining containing);

		void removeContaining(TContainingAssociation association, TContaining containing);

		void clearContaining(TContainingAssociation association);

		TContainedAssociation getContainedIndexedEmbedded(TContaining containing);

		void setContainedIndexedEmbedded(TContaining containing, TContainedAssociation association);

		TContainingAssociation getContainingAsIndexedEmbedded(TContained contained);

		TContainedAssociation getContainedNonIndexedEmbedded(TContaining containing);

		void setContainedNonIndexedEmbedded(TContaining containing, TContainedAssociation association);

		TContainingAssociation getContainingAsNonIndexedEmbedded(TContained contained);

		TContainedAssociation getContainedIndexedEmbeddedNoReindexOnUpdate(TContaining containing);

		void setContainedIndexedEmbeddedNoReindexOnUpdate(TContaining containing, TContainedAssociation association);

		TContainingAssociation getContainingAsIndexedEmbeddedNoReindexOnUpdate(TContained contained);

		TContainedAssociation getContainedUsedInCrossEntityDerivedProperty(TContaining containing);

		TContainingAssociation getContainingAsUsedInCrossEntityDerivedProperty(TContained contained);
	}

}
