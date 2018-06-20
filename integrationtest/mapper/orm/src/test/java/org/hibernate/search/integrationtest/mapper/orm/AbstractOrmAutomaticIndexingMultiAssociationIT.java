/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm;

import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

/**
 * An abstract base for tests dealing with automatic indexing based on Hibernate ORM entity events
 * and involving a multi-valued association.
 * <p>
 * See {@link AbstractOrmAutomaticIndexingAssociationIT} for more details on how this test is designed.
 */
public abstract class AbstractOrmAutomaticIndexingMultiAssociationIT<
		TIndexed extends TContaining, TContaining, TContained,
		TContainedAssociation, TContainingAssociation
		>
		extends AbstractOrmAutomaticIndexingAssociationIT<
		TIndexed, TContaining, TContained
		> {

	private final MultiAssociationModelPrimitives<TIndexed, TContaining, TContained,
			TContainedAssociation, TContainingAssociation> primitives;

	AbstractOrmAutomaticIndexingMultiAssociationIT(
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
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 2 );
			primitives.setIndexedField( contained, "firstValue" );

			primitives.addContained( primitives.getContainedIndexedEmbedded( entity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained ), entity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", "firstValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.setIndexedField( contained, "secondValue" );

			primitives.addContained( primitives.getContainedIndexedEmbedded( entity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained ), entity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", "firstValue" )
							)
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", "secondValue" )
							)
					)
					.preparedThenExecuted();
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
									.field( "indexedField", "secondValue" )
							)
					)
					.preparedThenExecuted();
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
			primitives.setIndexedField( contained, "firstValue" );

			primitives.addContained( primitives.getContainedIndexedEmbedded( entity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained ), entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", "firstValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.setIndexedField( contained, "secondValue" );

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
									.field( "indexedField", "firstValue" )
							)
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", "secondValue" )
							)
					)
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
			TIndexed entity1 = primitives.newIndexed( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> { } )
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 2 );
			primitives.setIndexedField( contained, "firstValue" );

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
			primitives.setIndexedField( contained, "secondValue" );

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
			primitives.setIndexedField( contained, "firstValue" );

			primitives.addContained( primitives.getContainedNonIndexedEmbedded( entity1 ), contained );
			primitives.addContaining( primitives.getContainingAsNonIndexedEmbedded( contained ), entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> { } )
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained contained = primitives.newContained( 3 );
			primitives.setIndexedField( contained, "secondValue" );

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
					.preparedThenExecuted();
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
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 4 );
			primitives.setIndexedField( contained, "firstValue" );

			primitives.addContained( primitives.getContainedIndexedEmbedded( containingEntity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained ), containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "firstValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 5 );
			primitives.setIndexedField( contained, "secondValue" );

			primitives.addContained( primitives.getContainedIndexedEmbedded( containingEntity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained ), containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "firstValue" )
									)
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "secondValue" )
									)
							)
					)
					.preparedThenExecuted();
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
											.field( "indexedField", "secondValue" )
									)
							)
					)
					.preparedThenExecuted();
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
			primitives.setIndexedField( contained, "firstValue" );
			primitives.addContained( primitives.getContainedIndexedEmbedded( containingEntity1 ), contained );
			primitives.addContaining( primitives.getContainingAsIndexedEmbedded( contained ), containingEntity1 );

			session.persist( contained );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "firstValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 4 );
			primitives.setIndexedField( contained, "secondValue" );

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
											.field( "indexedField", "firstValue" )
									)
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "secondValue" )
									)
							)
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
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 4 );
			primitives.setIndexedField( contained, "firstValue" );

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
			primitives.setIndexedField( contained, "secondValue" );

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
			primitives.setIndexedField( contained, "firstValue" );
			primitives.addContained( primitives.getContainedNonIndexedEmbedded( containingEntity1 ), contained );
			primitives.addContaining( primitives.getContainingAsNonIndexedEmbedded( contained ), containingEntity1 );

			session.persist( contained );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained contained = primitives.newContained( 4 );
			primitives.setIndexedField( contained, "secondValue" );

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
					.preparedThenExecuted();
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
	}

}
