/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype;

import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor.MultiValuedPropertyAccessor;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor.PropertyAccessor;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

/**
 * Abstract base for tests of automatic indexing caused by association updates
 * or by updates of associated (contained) entities,
 * with a multi-valued association.
 */
public abstract class AbstractAutomaticIndexingMultiValuedAssociationBaseIT<
		TIndexed extends TContaining,
		TContaining,
		TContainingEmbeddable,
		TContained,
		TContainedEmbeddable,
		TContainedAssociation>
		extends AbstractAutomaticIndexingAssociationBaseIT<
				TIndexed,
				TContaining,
				TContainingEmbeddable,
				TContained,
				TContainedEmbeddable> {

	private final ContainingEntityPrimitives<TContaining,
			TContainingEmbeddable,
			TContained,
			TContainedAssociation> containingPrimitives;
	private final ContainingEmbeddablePrimitives<TContainingEmbeddable,
			TContained,
			TContainedAssociation> containingEmbeddablePrimitives;

	public AbstractAutomaticIndexingMultiValuedAssociationBaseIT(IndexedEntityPrimitives<TIndexed> indexedPrimitives,
			ContainingEntityPrimitives<TContaining,
					TContainingEmbeddable,
					TContained,
					TContainedAssociation> containingPrimitives,
			ContainingEmbeddablePrimitives<TContainingEmbeddable,
					TContained,
					TContainedAssociation> containingEmbeddablePrimitives,
			ContainedEntityPrimitives<TContained, TContainedEmbeddable, TContaining> containedPrimitives,
			ContainedEmbeddablePrimitives<TContainedEmbeddable, TContaining> containedEmbeddablePrimitives) {
		super( indexedPrimitives, containingPrimitives, containingEmbeddablePrimitives, containedPrimitives,
				containedEmbeddablePrimitives );
		this.containingPrimitives = containingPrimitives;
		this.containingEmbeddablePrimitives = containingEmbeddablePrimitives;
	}

	@Override
	protected ContainingEntityPrimitives<TContaining, TContainingEmbeddable, TContained, TContainedAssociation> _containing() {
		return containingPrimitives;
	}

	@Override
	protected ContainingEmbeddablePrimitives<TContainingEmbeddable, TContained, TContainedAssociation> _containingEmbeddable() {
		return containingEmbeddablePrimitives;
	}

	@Override
	protected final boolean isAssociationMultiValuedOnContainingSide() {
		return true;
	}

	@Override
	protected boolean isAssociationLazyOnContainingSide() {
		return true;
	}

	@Test
	public void directMultiValuedAssociationUpdate_indexedEmbedded() {
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> {} );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
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
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = session.get( _contained().entityClass(), 2 );

			containedAssociation.clear( contained );
			containingAssociation.remove( entity1, contained );

			backendMock.expectWorks( _indexed().indexName() )
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
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			TContainedAssociation newAssociation = _containing().newContainedAssociation(
					containingAssociation.getContainer( entity1 )
			);
			containingAssociation.setContainer( entity1, newAssociation );
			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
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
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().containedNonIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsNonIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> {} );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = session.get( _contained().entityClass(), 2 );

			containedAssociation.clear( contained );
			containingAssociation.remove( entity1, contained );

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
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().containedNonIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsNonIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> {} );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			TContainedAssociation newAssociation = _containing().newContainedAssociation(
					containingAssociation.getContainer( entity1 )
			);
			containingAssociation.setContainer( entity1, newAssociation );
			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> {} );
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
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().containedIndexedEmbeddedShallowReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation =
				_contained().containingAsIndexedEmbeddedShallowReindexOnUpdate();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> {} );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
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
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = session.get( _contained().entityClass(), 2 );

			containingAssociation.remove( entity1, contained );
			containedAssociation.clear( contained );

			backendMock.expectWorks( _indexed().indexName() )
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
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().containedIndexedEmbeddedShallowReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation =
				_contained().containingAsIndexedEmbeddedShallowReindexOnUpdate();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			TContainedAssociation newAssociation = _containing().newContainedAssociation(
					containingAssociation.getContainer( entity1 )
			);
			containingAssociation.setContainer( entity1, newAssociation );
			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
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
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().containedIndexedEmbeddedNoReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation =
				_contained().containingAsIndexedEmbeddedNoReindexOnUpdate();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> {} );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = session.get( _contained().entityClass(), 2 );

			containingAssociation.remove( entity1, contained );
			containedAssociation.clear( contained );

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
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().containedIndexedEmbeddedNoReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation =
				_contained().containingAsIndexedEmbeddedNoReindexOnUpdate();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			TContainedAssociation newAssociation = _containing().newContainedAssociation(
					containingAssociation.getContainer( entity1 )
			);
			containingAssociation.setContainer( entity1, newAssociation );
			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( _indexed().indexName() )
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
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public void directMultiValuedAssociationUpdate_embeddedAssociationsIndexedEmbedded() {
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().embeddedAssociations()
						.andThen( _containingEmbeddable()::newInstance, _containingEmbeddable().containedIndexedEmbedded() );
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().embeddedAssociations()
				.andThen( _containedEmbeddable()::newInstance, _containedEmbeddable().containingAsIndexedEmbedded() );
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> {} );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "embeddedAssociations", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "embeddedAssociations", b2 -> b2
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

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = session.get( _contained().entityClass(), 2 );

			containedAssociation.clear( contained );
			containingAssociation.remove( entity1, contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "embeddedAssociations", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_2 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public void directMultiValuedAssociationReplace_embeddedAssociationsIndexedEmbedded() {
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().embeddedAssociations()
						.andThen( _containingEmbeddable()::newInstance, _containingEmbeddable().containedIndexedEmbedded() );
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().embeddedAssociations()
				.andThen( _containedEmbeddable()::newInstance, _containedEmbeddable().containingAsIndexedEmbedded() );
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "embeddedAssociations", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			TContainedAssociation newAssociation = _containing().newContainedAssociation(
					containingAssociation.getContainer( entity1 )
			);
			containingAssociation.setContainer( entity1, newAssociation );
			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "embeddedAssociations", b2 -> b2
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public void directMultiValuedAssociationMultiValuedUpdate_embeddedAssociationsNonIndexedEmbedded() {
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().embeddedAssociations()
						.andThen( _containingEmbeddable()::newInstance, _containingEmbeddable().containedNonIndexedEmbedded() );
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().embeddedAssociations()
				.andThen( _containedEmbeddable()::newInstance, _containedEmbeddable().containingAsNonIndexedEmbedded() );
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> {} );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = session.get( _contained().entityClass(), 2 );

			containedAssociation.clear( contained );
			containingAssociation.remove( entity1, contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public void directMultiValuedAssociationReplace_embeddedAssociationsNonIndexedEmbedded() {
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().embeddedAssociations()
						.andThen( _containingEmbeddable()::newInstance, _containingEmbeddable().containedNonIndexedEmbedded() );
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().embeddedAssociations()
				.andThen( _containedEmbeddable()::newInstance, _containedEmbeddable().containingAsNonIndexedEmbedded() );
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> {} );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			TContainedAssociation newAssociation = _containing().newContainedAssociation(
					containingAssociation.getContainer( entity1 )
			);
			containingAssociation.setContainer( entity1, newAssociation );
			containingAssociation.add( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> {} );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectMultiValuedAssociationUpdate_indexedEmbedded() {
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = _containing().newInstance( 3 );
			_containing().child().set( containingEntity1, deeplyNestedContainingEntity );
			_containing().parent().set( deeplyNestedContainingEntity, containingEntity1 );

			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> {} )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
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
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 5 );
			field.set( contained, VALUE_2 );

			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
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
		setupHolder.runInTransaction( session -> {
			TContaining deeplyNestedContainingEntity1 = session.get( _containing().entityClass(), 3 );

			TContained contained = _contained().newInstance( 6 );
			field.set( contained, "outOfScopeValue" );

			containingAssociation.add( deeplyNestedContainingEntity1, contained );
			containedAssociation.set( contained, deeplyNestedContainingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = session.get( _contained().entityClass(), 4 );

			containedAssociation.clear( contained );
			containingAssociation.remove( containingEntity1, contained );

			backendMock.expectWorks( _indexed().indexName() )
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
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_1 );
			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_2 );

			TContainedAssociation newAssociation = _containing().newContainedAssociation(
					containingAssociation.getContainer( containingEntity1 )
			);
			containingAssociation.setContainer( containingEntity1, newAssociation );
			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
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
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().containedNonIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsNonIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> {} )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 5 );
			field.set( contained, VALUE_2 );

			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = session.get( _contained().entityClass(), 4 );

			containedAssociation.clear( contained );
			containingAssociation.remove( containingEntity1, contained );

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
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().containedNonIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsNonIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_1 );
			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> {} )
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_2 );

			TContainedAssociation newAssociation = _containing().newContainedAssociation(
					containingAssociation.getContainer( containingEntity1 )
			);
			containingAssociation.setContainer( containingEntity1, newAssociation );
			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> {} )
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
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().containedIndexedEmbeddedShallowReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation =
				_contained().containingAsIndexedEmbeddedShallowReindexOnUpdate();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> {} )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
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
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 5 );
			field.set( contained, VALUE_2 );

			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
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
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = session.get( _contained().entityClass(), 4 );

			containingAssociation.remove( containingEntity1, contained );
			containedAssociation.clear( contained );

			backendMock.expectWorks( _indexed().indexName() )
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
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().containedIndexedEmbeddedShallowReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation =
				_contained().containingAsIndexedEmbeddedShallowReindexOnUpdate();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_1 );
			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_2 );

			TContainedAssociation newAssociation = _containing().newContainedAssociation(
					containingAssociation.getContainer( containingEntity1 )
			);
			containingAssociation.setContainer( containingEntity1, newAssociation );
			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
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
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().containedIndexedEmbeddedNoReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation =
				_contained().containingAsIndexedEmbeddedNoReindexOnUpdate();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> {} )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 5 );
			field.set( contained, VALUE_2 );

			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = session.get( _contained().entityClass(), 4 );

			containingAssociation.remove( containingEntity1, contained );
			containedAssociation.clear( contained );

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
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().containedIndexedEmbeddedNoReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation =
				_contained().containingAsIndexedEmbeddedNoReindexOnUpdate();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_1 );
			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_2 );

			TContainedAssociation newAssociation = _containing().newContainedAssociation(
					containingAssociation.getContainer( containingEntity1 )
			);
			containingAssociation.setContainer( containingEntity1, newAssociation );
			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( _indexed().indexName() )
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public void indirectMultiValuedAssociationUpdate_embeddedAssociationsIndexedEmbedded() {
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().embeddedAssociations()
						.andThen( _containingEmbeddable()::newInstance, _containingEmbeddable().containedIndexedEmbedded() );
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().embeddedAssociations()
				.andThen( _containedEmbeddable()::newInstance, _containedEmbeddable().containingAsIndexedEmbedded() );
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = _containing().newInstance( 3 );
			_containing().child().set( containingEntity1, deeplyNestedContainingEntity );
			_containing().parent().set( deeplyNestedContainingEntity, containingEntity1 );

			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> {} )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "embeddedAssociations", b3 -> b3
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", VALUE_1 )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 5 );
			field.set( contained, VALUE_2 );

			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "embeddedAssociations", b3 -> b3
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", VALUE_1 )
											)
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", VALUE_2 )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		setupHolder.runInTransaction( session -> {
			TContaining deeplyNestedContainingEntity1 = session.get( _containing().entityClass(), 3 );

			TContained contained = _contained().newInstance( 6 );
			field.set( contained, "outOfScopeValue" );

			containingAssociation.add( deeplyNestedContainingEntity1, contained );
			containedAssociation.set( contained, deeplyNestedContainingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = session.get( _contained().entityClass(), 4 );

			containedAssociation.clear( contained );
			containingAssociation.remove( containingEntity1, contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "embeddedAssociations", b3 -> b3
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", VALUE_2 )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public void indirectMultiValuedAssociationReplace_embeddedAssociationsIndexedEmbedded() {
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().embeddedAssociations()
						.andThen( _containingEmbeddable()::newInstance, _containingEmbeddable().containedIndexedEmbedded() );
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().embeddedAssociations()
				.andThen( _containedEmbeddable()::newInstance, _containedEmbeddable().containingAsIndexedEmbedded() );
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_1 );
			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "embeddedAssociations", b3 -> b3
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", VALUE_1 )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_2 );

			TContainedAssociation newAssociation = _containing().newContainedAssociation(
					containingAssociation.getContainer( containingEntity1 )
			);
			containingAssociation.setContainer( containingEntity1, newAssociation );
			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "embeddedAssociations", b3 -> b3
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", VALUE_1 )
											)
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", VALUE_2 )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public void indirectMultiValuedAssociationUpdate_embeddedAssociationsNonIndexedEmbedded() {
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().embeddedAssociations()
						.andThen( _containingEmbeddable()::newInstance, _containingEmbeddable().containedNonIndexedEmbedded() );
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().embeddedAssociations()
				.andThen( _containedEmbeddable()::newInstance, _containedEmbeddable().containingAsNonIndexedEmbedded() );
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> {} )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_1 );

			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 5 );
			field.set( contained, VALUE_2 );

			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = session.get( _contained().entityClass(), 4 );

			containedAssociation.clear( contained );
			containingAssociation.remove( containingEntity1, contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public void indirectMultiValuedAssociationReplace_embeddedAssociationsNonIndexedEmbedded() {
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containingAssociation =
				_containing().embeddedAssociations()
						.andThen( _containingEmbeddable()::newInstance, _containingEmbeddable().containedNonIndexedEmbedded() );
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().embeddedAssociations()
				.andThen( _containedEmbeddable()::newInstance, _containedEmbeddable().containingAsNonIndexedEmbedded() );
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_1 );
			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> {} )
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_2 );

			TContainedAssociation newAssociation = _containing().newContainedAssociation(
					containingAssociation.getContainer( containingEntity1 )
			);
			containingAssociation.setContainer( containingEntity1, newAssociation );
			containingAssociation.add( containingEntity1, contained );
			containedAssociation.set( contained, containingEntity1 );

			session.persist( contained );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> {} )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	protected interface ContainingEntityPrimitives<TContaining, TContainingEmbeddable, TContained, TContainedAssociation>
			extends
			AbstractAutomaticIndexingAssociationBaseIT.ContainingEntityPrimitives<TContaining,
					TContainingEmbeddable,
					TContained> {
		TContainedAssociation newContainedAssociation(TContainedAssociation original);

		@Override
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedIndexedEmbedded();

		@Override
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedNonIndexedEmbedded();

		@Override
		MultiValuedPropertyAccessor<TContaining,
				TContained,
				TContainedAssociation> containedIndexedEmbeddedShallowReindexOnUpdate();

		@Override
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedIndexedEmbeddedNoReindexOnUpdate();

		@Override
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedUsedInCrossEntityDerivedProperty();

		@Override
		MultiValuedPropertyAccessor<TContaining, TContained, TContainedAssociation> containedIndexedEmbeddedWithCast();
	}


	protected interface ContainingEmbeddablePrimitives<TContainingEmbeddable, TContained, TContainedAssociation>
			extends
			AbstractAutomaticIndexingAssociationBaseIT.ContainingEmbeddablePrimitives<TContainingEmbeddable, TContained> {
		@Override
		MultiValuedPropertyAccessor<TContainingEmbeddable, TContained, TContainedAssociation> containedIndexedEmbedded();

		@Override
		MultiValuedPropertyAccessor<TContainingEmbeddable, TContained, TContainedAssociation> containedNonIndexedEmbedded();

	}
}
