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
 * We use a contrived design based on a {@link ModelPrimitives} class that defines all the factory methods,
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

	private final ModelPrimitives<TIndexed, TContaining, TContained> primitives;

	public AbstractAutomaticIndexingAssociationBaseIT(
			ModelPrimitives<TIndexed, TContaining, TContained> primitives) {
		this.primitives = primitives;
	}

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext, ReusableOrmSetupHolder.DataClearConfig dataClearConfig) {
		Consumer<StubIndexSchemaDataNode.Builder> associationFieldContributor = b -> {
			if ( primitives.isMultiValuedAssociation() ) {
				b.multiValued( true );
			}
		};
		backendMock.expectSchema( primitives.getIndexName(), b -> b
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

		setupContext.withAnnotatedTypes( primitives.getIndexedClass(), primitives.getContainingClass(),
				primitives.getContainedClass() );

		if ( primitives.isAssociationOwnedByContainedSide() ) {
			dataClearConfig.clearOrder( primitives.getContainedClass(), primitives.getContainingClass(),
					primitives.getIndexedClass() );
		}
		else {
			dataClearConfig.clearOrder( primitives.getContainingClass(), primitives.getIndexedClass(),
					primitives.getContainedClass() );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4305")
	public void directAssociationUpdate_indexedEmbedded() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
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
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained containedEntity = primitives.newContained( 3 );
			primitives.indexedField().set( containedEntity, "updatedValue" );

			TContained oldContained = primitives.containedIndexedEmbedded().get( entity1 );
			primitives.containingAsIndexedEmbedded().clear( oldContained );
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
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained oldContained = primitives.containedIndexedEmbedded().get( entity1 );
			primitives.containingAsIndexedEmbedded().clear( oldContained );
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
	public final void directAssociationUpdate_nonIndexedEmbedded() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
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
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained containedEntity = primitives.newContained( 3 );
			primitives.nonIndexedField().set( containedEntity, "updatedValue" );

			TContained oldContained = primitives.containedNonIndexedEmbedded().get( entity1 );
			primitives.containingAsNonIndexedEmbedded().clear( oldContained );
			primitives.containedNonIndexedEmbedded().set( entity1, containedEntity );
			primitives.containingAsNonIndexedEmbedded().set( containedEntity, entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained oldContained = primitives.containedNonIndexedEmbedded().get( entity1 );
			primitives.containingAsNonIndexedEmbedded().clear( oldContained );
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
	@TestForIssue(jiraKey = { "HSEARCH-4001", "HSEARCH-4305" })
	public void directAssociationUpdate_indexedEmbeddedShallowReindexOnUpdate() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained containedEntity = primitives.newContained( 2 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().set( entity1, containedEntity );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().set( containedEntity, entity1 );

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
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained containedEntity = primitives.newContained( 3 );
			primitives.indexedField().set( containedEntity, "updatedValue" );

			TContained oldContained = primitives.containedIndexedEmbeddedShallowReindexOnUpdate().get( entity1 );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().clear( oldContained );
			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().set( entity1, containedEntity );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().set( containedEntity, entity1 );

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
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained oldContained = primitives.containedIndexedEmbeddedShallowReindexOnUpdate().get( entity1 );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().clear( oldContained );
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
	public final void directAssociationUpdate_indexedEmbeddedNoReindexOnUpdate() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained containedEntity = primitives.newContained( 2 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			primitives.containedIndexedEmbeddedNoReindexOnUpdate().set( entity1, containedEntity );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().set( containedEntity, entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained containedEntity = primitives.newContained( 3 );
			primitives.indexedField().set( containedEntity, "updatedValue" );

			TContained oldContained = primitives.containedIndexedEmbeddedNoReindexOnUpdate().get( entity1 );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().clear( oldContained );
			primitives.containedIndexedEmbeddedNoReindexOnUpdate().set( entity1, containedEntity );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().set( containedEntity, entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained oldContained = primitives.containedIndexedEmbeddedNoReindexOnUpdate().get( entity1 );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().clear( oldContained );
			primitives.containedIndexedEmbeddedNoReindexOnUpdate().clear( entity1 );

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
				primitives.isAssociationOwnedByContainedSide() );
		assumeTrue( "This test can only succeed if the containing side of the association is loaded after the contained entity is inserted."
						+ " See the paragraph starting with \"By the way\" in"
						+ " https://discourse.hibernate.org/t/hs6-not-indexing-add-or-delete-only-update-with-onetomany-indexedembedded/5638/6",
				primitives.isAssociationLazyOnContainingSide() || !setupHolder.areEntitiesProcessedInSession() );

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TContained containedEntity = primitives.newContained( 2 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			// Do NOT set the association on the containing side; that's on purpose.
			// Only set it on the contained side.
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
				primitives.isAssociationOwnedByContainedSide() );
		assumeTrue( "This test can only succeed if the containing side of the association is loaded after the contained entity is inserted."
						+ " See the paragraph starting with \"By the way\" in"
						+ " https://discourse.hibernate.org/t/hs6-not-indexing-add-or-delete-only-update-with-onetomany-indexedembedded/5638/6",
				primitives.isAssociationLazyOnContainingSide() || !setupHolder.areEntitiesProcessedInSession() );

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );
			TContained containedEntity = primitives.newContained( 2 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			session.persist( entity1 );
			session.persist( containedEntity );

			primitives.containedIndexedEmbedded().set( entity1, containedEntity );
			primitives.containingAsIndexedEmbedded().set( containedEntity, entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", "initialValue" )
							) );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContained containedEntity = session.get( primitives.getContainedClass(), 2 );

			// Do NOT update the association on either side; that's on purpose.
			// But DO force loading on contained side:
			// if the association is lazy and unloaded, it won't be loadable after the deletion
			// and the deletion even will simply be ignored.
			primitives.containingAsIndexedEmbedded().get( containedEntity );

			session.remove( containedEntity );

			backendMock.expectWorks( primitives.getIndexName() )
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
				primitives.isAssociationOwnedByContainedSide() );

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );
			TContained containedEntity = primitives.newContained( 2 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			session.persist( entity1 );
			session.persist( containedEntity );

			primitives.containedIndexedEmbedded().set( entity1, containedEntity );
			primitives.containingAsIndexedEmbedded().set( containedEntity, entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "containedIndexedEmbedded", b2 -> b2
									.field( "indexedField", "initialValue" )
							) );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );
			// Make sure we initialize the association from indexed to contained;
			// the magic is in the fact that Hibernate doesn't index the contained entity
			// even though it's referenced by the Java representation of the association.
			TContained containedEntity = primitives.containedIndexedEmbedded().get( entity1 );

			// Do NOT update the association on either side; that's on purpose.
			// But DO force loading on contained side:
			// if the association is lazy and unloaded, it won't be loadable after the deletion
			// and the deletion even will simply be ignored.
			primitives.containingAsIndexedEmbedded().get( containedEntity );

			session.remove( containedEntity );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4305")
	public void indirectAssociationUpdate_indexedEmbedded() {
		setupHolder.runInTransaction( session -> {
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
		setupHolder.runInTransaction( session -> {
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
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 5 );
			primitives.indexedField().set( containedEntity, "updatedValue" );

			TContained oldContained = primitives.containedIndexedEmbedded().get( containingEntity1 );
			primitives.containingAsIndexedEmbedded().clear( oldContained );
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
		setupHolder.runInTransaction( session -> {
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
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained oldContained = primitives.containedIndexedEmbedded().get( containingEntity1 );
			primitives.containingAsIndexedEmbedded().clear( oldContained );
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
	public final void indirectAssociationUpdate_nonIndexedEmbedded() {
		setupHolder.runInTransaction( session -> {
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
		setupHolder.runInTransaction( session -> {
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
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 5 );
			primitives.indexedField().set( containedEntity, "updatedValue" );

			TContained oldContained = primitives.containedNonIndexedEmbedded().get( containingEntity1 );
			primitives.containingAsNonIndexedEmbedded().clear( oldContained );
			primitives.containedNonIndexedEmbedded().set( containingEntity1, containedEntity );
			primitives.containingAsNonIndexedEmbedded().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained oldContained = primitives.containedNonIndexedEmbedded().get( containingEntity1 );
			primitives.containingAsNonIndexedEmbedded().clear( oldContained );
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
	@TestForIssue(jiraKey = { "HSEARCH-4001", "HSEARCH-4305" })
	public void indirectAssociationUpdate_indexedEmbeddedShallowReindexOnUpdate() {
		setupHolder.runInTransaction( session -> {
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
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 4 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().set( containingEntity1, containedEntity );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().set( containedEntity, containingEntity1 );

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
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 5 );
			primitives.indexedField().set( containedEntity, "updatedValue" );

			TContained oldContained = primitives.containedIndexedEmbeddedShallowReindexOnUpdate().get( containingEntity1 );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().clear( oldContained );
			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().set( containingEntity1, containedEntity );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().set( containedEntity, containingEntity1 );

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
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );
			TContained containedEntity = session.get( primitives.getContainedClass(), 5 );

			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().clear( containingEntity1 );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().clear( containedEntity );

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
	public final void indirectAssociationUpdate_indexedEmbeddedNoReindexOnUpdate() {
		setupHolder.runInTransaction( session -> {
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
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 4 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			primitives.containedIndexedEmbeddedNoReindexOnUpdate().set( containingEntity1, containedEntity );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 5 );
			primitives.indexedField().set( containedEntity, "updatedValue" );

			TContained oldContained = primitives.containedIndexedEmbeddedNoReindexOnUpdate().get( containingEntity1 );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().clear( oldContained );
			primitives.containedIndexedEmbeddedNoReindexOnUpdate().set( containingEntity1, containedEntity );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );
			TContained containedEntity = session.get( primitives.getContainedClass(), 5 );

			primitives.containedIndexedEmbeddedNoReindexOnUpdate().clear( containingEntity1 );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().clear( containedEntity );

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
		setupHolder.runInTransaction( session -> {
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
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 5 );
			primitives.fieldUsedInCrossEntityDerivedField1().set( containedEntity, "field1_updatedValue" );
			primitives.fieldUsedInCrossEntityDerivedField2().set( containedEntity, "field2_updatedValue" );

			TContained oldContained = primitives.containedUsedInCrossEntityDerivedProperty().get( containingEntity1 );
			primitives.containingAsUsedInCrossEntityDerivedProperty().clear( oldContained );
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
		setupHolder.runInTransaction( session -> {
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
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained oldContained = primitives.containedUsedInCrossEntityDerivedProperty().get( containingEntity1 );
			primitives.containingAsUsedInCrossEntityDerivedProperty().clear( oldContained );
			primitives.containedUsedInCrossEntityDerivedProperty().clear( containingEntity1 );

			backendMock.expectWorks( primitives.getIndexName() )
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
				primitives.isAssociationOwnedByContainedSide() );
		assumeTrue( "This test can only succeed if the containing side of the association is loaded after the contained entity is inserted."
						+ " See the paragraph starting with \"By the way\" in"
						+ " https://discourse.hibernate.org/t/hs6-not-indexing-add-or-delete-only-update-with-onetomany-indexedembedded/5638/6",
				primitives.isAssociationLazyOnContainingSide() || !setupHolder.areEntitiesProcessedInSession() );

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } ) );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( primitives.getContainingClass(), 2 );

			TContained containedEntity = primitives.newContained( 2 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			// Do NOT set the association on the containing side; that's on purpose.
			// Only set it on the contained side.
			primitives.containingAsIndexedEmbedded().set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( primitives.getIndexName() )
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
				primitives.isAssociationOwnedByContainedSide() );
		assumeTrue( "This test can only succeed if the containing side of the association is loaded after the contained entity is inserted."
						+ " See the paragraph starting with \"By the way\" in"
						+ " https://discourse.hibernate.org/t/hs6-not-indexing-add-or-delete-only-update-with-onetomany-indexedembedded/5638/6",
				primitives.isAssociationLazyOnContainingSide() || !setupHolder.areEntitiesProcessedInSession() );

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );
			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );
			TContained containedEntity = primitives.newContained( 2 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			session.persist( containingEntity1 );
			session.persist( entity1 );
			session.persist( containedEntity );

			primitives.containedIndexedEmbedded().set( containingEntity1, containedEntity );
			primitives.containingAsIndexedEmbedded().set( containedEntity, containingEntity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "initialValue" )
									) ) );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContained containedEntity = session.get( primitives.getContainedClass(), 2 );

			// Do NOT update the association on either side; that's on purpose.
			// But DO force loading on contained side:
			// if the association is lazy and unloaded, it won't be loadable after the deletion
			// and the deletion even will simply be ignored.
			primitives.containingAsIndexedEmbedded().get( containedEntity );

			session.remove( containedEntity );

			backendMock.expectWorks( primitives.getIndexName() )
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
				primitives.isAssociationOwnedByContainedSide() );

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );
			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );
			TContained containedEntity = primitives.newContained( 2 );
			primitives.indexedField().set( containedEntity, "initialValue" );

			session.persist( containingEntity1 );
			session.persist( entity1 );
			session.persist( containedEntity );

			primitives.containedIndexedEmbedded().set( containingEntity1, containedEntity );
			primitives.containingAsIndexedEmbedded().set( containedEntity, containingEntity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "initialValue" )
									) ) );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( primitives.getContainingClass(), 2 );
			// Make sure we initialize the association from containing to contained;
			// the magic is in the fact that Hibernate doesn't index the contained entity
			// even though it's referenced by the Java representation of the association.
			TContained containedEntity = primitives.containedIndexedEmbedded().get( containing );

			// Do NOT update the association on either side; that's on purpose.
			// But DO force loading on contained side:
			// if the association is lazy and unloaded, it won't be loadable after the deletion
			// and the deletion even will simply be ignored.
			primitives.containingAsIndexedEmbedded().get( containedEntity );

			session.remove( containedEntity );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> { } ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_indexedEmbedded_singleValue_indexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = primitives.newContaining( 3 );
			primitives.child().set( containingEntity1, deeplyNestedContainingEntity );
			primitives.parent().set( deeplyNestedContainingEntity, containingEntity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedField().set( contained1, "initialValue" );
			primitives.containedIndexedEmbedded().set( containingEntity1, contained1 );
			primitives.containingAsIndexedEmbedded().set( contained1, containingEntity1 );

			TContained contained2 = primitives.newContained( 5 );
			primitives.indexedField().set( contained2, "initialOutOfScopeValue" );
			primitives.containedIndexedEmbedded().set( deeplyNestedContainingEntity, contained2 );
			primitives.containingAsIndexedEmbedded().set( contained2, deeplyNestedContainingEntity );

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
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.indexedField().set( contained, "updatedValue" );

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

		// Test updating a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( primitives.getContainedClass(), 5 );
			primitives.indexedField().set( contained, "updatedOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4137")
	public void directValueUpdate_nonIndexed_then_indirectValueUpdate_indexedEmbedded_singleValue_indexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );
			primitives.containingEntityNonIndexedField().set( entity1, "initialValue" );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedField().set( contained1, "initialValue" );
			primitives.containedIndexedEmbedded().set( containingEntity1, contained1 );
			primitives.containingAsIndexedEmbedded().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TIndexed indexed = session.get( primitives.getIndexedClass(), 1 );
			primitives.containingEntityNonIndexedField().set( indexed, "updatedValue" );
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.indexedField().set( contained, "updatedValue" );

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
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.nonIndexedField().set( contained1, "initialValue" );
			primitives.containedIndexedEmbedded().set( containingEntity1, contained1 );
			primitives.containingAsIndexedEmbedded().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.nonIndexedField().set( contained, "updatedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_indexedEmbedded_elementCollectionValue_indexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = primitives.newContaining( 3 );
			primitives.child().set( containingEntity1, deeplyNestedContainingEntity );
			primitives.parent().set( deeplyNestedContainingEntity, containingEntity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedElementCollectionField().add( contained1, "firstValue" );
			primitives.containedIndexedEmbedded().set( containingEntity1, contained1 );
			primitives.containingAsIndexedEmbedded().set( contained1, containingEntity1 );

			TContained contained2 = primitives.newContained( 5 );
			primitives.indexedElementCollectionField().add( contained2, "firstOutOfScopeValue" );
			primitives.containedIndexedEmbedded().set( deeplyNestedContainingEntity, contained2 );
			primitives.containingAsIndexedEmbedded().set( contained2, deeplyNestedContainingEntity );

			session.persist( contained1 );
			session.persist( contained2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.indexedElementCollectionField().add( contained, "secondValue" );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.indexedElementCollectionField().remove( contained, "firstValue" );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 5 );
			primitives.indexedElementCollectionField().add( contained, "secondOutOfScopeValue" );

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
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = primitives.newContaining( 3 );
			primitives.child().set( containingEntity1, deeplyNestedContainingEntity );
			primitives.parent().set( deeplyNestedContainingEntity, containingEntity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedElementCollectionField().add( contained1, "firstValue" );
			primitives.containedIndexedEmbedded().set( containingEntity1, contained1 );
			primitives.containingAsIndexedEmbedded().set( contained1, containingEntity1 );

			TContained contained2 = primitives.newContained( 5 );
			primitives.indexedElementCollectionField().add( contained2, "firstOutOfScopeValue" );
			primitives.containedIndexedEmbedded().set( deeplyNestedContainingEntity, contained2 );
			primitives.containingAsIndexedEmbedded().set( contained2, deeplyNestedContainingEntity );

			session.persist( contained1 );
			session.persist( contained2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.indexedElementCollectionField().setContainer( contained, new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 5 );
			primitives.indexedElementCollectionField().setContainer( contained, new ArrayList<>( Arrays.asList(
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
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.nonIndexedElementCollectionField().add( contained1, "firstValue" );
			primitives.containedIndexedEmbedded().set( containingEntity1, contained1 );
			primitives.containingAsIndexedEmbedded().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.nonIndexedElementCollectionField().add( contained, "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.nonIndexedElementCollectionField().remove( contained, "firstValue" );

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
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.nonIndexedElementCollectionField().add( contained1, "firstValue" );
			primitives.containedIndexedEmbedded().set( containingEntity1, contained1 );
			primitives.containingAsIndexedEmbedded().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.nonIndexedElementCollectionField().setContainer( contained, new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( primitives.getIndexName() )
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
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.fieldUsedInContainedDerivedField1().set( contained1, "field1_initialValue" );
			primitives.fieldUsedInContainedDerivedField2().set( contained1, "field2_initialValue" );
			primitives.containedIndexedEmbedded().set( containingEntity1, contained1 );
			primitives.containingAsIndexedEmbedded().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.fieldUsedInContainedDerivedField1().set( contained, "field1_updatedValue" );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.fieldUsedInContainedDerivedField2().set( contained, "field2_updatedValue" );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.fieldUsedInCrossEntityDerivedField1().set( contained1, "field1_initialValue" );
			primitives.fieldUsedInCrossEntityDerivedField2().set( contained1, "field2_initialValue" );
			primitives.containedUsedInCrossEntityDerivedProperty().set( containingEntity1, contained1 );
			primitives.containingAsUsedInCrossEntityDerivedProperty().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.fieldUsedInCrossEntityDerivedField1().set( contained, "field1_updatedValue" );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.fieldUsedInCrossEntityDerivedField2().set( contained, "field2_updatedValue" );

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
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4001")
	public void indirectValueUpdate_indexedEmbeddedShallowReindexOnUpdate_singleValue_indexed() {
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedField().set( contained1, "initialValue" );
			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().set( containingEntity1, contained1 );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.indexedField().set( contained, "updatedValue" );

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
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedElementCollectionField().add( contained1, "firstValue" );
			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().set( containingEntity1, contained1 );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.indexedElementCollectionField().add( contained, "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.indexedElementCollectionField().remove( contained, "firstValue" );

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
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedElementCollectionField().add( contained1, "firstValue" );
			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().set( containingEntity1, contained1 );
			primitives.containingAsIndexedEmbeddedShallowReindexOnUpdate().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.indexedElementCollectionField().setContainer( contained, new ArrayList<>( Arrays.asList(
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
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedField().set( contained1, "initialValue" );
			primitives.containedIndexedEmbeddedNoReindexOnUpdate().set( containingEntity1, contained1 );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().set( contained1, containingEntity1 );

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
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.indexedField().set( contained, "updatedValue" );

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
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedElementCollectionField().add( contained1, "firstValue" );
			primitives.containedIndexedEmbeddedNoReindexOnUpdate().set( containingEntity1, contained1 );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.indexedElementCollectionField().add( contained, "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.indexedElementCollectionField().remove( contained, "firstValue" );

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
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedElementCollectionField().add( contained1, "firstValue" );
			primitives.containedIndexedEmbeddedNoReindexOnUpdate().set( containingEntity1, contained1 );
			primitives.containingAsIndexedEmbeddedNoReindexOnUpdate().set( contained1, containingEntity1 );

			session.persist( contained1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.indexedElementCollectionField().setContainer( contained, new ArrayList<>( Arrays.asList(
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
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = primitives.newContaining( 3 );
			primitives.child().set( containingEntity1, deeplyNestedContainingEntity );
			primitives.parent().set( deeplyNestedContainingEntity, containingEntity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedField().set( contained1, "initialValue" );
			primitives.containedIndexedEmbeddedWithCast().set( containingEntity1, contained1 );
			primitives.containingAsIndexedEmbeddedWithCast().set( contained1, containingEntity1 );

			TContained contained2 = primitives.newContained( 5 );
			primitives.indexedField().set( contained2, "initialOutOfScopeValue" );
			primitives.containedIndexedEmbeddedWithCast().set( deeplyNestedContainingEntity, contained2 );
			primitives.containingAsIndexedEmbeddedWithCast().set( contained2, deeplyNestedContainingEntity );

			session.persist( contained1 );
			session.persist( contained2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.indexedField().set( contained, "updatedValue" );

			backendMock.expectWorks( primitives.getIndexName() )
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
			TContained contained = session.get( primitives.getContainedClass(), 5 );
			primitives.indexedField().set( contained, "updatedOutOfScopeValue" );

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

	public interface ModelPrimitives<
					TIndexed extends TContaining,
					TContaining,
					TContained
			> {
		String getIndexName();

		boolean isMultiValuedAssociation();

		boolean isAssociationOwnedByContainedSide();

		boolean isAssociationLazyOnContainingSide();

		Class<TIndexed> getIndexedClass();

		Class<TContaining> getContainingClass();

		Class<TContained> getContainedClass();

		TIndexed newIndexed(int id);

		TContaining newContaining(int id);

		TContained newContained(int id);

		SingleValuedPropertyAccessor<TContaining, String> containingEntityNonIndexedField();

		SingleValuedPropertyAccessor<TContaining, TContaining> child();

		SingleValuedPropertyAccessor<TContaining, TContaining> parent();

		PropertyAccessor<TContaining, TContained> containedIndexedEmbedded();

		PropertyAccessor<TContained, TContaining> containingAsIndexedEmbedded();

		PropertyAccessor<TContaining, TContained> containedNonIndexedEmbedded();

		PropertyAccessor<TContained, TContaining> containingAsNonIndexedEmbedded();

		PropertyAccessor<TContaining, TContained> containedIndexedEmbeddedShallowReindexOnUpdate();

		PropertyAccessor<TContained, TContaining> containingAsIndexedEmbeddedShallowReindexOnUpdate();

		PropertyAccessor<TContaining, TContained> containedIndexedEmbeddedNoReindexOnUpdate();

		PropertyAccessor<TContained, TContaining> containingAsIndexedEmbeddedNoReindexOnUpdate();

		PropertyAccessor<TContaining, TContained> containedUsedInCrossEntityDerivedProperty();

		PropertyAccessor<TContained, TContaining> containingAsUsedInCrossEntityDerivedProperty();

		PropertyAccessor<TContaining, TContained> containedIndexedEmbeddedWithCast();

		PropertyAccessor<TContained, TContaining> containingAsIndexedEmbeddedWithCast();

		SingleValuedPropertyAccessor<TContained, String> indexedField();

		SingleValuedPropertyAccessor<TContained, String> nonIndexedField();

		MultiValuedPropertyAccessor<TContained, String, List<String>> indexedElementCollectionField();

		MultiValuedPropertyAccessor<TContained, String, List<String>> nonIndexedElementCollectionField();

		SingleValuedPropertyAccessor<TContained, String> fieldUsedInContainedDerivedField1();

		SingleValuedPropertyAccessor<TContained, String> fieldUsedInContainedDerivedField2();

		SingleValuedPropertyAccessor<TContained, String> fieldUsedInCrossEntityDerivedField1();

		SingleValuedPropertyAccessor<TContained, String> fieldUsedInCrossEntityDerivedField2();
	}
}
