/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype;

import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor.MultiValuedPropertyAccessor;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor.PropertyAccessor;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * Abstract base for tests of automatic indexing caused by association updates
 * or by updates of associated (contained) entities.
 * <p>
 * We use a contrived design based on "primitives" classes
 * ({@link IndexedEntityPrimitives}, {@link ContainingEntityPrimitives}, {@link ContainingEntityPrimitives})
 * that define all the factory methods,
 * setters and getters we need,
 * because we want to have separate model classes for every test,
 * in order to avoid introducing exotic situations that would arise
 * when using generics or superclasses in an ORM model.
 * <p>
 * Generally all models follow these guidelines:
 * <ul>
 *     <li>TIndexed is an @Indexed entity type</li>
 *     <li>TContained is a non-@Indexed entity type</li>
 *     <li>
 *         TContaining is a non-abstract supertype of TIndexed which defines, in particular,
 *         a one-to-one, @IndexedEmbedded, child association to TContaining,
 *         and several @IndexedEmbedded associations (the ones under test) to TContained.
 *     </li>
 * </ul>
 * <p>
 * Most tests defined in this class use a very simple setup when it comes to associations,
 * with one parent TIndexed linked to one child TContaining linked to one TContained.
 * Subclasses define other tests with more advanced setups, with more assumptions as to what the associations allow.
 * <p>
 * Tests in this class and subclasses follow these naming conventions:
 * <ul>
 *     <li>the test method is a sequence of camel-case tokens separated by underscores</li>
 *     <li>
 *         the first token defines what operation we are testing:
 *         <ul>
 *             <li>
 *                 directAssociationUpdate: set the value of the association
 *                 directly on the indexed entity
 *             </li>
 *             <li>
 *                 directImplicitAssociationUpdateThroughInsert: set the value of the association
 *                 directly on the indexed entity,
 *                 but implicitly through an insert of the contained entity,
 *                 without updating the containing side of the association explicitly.
 *             </li>
 *             <li>
 *                 directImplicitAssociationUpdateThroughDelete: set the value of the association
 *                 directly on the indexed entity,
 *                 but implicitly through a delete of the contained entity,
 *                 without updating the containing side of the association explicitly.
 *             </li>
 *             <li>
 *                 directMultiValuedAssociationUpdate: set the value of the association
 *                 directly on the indexed entity,
 *                 adding multiple values to the association
 *             </li>
 *             <li>
 *                 directMultiValuedAssociationReplace: replace the entire value of the association
 *                 (not clear/add, but really use a different collection)
 *                 directly on the indexed entity
 *             </li>
 *             <li>
 *                 indirectAssociationUpdate: set the value of the association
 *                 on an entity that is itself indexed-embedded in the indexed entity
 *             </li>
 *             <li>
 *                 indirectImplicitAssociationUpdateThroughInsert: set the value of the association
 *                 on an entity that is itself indexed-embedded in the indexed entity,
 *                 but implicitly through an insert of the contained entity,
 *                 without updating the containing side of the association explicitly.
 *             </li>
 *             <li>
 *                 indirectImplicitAssociationUpdateThroughDelete: set the value of the association
 *                 on an entity that is itself indexed-embedded in the indexed entity,
 *                 but implicitly through a delete of the contained entity,
 *                 without updating the containing side of the association explicitly.
 *             </li>
 *             <li>
 *                 indirectMultiValuedAssociationUpdate: set the value of the association
 *                 on an entity that is itself indexed-embedded in the indexed entity,
 *                 adding multiple values to the association
 *             </li>
 *             <li>
 *                 directMultiValuedAssociationReplace: replace the entire value of the association
 *                 (not clear/add, but really use a different collection)
 *                 on an entity that is itself indexed-embedded in the indexed entity
 *             </li>
 *             <li>
 *                 indirectValueUpdate: set the value of a basic field,
 *                 add/remove elements to/from an element-collection field
 *             </li>
 *             <li>
 *                 indirectValueReplace: replace the entire value of an element-collection field
 *                 (not clear/add, but really use a different collection)
 *             </li>
 *         </ul>
 *      </li>
 *     <li>
 *         the second token defines how the association between TContaining and TContained is indexed:
 *         indexed-embedded, non-indexed-embedded, indexed-embbedded with ReindexOnUpdate.NO,
 *         indexed as a side effect of being used in a cross-entity derived property.
 *     </li>
 *     <li>
 *         the third token (if any) defines which type of value is being updated/replaced:
 *         singleValue, elementCollectionValue, containedDerivedValue, or cross-entity derived value
 *     </li>
 *     <li>
 *         the fourth token (if any) defines whether the updated/replaced value is indexed
 *         (i.e. included in the @IndexedEmbedded.includePath) or not.
 *     </li>
 * </ul>
 */
public abstract class AbstractAutomaticIndexingAssociationBaseIT<
				TIndexed extends TContaining, TContaining, TContained
		> {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	private final IndexedEntityPrimitives<TIndexed> indexedPrimitives;

	private final ContainingEntityPrimitives<TContaining, TContained> containingPrimitives;

	private final ContainedEntityPrimitives<TContained, TContaining> containedPrimitives;

	public AbstractAutomaticIndexingAssociationBaseIT(IndexedEntityPrimitives<TIndexed> indexedPrimitives,
			ContainingEntityPrimitives<TContaining, TContained> containingPrimitives,
			ContainedEntityPrimitives<TContained, TContaining> containedPrimitives) {
		this.indexedPrimitives = indexedPrimitives;
		this.containingPrimitives = containingPrimitives;
		this.containedPrimitives = containedPrimitives;
	}

	protected abstract boolean isMultiValuedAssociation();

	protected abstract boolean isAssociationOwnedByContainedSide();

	protected abstract boolean isAssociationLazyOnContainingSide();

	protected IndexedEntityPrimitives<TIndexed> _indexed() {
		return indexedPrimitives;
	}

	protected ContainingEntityPrimitives<TContaining, TContained> _containing() {
		return containingPrimitives;
	}

	protected ContainedEntityPrimitives<TContained, TContaining> _contained() {
		return containedPrimitives;
	}

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext, ReusableOrmSetupHolder.DataClearConfig dataClearConfig) {
		Consumer<StubIndexSchemaDataNode.Builder> associationFieldContributor = b -> {
			if ( isMultiValuedAssociation() ) {
				b.multiValued( true );
			}
		};
		backendMock.expectSchema( _indexed().indexName(), b -> b
				.objectField( "containedIndexedEmbedded",
						associationFieldContributor.andThen( b2 -> b2
								.field( "indexedField", String.class )
								.field( "indexedElementCollectionField", String.class, b3 -> b3.multiValued( true ) )
								.field( "containedDerivedField", String.class )
						)
				)
				.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate",
						associationFieldContributor.andThen( b2 -> b2
								.field( "indexedField", String.class )
								.field( "indexedElementCollectionField", String.class, b3 -> b3.multiValued( true ) )
								.field( "containedDerivedField", String.class )
						)
				)
				.objectField( "containedIndexedEmbeddedNoReindexOnUpdate",
						associationFieldContributor.andThen( b2 -> b2
								.field( "indexedField", String.class )
								.field( "indexedElementCollectionField", String.class, b3 -> b3.multiValued( true ) )
								.field( "containedDerivedField", String.class )
						)
				)
				.objectField( "containedIndexedEmbeddedWithCast",
						associationFieldContributor.andThen( b2 -> b2
								.field( "indexedField", String.class )
						)
				)
				.field( "crossEntityDerivedField", String.class )
				.objectField( "child", b3 -> b3
						.objectField( "containedIndexedEmbedded",
								associationFieldContributor.andThen( b2 -> b2
										.field( "indexedField", String.class )
										.field( "indexedElementCollectionField", String.class, b4 -> b4.multiValued( true ) )
										.field( "containedDerivedField", String.class )
								)
						)
						.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate",
								associationFieldContributor.andThen( b2 -> b2
										.field( "indexedField", String.class )
										.field( "indexedElementCollectionField", String.class, b4 -> b4.multiValued( true ) )
										.field( "containedDerivedField", String.class )
								)
						)
						.objectField( "containedIndexedEmbeddedNoReindexOnUpdate",
								associationFieldContributor.andThen( b2 -> b2
										.field( "indexedField", String.class )
										.field( "indexedElementCollectionField", String.class, b4 -> b4.multiValued( true ) )
										.field( "containedDerivedField", String.class )
								)
						)
						.objectField( "containedIndexedEmbeddedWithCast",
								associationFieldContributor.andThen( b2 -> b2
										.field( "indexedField", String.class )
								)
						)
						.field( "crossEntityDerivedField", String.class )
				)
		);

		setupContext.withAnnotatedTypes( _indexed().entityClass(), _containing().entityClass(),
				_contained().entityClass() );

		if ( isAssociationOwnedByContainedSide() ) {
			dataClearConfig.clearOrder( _contained().entityClass(), _containing().entityClass(),
					_indexed().entityClass() );
		}
		else {
			dataClearConfig.clearOrder( _containing().entityClass(), _indexed().entityClass(),
					_contained().entityClass() );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4305")
	public void directAssociationUpdate_indexedEmbedded() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained containedEntity = _contained().newInstance( 2 );
			_contained().indexedField().set( containedEntity, "initialValue" );

			_containing().containedIndexedEmbedded().set( entity1, containedEntity );
			_contained().containingAsIndexedEmbedded().set( containedEntity, entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", "initialValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained containedEntity = _contained().newInstance( 3 );
			_contained().indexedField().set( containedEntity, "updatedValue" );

			TContained oldContained = _containing().containedIndexedEmbedded().get( entity1 );
			_contained().containingAsIndexedEmbedded().clear( oldContained );
			_containing().containedIndexedEmbedded().set( entity1, containedEntity );
			_contained().containingAsIndexedEmbedded().set( containedEntity, entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", "updatedValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained oldContained = _containing().containedIndexedEmbedded().get( entity1 );
			_contained().containingAsIndexedEmbedded().clear( oldContained );
			_containing().containedIndexedEmbedded().clear( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
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
	public final void directAssociationUpdate_nonIndexedEmbedded() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained containedEntity = _contained().newInstance( 2 );
			_contained().nonIndexedField().set( containedEntity, "initialValue" );

			_containing().containedNonIndexedEmbedded().set( entity1, containedEntity );
			_contained().containingAsNonIndexedEmbedded().set( containedEntity, entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained containedEntity = _contained().newInstance( 3 );
			_contained().nonIndexedField().set( containedEntity, "updatedValue" );

			TContained oldContained = _containing().containedNonIndexedEmbedded().get( entity1 );
			_contained().containingAsNonIndexedEmbedded().clear( oldContained );
			_containing().containedNonIndexedEmbedded().set( entity1, containedEntity );
			_contained().containingAsNonIndexedEmbedded().set( containedEntity, entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained oldContained = _containing().containedNonIndexedEmbedded().get( entity1 );
			_contained().containingAsNonIndexedEmbedded().clear( oldContained );
			_containing().containedNonIndexedEmbedded().clear( entity1 );

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
	@TestForIssue(jiraKey = { "HSEARCH-4001", "HSEARCH-4305" })
	public void directAssociationUpdate_indexedEmbeddedShallowReindexOnUpdate() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained containedEntity = _contained().newInstance( 2 );
			_contained().indexedField().set( containedEntity, "initialValue" );

			_containing().containedIndexedEmbeddedShallowReindexOnUpdate().set( entity1, containedEntity );
			_contained().containingAsIndexedEmbeddedShallowReindexOnUpdate().set( containedEntity, entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b2 -> b2
									.field( "indexedField", "initialValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained containedEntity = _contained().newInstance( 3 );
			_contained().indexedField().set( containedEntity, "updatedValue" );

			TContained oldContained = _containing().containedIndexedEmbeddedShallowReindexOnUpdate().get( entity1 );
			_contained().containingAsIndexedEmbeddedShallowReindexOnUpdate().clear( oldContained );
			_containing().containedIndexedEmbeddedShallowReindexOnUpdate().set( entity1, containedEntity );
			_contained().containingAsIndexedEmbeddedShallowReindexOnUpdate().set( containedEntity, entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b2 -> b2
									.field( "indexedField", "updatedValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained oldContained = _containing().containedIndexedEmbeddedShallowReindexOnUpdate().get( entity1 );
			_contained().containingAsIndexedEmbeddedShallowReindexOnUpdate().clear( oldContained );
			_containing().containedIndexedEmbeddedShallowReindexOnUpdate().clear( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
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
	public final void directAssociationUpdate_indexedEmbeddedNoReindexOnUpdate() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained containedEntity = _contained().newInstance( 2 );
			_contained().indexedField().set( containedEntity, "initialValue" );

			_containing().containedIndexedEmbeddedNoReindexOnUpdate().set( entity1, containedEntity );
			_contained().containingAsIndexedEmbeddedNoReindexOnUpdate().set( containedEntity, entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained containedEntity = _contained().newInstance( 3 );
			_contained().indexedField().set( containedEntity, "updatedValue" );

			TContained oldContained = _containing().containedIndexedEmbeddedNoReindexOnUpdate().get( entity1 );
			_contained().containingAsIndexedEmbeddedNoReindexOnUpdate().clear( oldContained );
			_containing().containedIndexedEmbeddedNoReindexOnUpdate().set( entity1, containedEntity );
			_contained().containingAsIndexedEmbeddedNoReindexOnUpdate().set( containedEntity, entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained oldContained = _containing().containedIndexedEmbeddedNoReindexOnUpdate().get( entity1 );
			_contained().containingAsIndexedEmbeddedNoReindexOnUpdate().clear( oldContained );
			_containing().containedIndexedEmbeddedNoReindexOnUpdate().clear( entity1 );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that inserting a contained entity and having its side of the association point to the containing entity
	 * is enough to trigger reindexing of the indexed entity,
	 * even if we don't update the containing side of the association.
	 * <p>
	 * Until HSEARCH-3567 is fixed (and maybe even after),
	 * this can only work if
	 * the owning side of the association is the contained side,
	 * and the containing side of the association is lazy and is not yet loaded
	 * before the contained entity is inserted.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4303")
	public final void directImplicitAssociationUpdateThroughInsert_indexedEmbedded() {
		assumeTrue( "This test only makes sense if the association is owned by the contained side",
				isAssociationOwnedByContainedSide() );
		assumeTrue( "This test can only succeed if the containing side of the association is loaded after the contained entity is inserted."
						+ " See the paragraph starting with \"By the way\" in"
						+ " https://discourse.hibernate.org/t/hs6-not-indexing-add-or-delete-only-update-with-onetomany-indexedembedded/5638/6",
				isAssociationLazyOnContainingSide() || !setupHolder.areEntitiesProcessedInSession() );

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained containedEntity = _contained().newInstance( 2 );
			_contained().indexedField().set( containedEntity, "initialValue" );

			// Do NOT set the association on the containing side; that's on purpose.
			// Only set it on the contained side.
			_contained().containingAsIndexedEmbedded().set( containedEntity, entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", "initialValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that deleting a contained entity
	 * is enough to trigger reindexing of the indexed entity,
	 * even if we don't update the containing side of the association.
	 * <p>
	 * Until HSEARCH-3567 is fixed (and maybe even after),
	 * this can only work if
	 * the contained side of the association is already loaded before the contained entity is deleted,
	 * and the contained side of the association is not explicitly updated
	 * before the contained entity is deleted.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4303")
	public final void directImplicitAssociationUpdateThroughDelete_indexedEmbedded() {
		assumeTrue( "This test only makes sense if the association is owned by the contained side",
				isAssociationOwnedByContainedSide() );
		assumeTrue( "This test can only succeed if the containing side of the association is loaded after the contained entity is inserted."
						+ " See the paragraph starting with \"By the way\" in"
						+ " https://discourse.hibernate.org/t/hs6-not-indexing-add-or-delete-only-update-with-onetomany-indexedembedded/5638/6",
				isAssociationLazyOnContainingSide() || !setupHolder.areEntitiesProcessedInSession() );

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );
			TContained containedEntity = _contained().newInstance( 2 );
			_contained().indexedField().set( containedEntity, "initialValue" );

			session.persist( entity1 );
			session.persist( containedEntity );

			_containing().containedIndexedEmbedded().set( entity1, containedEntity );
			_contained().containingAsIndexedEmbedded().set( containedEntity, entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", "initialValue" )
							) );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContained containedEntity = session.get( _contained().entityClass(), 2 );

			// Do NOT update the association on either side; that's on purpose.
			// But DO force loading on contained side:
			// if the association is lazy and unloaded, it won't be loadable after the deletion
			// and the deletion even will simply be ignored.
			_contained().containingAsIndexedEmbedded().get( containedEntity );

			session.remove( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that deleting a contained entity
	 * is enough to trigger reindexing of the indexed entity,
	 * even if we don't update the containing side of the association.
	 * <p>
	 * Until HSEARCH-3567 is fixed (and maybe even after),
	 * this can only work if
	 * the contained side of the association is already loaded before the contained entity is deleted,
	 * and the contained side of the association is not explicitly updated
	 * before the contained entity is deleted.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4365")
	public final void directImplicitAssociationUpdateThroughDeleteWithAlreadyLoadedAssociation_indexedEmbedded() {
		assumeTrue( "This test only makes sense if the association is owned by the contained side;" +
						" if the association is owned by the containing side," +
						" deleting a contained entity requires updating the association to avoid violating foreign key constraints.",
				isAssociationOwnedByContainedSide() );

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );
			TContained containedEntity = _contained().newInstance( 2 );
			_contained().indexedField().set( containedEntity, "initialValue" );

			session.persist( entity1 );
			session.persist( containedEntity );

			_containing().containedIndexedEmbedded().set( entity1, containedEntity );
			_contained().containingAsIndexedEmbedded().set( containedEntity, entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", "initialValue" )
							) );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );
			// Make sure we initialize the association from indexed to contained;
			// the magic is in the fact that Hibernate doesn't index the contained entity
			// even though it's referenced by the Java representation of the association.
			TContained containedEntity = _containing().containedIndexedEmbedded().get( entity1 );

			// Do NOT update the association on either side; that's on purpose.
			// But DO force loading on contained side:
			// if the association is lazy and unloaded, it won't be loadable after the deletion
			// and the deletion even will simply be ignored.
			_contained().containingAsIndexedEmbedded().get( containedEntity );

			session.remove( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4305")
	public void indirectAssociationUpdate_indexedEmbedded() {
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
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 4 );
			_contained().indexedField().set( containedEntity, "initialValue" );

			_containing().containedIndexedEmbedded().set( containingEntity1, containedEntity );
			_contained().containingAsIndexedEmbedded().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
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
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 5 );
			_contained().indexedField().set( containedEntity, "updatedValue" );

			TContained oldContained = _containing().containedIndexedEmbedded().get( containingEntity1 );
			_contained().containingAsIndexedEmbedded().clear( oldContained );
			_containing().containedIndexedEmbedded().set( containingEntity1, containedEntity );
			_contained().containingAsIndexedEmbedded().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
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
		setupHolder.runInTransaction( session -> {
			TContaining deeplyNestedContainingEntity1 = session.get( _containing().entityClass(), 3 );

			TContained containedEntity = _contained().newInstance( 6 );
			_contained().indexedField().set( containedEntity, "outOfScopeValue" );

			_containing().containedIndexedEmbedded().set( deeplyNestedContainingEntity1, containedEntity );
			_contained().containingAsIndexedEmbedded().set( containedEntity, deeplyNestedContainingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained oldContained = _containing().containedIndexedEmbedded().get( containingEntity1 );
			_contained().containingAsIndexedEmbedded().clear( oldContained );
			_containing().containedIndexedEmbedded().clear( containingEntity1 );

			backendMock.expectWorks( _indexed().indexName() )
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
	public final void indirectAssociationUpdate_nonIndexedEmbedded() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 4 );
			_contained().indexedField().set( containedEntity, "initialValue" );

			_containing().containedNonIndexedEmbedded().set( containingEntity1, containedEntity );
			_contained().containingAsNonIndexedEmbedded().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 5 );
			_contained().indexedField().set( containedEntity, "updatedValue" );

			TContained oldContained = _containing().containedNonIndexedEmbedded().get( containingEntity1 );
			_contained().containingAsNonIndexedEmbedded().clear( oldContained );
			_containing().containedNonIndexedEmbedded().set( containingEntity1, containedEntity );
			_contained().containingAsNonIndexedEmbedded().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained oldContained = _containing().containedNonIndexedEmbedded().get( containingEntity1 );
			_contained().containingAsNonIndexedEmbedded().clear( oldContained );
			_containing().containedNonIndexedEmbedded().clear( containingEntity1 );

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
	@TestForIssue(jiraKey = { "HSEARCH-4001", "HSEARCH-4305" })
	public void indirectAssociationUpdate_indexedEmbeddedShallowReindexOnUpdate() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 4 );
			_contained().indexedField().set( containedEntity, "initialValue" );

			_containing().containedIndexedEmbeddedShallowReindexOnUpdate().set( containingEntity1, containedEntity );
			_contained().containingAsIndexedEmbeddedShallowReindexOnUpdate().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
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
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 5 );
			_contained().indexedField().set( containedEntity, "updatedValue" );

			TContained oldContained = _containing().containedIndexedEmbeddedShallowReindexOnUpdate().get( containingEntity1 );
			_contained().containingAsIndexedEmbeddedShallowReindexOnUpdate().clear( oldContained );
			_containing().containedIndexedEmbeddedShallowReindexOnUpdate().set( containingEntity1, containedEntity );
			_contained().containingAsIndexedEmbeddedShallowReindexOnUpdate().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
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
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );
			TContained containedEntity = session.get( _contained().entityClass(), 5 );

			_containing().containedIndexedEmbeddedShallowReindexOnUpdate().clear( containingEntity1 );
			_contained().containingAsIndexedEmbeddedShallowReindexOnUpdate().clear( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
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
	public final void indirectAssociationUpdate_indexedEmbeddedNoReindexOnUpdate() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 4 );
			_contained().indexedField().set( containedEntity, "initialValue" );

			_containing().containedIndexedEmbeddedNoReindexOnUpdate().set( containingEntity1, containedEntity );
			_contained().containingAsIndexedEmbeddedNoReindexOnUpdate().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 5 );
			_contained().indexedField().set( containedEntity, "updatedValue" );

			TContained oldContained = _containing().containedIndexedEmbeddedNoReindexOnUpdate().get( containingEntity1 );
			_contained().containingAsIndexedEmbeddedNoReindexOnUpdate().clear( oldContained );
			_containing().containedIndexedEmbeddedNoReindexOnUpdate().set( containingEntity1, containedEntity );
			_contained().containingAsIndexedEmbeddedNoReindexOnUpdate().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );
			TContained containedEntity = session.get( _contained().entityClass(), 5 );

			_containing().containedIndexedEmbeddedNoReindexOnUpdate().clear( containingEntity1 );
			_contained().containingAsIndexedEmbeddedNoReindexOnUpdate().clear( containedEntity );

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
	@TestForIssue(jiraKey = "HSEARCH-4305")
	public void indirectAssociationUpdate_usedInCrossEntityDerivedProperty() {
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
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 4 );
			_contained().fieldUsedInCrossEntityDerivedField1().set( containedEntity, "field1_initialValue" );
			_contained().fieldUsedInCrossEntityDerivedField2().set( containedEntity, "field2_initialValue" );

			_containing().containedUsedInCrossEntityDerivedProperty().set( containingEntity1, containedEntity );
			_contained().containingAsUsedInCrossEntityDerivedProperty().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
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
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 5 );
			_contained().fieldUsedInCrossEntityDerivedField1().set( containedEntity, "field1_updatedValue" );
			_contained().fieldUsedInCrossEntityDerivedField2().set( containedEntity, "field2_updatedValue" );

			TContained oldContained = _containing().containedUsedInCrossEntityDerivedProperty().get( containingEntity1 );
			_contained().containingAsUsedInCrossEntityDerivedProperty().clear( oldContained );
			_containing().containedUsedInCrossEntityDerivedProperty().set( containingEntity1, containedEntity );
			_contained().containingAsUsedInCrossEntityDerivedProperty().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
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
		setupHolder.runInTransaction( session -> {
			TContaining deeplyNestedContainingEntity1 = session.get( _containing().entityClass(), 3 );

			TContained containedEntity = _contained().newInstance( 6 );
			_contained().fieldUsedInCrossEntityDerivedField1().set( containedEntity, "field1_outOfScopeValue" );
			_contained().fieldUsedInCrossEntityDerivedField2().set( containedEntity, "field2_outOfScopeValue" );

			_containing().containedUsedInCrossEntityDerivedProperty().set( deeplyNestedContainingEntity1, containedEntity );
			_contained().containingAsUsedInCrossEntityDerivedProperty().set( containedEntity, deeplyNestedContainingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained oldContained = _containing().containedUsedInCrossEntityDerivedProperty().get( containingEntity1 );
			_contained().containingAsUsedInCrossEntityDerivedProperty().clear( oldContained );
			_containing().containedUsedInCrossEntityDerivedProperty().clear( containingEntity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Same as {@link #directImplicitAssociationUpdateThroughInsert_indexedEmbedded()},
	 * but with an additional association: indexedEntity -> containingEntity -> containedEntity.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4303")
	public final void indirectImplicitAssociationUpdateThroughInsert_indexedEmbedded() {
		assumeTrue( "This test only makes sense if the association is owned by the contained side",
				isAssociationOwnedByContainedSide() );
		assumeTrue( "This test can only succeed if the containing side of the association is loaded after the contained entity is inserted."
						+ " See the paragraph starting with \"By the way\" in"
						+ " https://discourse.hibernate.org/t/hs6-not-indexing-add-or-delete-only-update-with-onetomany-indexedembedded/5638/6",
				isAssociationLazyOnContainingSide() || !setupHolder.areEntitiesProcessedInSession() );

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } ) );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 2 );
			_contained().indexedField().set( containedEntity, "initialValue" );

			// Do NOT set the association on the containing side; that's on purpose.
			// Only set it on the contained side.
			_contained().containingAsIndexedEmbedded().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "initialValue" )
									) ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Same as {@link #directImplicitAssociationUpdateThroughDelete_indexedEmbedded()},
	 * but with an additional association: indexedEntity -> containingEntity -> containedEntity.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4303")
	public final void indirectImplicitAssociationUpdateThroughDelete_indexedEmbedded() {
		assumeTrue( "This test only makes sense if the association is owned by the contained side",
				isAssociationOwnedByContainedSide() );
		assumeTrue( "This test can only succeed if the containing side of the association is loaded after the contained entity is inserted."
						+ " See the paragraph starting with \"By the way\" in"
						+ " https://discourse.hibernate.org/t/hs6-not-indexing-add-or-delete-only-update-with-onetomany-indexedembedded/5638/6",
				isAssociationLazyOnContainingSide() || !setupHolder.areEntitiesProcessedInSession() );

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );
			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );
			TContained containedEntity = _contained().newInstance( 2 );
			_contained().indexedField().set( containedEntity, "initialValue" );

			session.persist( containingEntity1 );
			session.persist( entity1 );
			session.persist( containedEntity );

			_containing().containedIndexedEmbedded().set( containingEntity1, containedEntity );
			_contained().containingAsIndexedEmbedded().set( containedEntity, containingEntity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "initialValue" )
									) ) );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContained containedEntity = session.get( _contained().entityClass(), 2 );

			// Do NOT update the association on either side; that's on purpose.
			// But DO force loading on contained side:
			// if the association is lazy and unloaded, it won't be loadable after the deletion
			// and the deletion even will simply be ignored.
			_contained().containingAsIndexedEmbedded().get( containedEntity );

			session.remove( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> { } ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Same as {@link #directImplicitAssociationUpdateThroughDeleteWithAlreadyLoadedAssociation_indexedEmbedded()},
	 * but with an additional association: indexedEntity -> containingEntity -> containedEntity.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4365")
	public final void indirectImplicitAssociationUpdateThroughDeleteWithAlreadyLoadedAssociation_indexedEmbedded() {
		assumeTrue( "This test only makes sense if the association is owned by the contained side;" +
						" if the association is owned by the containing side," +
						" deleting a contained entity requires updating the association to avoid violating foreign key constraints.",
				isAssociationOwnedByContainedSide() );

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );
			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );
			TContained containedEntity = _contained().newInstance( 2 );
			_contained().indexedField().set( containedEntity, "initialValue" );

			session.persist( containingEntity1 );
			session.persist( entity1 );
			session.persist( containedEntity );

			_containing().containedIndexedEmbedded().set( containingEntity1, containedEntity );
			_contained().containingAsIndexedEmbedded().set( containedEntity, containingEntity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "initialValue" )
									) ) );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );
			// Make sure we initialize the association from containing to contained;
			// the magic is in the fact that Hibernate doesn't index the contained entity
			// even though it's referenced by the Java representation of the association.
			TContained containedEntity = _containing().containedIndexedEmbedded().get( containing );

			// Do NOT update the association on either side; that's on purpose.
			// But DO force loading on contained side:
			// if the association is lazy and unloaded, it won't be loadable after the deletion
			// and the deletion even will simply be ignored.
			_contained().containingAsIndexedEmbedded().get( containedEntity );

			session.remove( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> { } ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_indexedEmbedded_singleValue_indexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = _containing().newInstance( 3 );
			_containing().child().set( containingEntity1, deeplyNestedContainingEntity );
			_containing().parent().set( deeplyNestedContainingEntity, containingEntity1 );

			TContained contained1 = _contained().newInstance( 4 );
			_contained().indexedField().set( contained1, "initialValue" );
			_containing().containedIndexedEmbedded().set( containingEntity1, contained1 );
			_contained().containingAsIndexedEmbedded().set( contained1, containingEntity1 );

			TContained contained2 = _contained().newInstance( 5 );
			_contained().indexedField().set( contained2, "initialOutOfScopeValue" );
			_containing().containedIndexedEmbedded().set( deeplyNestedContainingEntity, contained2 );
			_contained().containingAsIndexedEmbedded().set( contained2, deeplyNestedContainingEntity );

			session.persist( contained1 );
			session.persist( contained2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "initialValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().indexedField().set( contained, "updatedValue" );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "updatedValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 5 );
			_contained().indexedField().set( contained, "updatedOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4137")
	public void directValueUpdate_nonIndexed_then_indirectValueUpdate_indexedEmbedded_singleValue_indexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );
			_containing().nonIndexedField().set( entity1, "initialValue" );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			_contained().indexedField().set( contained1, "initialValue" );
			_containing().containedIndexedEmbedded().set( containingEntity1, contained1 );
			_contained().containingAsIndexedEmbedded().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "initialValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value in the indexed entity, then in the same transaction updating a value in a contained entity
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );
			_containing().nonIndexedField().set( indexed, "updatedValue" );
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().indexedField().set( contained, "updatedValue" );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "updatedValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating a non-indexed, basic property in an entity
	 * whose properties are otherwise used in an IndexedEmbedded from an indexed entity
	 * does not trigger reindexing of the indexed entity.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void indirectValueUpdate_indexedEmbedded_singleValue_nonIndexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			_contained().nonIndexedField().set( contained1, "initialValue" );
			_containing().containedIndexedEmbedded().set( containingEntity1, contained1 );
			_contained().containingAsIndexedEmbedded().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().nonIndexedField().set( contained, "updatedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_indexedEmbedded_elementCollectionValue_indexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = _containing().newInstance( 3 );
			_containing().child().set( containingEntity1, deeplyNestedContainingEntity );
			_containing().parent().set( deeplyNestedContainingEntity, containingEntity1 );

			TContained contained1 = _contained().newInstance( 4 );
			_contained().indexedElementCollectionField().add( contained1, "firstValue" );
			_containing().containedIndexedEmbedded().set( containingEntity1, contained1 );
			_contained().containingAsIndexedEmbedded().set( contained1, containingEntity1 );

			TContained contained2 = _contained().newInstance( 5 );
			_contained().indexedElementCollectionField().add( contained2, "firstOutOfScopeValue" );
			_containing().containedIndexedEmbedded().set( deeplyNestedContainingEntity, contained2 );
			_contained().containingAsIndexedEmbedded().set( contained2, deeplyNestedContainingEntity );

			session.persist( contained1 );
			session.persist( contained2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
											.field(
													"indexedElementCollectionField",
													"firstValue"
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().indexedElementCollectionField().add( contained, "secondValue" );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
											.field(
													"indexedElementCollectionField",
													"firstValue", "secondValue"
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().indexedElementCollectionField().remove( contained, "firstValue" );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
											.field(
													"indexedElementCollectionField",
													"secondValue"
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 5 );
			_contained().indexedElementCollectionField().add( contained, "secondOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing an indexed ElementCollection property in an entity
	 * that is IndexedEmbedded in an indexed entity
	 * does trigger reindexing of the indexed entity.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void indirectValueReplace_indexedEmbedded_elementCollectionValue_indexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = _containing().newInstance( 3 );
			_containing().child().set( containingEntity1, deeplyNestedContainingEntity );
			_containing().parent().set( deeplyNestedContainingEntity, containingEntity1 );

			TContained contained1 = _contained().newInstance( 4 );
			_contained().indexedElementCollectionField().add( contained1, "firstValue" );
			_containing().containedIndexedEmbedded().set( containingEntity1, contained1 );
			_contained().containingAsIndexedEmbedded().set( contained1, containingEntity1 );

			TContained contained2 = _contained().newInstance( 5 );
			_contained().indexedElementCollectionField().add( contained2, "firstOutOfScopeValue" );
			_containing().containedIndexedEmbedded().set( deeplyNestedContainingEntity, contained2 );
			_contained().containingAsIndexedEmbedded().set( contained2, deeplyNestedContainingEntity );

			session.persist( contained1 );
			session.persist( contained2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
											.field(
													"indexedElementCollectionField",
													"firstValue"
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().indexedElementCollectionField().setContainer( contained, new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
											.field(
													"indexedElementCollectionField",
													"newFirstValue", "newSecondValue"
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 5 );
			_contained().indexedElementCollectionField().setContainer( contained, new ArrayList<>( Arrays.asList(
					"newFirstOutOfScopeValue", "newSecondOutOfScopeValue"
			) ) );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating a non-indexed, ElementCollection property in an entity
	 * whose properties are otherwise used in an IndexedEmbedded from an indexed entity
	 * does not trigger reindexing of the indexed entity.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void indirectValueUpdate_indexedEmbedded_elementCollectionValue_nonIndexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			_contained().nonIndexedElementCollectionField().add( contained1, "firstValue" );
			_containing().containedIndexedEmbedded().set( containingEntity1, contained1 );
			_contained().containingAsIndexedEmbedded().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().nonIndexedElementCollectionField().add( contained, "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().nonIndexedElementCollectionField().remove( contained, "firstValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing a non-indexed, ElementCollection property in an entity
	 * whose properties are otherwise used in an IndexedEmbedded from an indexed entity
	 * does not trigger reindexing of the indexed entity.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3204")
	public void indirectValueReplace_indexedEmbedded_elementCollectionValue_nonIndexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			_contained().nonIndexedElementCollectionField().add( contained1, "firstValue" );
			_containing().containedIndexedEmbedded().set( containingEntity1, contained1 );
			_contained().containingAsIndexedEmbedded().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing the values
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().nonIndexedElementCollectionField().setContainer( contained, new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_indexedEmbedded_containedDerivedValue_indexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			_contained().fieldUsedInContainedDerivedField1().set( contained1, "field1_initialValue" );
			_contained().fieldUsedInContainedDerivedField2().set( contained1, "field2_initialValue" );
			_containing().containedIndexedEmbedded().set( containingEntity1, contained1 );
			_contained().containingAsIndexedEmbedded().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )

											.field(
													"containedDerivedField",
													"field1_initialValue field2_initialValue"
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating one value the field depends on
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().fieldUsedInContainedDerivedField1().set( contained, "field1_updatedValue" );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
											.field(
													"containedDerivedField",
													"field1_updatedValue field2_initialValue"
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the other value the field depends on
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().fieldUsedInContainedDerivedField2().set( contained, "field2_updatedValue" );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
											.field(
													"containedDerivedField",
													"field1_updatedValue field2_updatedValue"
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating a property in an entity
	 * when this property is the last component of a path in a @IndexingDependency.derivedFrom attribute
	 * does trigger reindexing of the indexed entity.
	 */
	@Test
	public void indirectValueUpdate_usedInCrossEntityDerivedProperty_crossEntityDerivedValue_indexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			_contained().fieldUsedInCrossEntityDerivedField1().set( contained1, "field1_initialValue" );
			_contained().fieldUsedInCrossEntityDerivedField2().set( contained1, "field2_initialValue" );
			_containing().containedUsedInCrossEntityDerivedProperty().set( containingEntity1, contained1 );
			_contained().containingAsUsedInCrossEntityDerivedProperty().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.field(
											"crossEntityDerivedField",
											"field1_initialValue field2_initialValue"
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating one value the field depends on
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().fieldUsedInCrossEntityDerivedField1().set( contained, "field1_updatedValue" );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.field(
											"crossEntityDerivedField",
											"field1_updatedValue field2_initialValue"
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the other value the field depends on
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().fieldUsedInCrossEntityDerivedField2().set( contained, "field2_updatedValue" );

			backendMock.expectWorks( _indexed().indexName() )
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
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4001")
	public void indirectValueUpdate_indexedEmbeddedShallowReindexOnUpdate_singleValue_indexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			_contained().indexedField().set( contained1, "initialValue" );
			_containing().containedIndexedEmbeddedShallowReindexOnUpdate().set( containingEntity1, contained1 );
			_contained().containingAsIndexedEmbeddedShallowReindexOnUpdate().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b3 -> b3
											.field( "indexedField", "initialValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().indexedField().set( contained, "updatedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating an indexed ElementCollection property in an entity
	 * that is IndexedEmbedded in an indexed entity
	 * does not trigger reindexing of the indexed entity
	 * if the association is marked with ReindexOnUpdate = SHALLOW.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4001")
	public void indirectValueUpdate_indexedEmbeddedShallowReindexOnUpdate_elementCollectionValue_indexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			_contained().indexedElementCollectionField().add( contained1, "firstValue" );
			_containing().containedIndexedEmbeddedShallowReindexOnUpdate().set( containingEntity1, contained1 );
			_contained().containingAsIndexedEmbeddedShallowReindexOnUpdate().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b3 -> b3
											.field( "indexedField", null )
											.field(
													"indexedElementCollectionField",
													"firstValue"
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().indexedElementCollectionField().add( contained, "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().indexedElementCollectionField().remove( contained, "firstValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing an indexed ElementCollection property in an entity
	 * that is IndexedEmbedded in an indexed entity
	 * does not trigger reindexing of the indexed entity
	 * if the association is marked with ReindexOnUpdate = SHALLOW.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4001")
	public void indirectValueReplace_indexedEmbeddedShallowReindexOnUpdate_elementCollectionValue_indexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			_contained().indexedElementCollectionField().add( contained1, "firstValue" );
			_containing().containedIndexedEmbeddedShallowReindexOnUpdate().set( containingEntity1, contained1 );
			_contained().containingAsIndexedEmbeddedShallowReindexOnUpdate().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedShallowReindexOnUpdate", b3 -> b3
											.field( "indexedField", null )
											.field(
													"indexedElementCollectionField",
													"firstValue"
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().indexedElementCollectionField().setContainer( contained, new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3206")
	public void indirectValueUpdate_indexedEmbeddedNoReindexOnUpdate_singleValue_indexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			_contained().indexedField().set( contained1, "initialValue" );
			_containing().containedIndexedEmbeddedNoReindexOnUpdate().set( containingEntity1, contained1 );
			_contained().containingAsIndexedEmbeddedNoReindexOnUpdate().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b3 -> b3
											.field( "indexedField", "initialValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().indexedField().set( contained, "updatedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that updating an indexed ElementCollection property in an entity
	 * that is IndexedEmbedded in an indexed entity
	 * does not trigger reindexing of the indexed entity
	 * if the association is marked with ReindexOnUpdate = NO.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3206")
	public void indirectValueUpdate_indexedEmbeddedNoReindexOnUpdate_elementCollectionValue_indexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			_contained().indexedElementCollectionField().add( contained1, "firstValue" );
			_containing().containedIndexedEmbeddedNoReindexOnUpdate().set( containingEntity1, contained1 );
			_contained().containingAsIndexedEmbeddedNoReindexOnUpdate().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b3 -> b3
											.field( "indexedField", null )
											.field(
													"indexedElementCollectionField",
													"firstValue"
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().indexedElementCollectionField().add( contained, "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().indexedElementCollectionField().remove( contained, "firstValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing an indexed ElementCollection property in an entity
	 * that is IndexedEmbedded in an indexed entity
	 * does not trigger reindexing of the indexed entity
	 * if the association is marked with ReindexOnUpdate = NO.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3206")
	public void indirectValueReplace_indexedEmbeddedNoReindexOnUpdate_elementCollectionValue_indexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			_contained().indexedElementCollectionField().add( contained1, "firstValue" );
			_containing().containedIndexedEmbeddedNoReindexOnUpdate().set( containingEntity1, contained1 );
			_contained().containingAsIndexedEmbeddedNoReindexOnUpdate().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedNoReindexOnUpdate", b3 -> b3
											.field( "indexedField", null )
											.field(
													"indexedElementCollectionField",
													"firstValue"
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().indexedElementCollectionField().setContainer( contained, new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3072")
	public void indirectValueUpdate_indexedEmbeddedWithCast_singleValue() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = _containing().newInstance( 3 );
			_containing().child().set( containingEntity1, deeplyNestedContainingEntity );
			_containing().parent().set( deeplyNestedContainingEntity, containingEntity1 );

			TContained contained1 = _contained().newInstance( 4 );
			_contained().indexedField().set( contained1, "initialValue" );
			_containing().containedIndexedEmbeddedWithCast().set( containingEntity1, contained1 );
			_contained().containingAsIndexedEmbeddedWithCast().set( contained1, containingEntity1 );

			TContained contained2 = _contained().newInstance( 5 );
			_contained().indexedField().set( contained2, "initialOutOfScopeValue" );
			_containing().containedIndexedEmbeddedWithCast().set( deeplyNestedContainingEntity, contained2 );
			_contained().containingAsIndexedEmbeddedWithCast().set( contained2, deeplyNestedContainingEntity );

			session.persist( contained1 );
			session.persist( contained2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedWithCast", b3 -> b3
											.field( "indexedField", "initialValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			_contained().indexedField().set( contained, "updatedValue" );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbeddedWithCast", b3 -> b3
											.field( "indexedField", "updatedValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 5 );
			_contained().indexedField().set( contained, "updatedOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/*
	 * Use the type "Optional<String>" for computed fields instead of just "String" for two reasons:
	 * 1/ It means the field will not appear in the indexed document produced by the stub backend
	 * when both "base" fields are null, which is great because it means tests that do not use
	 * the derived field do not have to expect it to appear with a null value out of nowehere.
	 * 2/ It allows us to check that transient fields may have a container type that cannot
	 * be expressed in ORM metadata.
	 */
	protected static Optional<String> computeDerived(Stream<String> fieldValues) {
		String value = fieldValues.filter( Objects::nonNull )
				.collect( Collectors.joining( " " ) );
		if ( value.isEmpty() ) {
			return Optional.empty();
		}
		else {
			return Optional.of( value );
		}
	}

	protected interface IndexedEntityPrimitives<TIndexed> {

		Class<TIndexed> entityClass();

		String indexName();

		TIndexed newInstance(int id);

	}

	protected interface ContainingEntityPrimitives<TContaining, TContained> {

		Class<TContaining> entityClass();

		TContaining newInstance(int id);

		PropertyAccessor<TContaining, String> nonIndexedField();

		PropertyAccessor<TContaining, TContaining> child();

		PropertyAccessor<TContaining, TContaining> parent();

		PropertyAccessor<TContaining, TContained> containedIndexedEmbedded();

		PropertyAccessor<TContaining, TContained> containedNonIndexedEmbedded();

		PropertyAccessor<TContaining, TContained> containedIndexedEmbeddedShallowReindexOnUpdate();

		PropertyAccessor<TContaining, TContained> containedIndexedEmbeddedNoReindexOnUpdate();

		PropertyAccessor<TContaining, TContained> containedUsedInCrossEntityDerivedProperty();

		PropertyAccessor<TContaining, TContained> containedIndexedEmbeddedWithCast();

	}

	protected interface ContainedEntityPrimitives<TContained, TContaining> {

		Class<TContained> entityClass();

		TContained newInstance(int id);

		PropertyAccessor<TContained, TContaining> containingAsIndexedEmbedded();

		PropertyAccessor<TContained, TContaining> containingAsNonIndexedEmbedded();

		PropertyAccessor<TContained, TContaining> containingAsIndexedEmbeddedShallowReindexOnUpdate();

		PropertyAccessor<TContained, TContaining> containingAsIndexedEmbeddedNoReindexOnUpdate();

		PropertyAccessor<TContained, TContaining> containingAsUsedInCrossEntityDerivedProperty();

		PropertyAccessor<TContained, TContaining> containingAsIndexedEmbeddedWithCast();

		PropertyAccessor<TContained, String> indexedField();

		PropertyAccessor<TContained, String> nonIndexedField();

		MultiValuedPropertyAccessor<TContained, String, List<String>> indexedElementCollectionField();

		MultiValuedPropertyAccessor<TContained, String, List<String>> nonIndexedElementCollectionField();

		PropertyAccessor<TContained, String> fieldUsedInContainedDerivedField1();

		PropertyAccessor<TContained, String> fieldUsedInContainedDerivedField2();

		PropertyAccessor<TContained, String> fieldUsedInCrossEntityDerivedField1();

		PropertyAccessor<TContained, String> fieldUsedInCrossEntityDerivedField2();

	}
}
