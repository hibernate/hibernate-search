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
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNodeDiffer;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
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
 *                 directEmbeddedAssociationReplace: replace the entire embedded holding the association
 *                 (not clear/add, but really use a different embedded object, with a different collection if relevant)
 *                 directly on the indexed entity
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
 *         indexed-embedded, non-indexed-embedded, indexed-embbedded with ReindexOnUpdate.SHALLOW,
 *         indexed-embbedded with ReindexOnUpdate.NO,
 *         indexed as a side effect of being used in a cross-entity derived property,
 *         in an embeddable (embeddedAssociations) and indexed-embbedded,
 *         in an embeddable (embeddedAssociations) and non-indexed-embbedded.
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
				TIndexed extends TContaining, TContaining, TContainingEmbeddable, TContained, TContainedEmbeddable
		> {

	/*
	 * Make sure that the values are in lexicographical order, so that SortedMap tests
	 * using these values as keys work correctly.
	 */
	protected final String VALUE_1 = "1 - firstValue";
	protected final String VALUE_2 = "2 - secondValue";
	protected final String VALUE_3 = "3 - thirdValue";
	protected final String VALUE_4 = "4 - fourthValue";

	protected static AssertionFailure primitiveNotSupported() {
		throw new AssertionFailure(
				"This primitive is not supported for this association type and this method should not have been called."
						+ " There is most likely a bug in the implementation of tests (missing primitive or test not ignored properly)." );
	}

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	private final IndexedEntityPrimitives<TIndexed> indexedPrimitives;

	private final ContainingEntityPrimitives<TContaining, TContainingEmbeddable, TContained> containingPrimitives;
	private final ContainingEmbeddablePrimitives<TContainingEmbeddable, TContained> containingEmbeddablePrimitives;

	private final ContainedEntityPrimitives<TContained, TContainedEmbeddable, TContaining> containedPrimitives;
	private final ContainedEmbeddablePrimitives<TContainedEmbeddable, TContaining> containedEmbeddablePrimitives;

	public AbstractAutomaticIndexingAssociationBaseIT(IndexedEntityPrimitives<TIndexed> indexedPrimitives,
			ContainingEntityPrimitives<TContaining, TContainingEmbeddable, TContained> containingPrimitives,
			ContainingEmbeddablePrimitives<TContainingEmbeddable, TContained> containingEmbeddablePrimitives,
			ContainedEntityPrimitives<TContained, TContainedEmbeddable, TContaining> containedPrimitives,
			ContainedEmbeddablePrimitives<TContainedEmbeddable, TContaining> containedEmbeddablePrimitives) {
		this.indexedPrimitives = indexedPrimitives;
		this.containingPrimitives = containingPrimitives;
		this.containingEmbeddablePrimitives = containingEmbeddablePrimitives;
		this.containedPrimitives = containedPrimitives;
		this.containedEmbeddablePrimitives = containedEmbeddablePrimitives;
	}

	protected abstract boolean isAssociationMultiValuedOnContainingSide();

	protected abstract boolean isAssociationMultiValuedOnContainedSide();

	protected abstract boolean isAssociationOwnedByContainedSide();

	protected abstract boolean isAssociationLazyOnContainingSide();

	protected boolean isEmbeddedAssociationChangeCausingWork() {
		return !isAssociationOwnedByContainedSide() && !isAssociationMultiValuedOnContainingSide();
	}

	private boolean isElementCollectionAssociationsOnContainingSide() {
		return !isAssociationOwnedByContainedSide() && !isAssociationMultiValuedOnContainingSide();
	}

	private void assumeElementCollectionAssociationsOnContainingSide() {
		assumeTrue( "This test only makes sense if there is an element collection with nested associations"
				+ " on the containing side,"
				+ " which requires that the associations be owned by the containing side"
				+ " and be single-valued on the containing side.",
				isElementCollectionAssociationsOnContainingSide() );
	}

	private boolean isElementCollectionAssociationsOnContainedSide() {
		return isAssociationOwnedByContainedSide() && !isAssociationMultiValuedOnContainedSide();
	}

	private void assumeElementCollectionAssociationsOnContainedSide() {
		assumeTrue( "This test only makes sense if there is an element collection with nested associations"
						+ " on the contained side,"
						+ " which requires that the associations be owned by the contained side"
						+ " and be single-valued on the contained side.",
				isElementCollectionAssociationsOnContainedSide() );
	}

	protected IndexedEntityPrimitives<TIndexed> _indexed() {
		return indexedPrimitives;
	}

	protected ContainingEntityPrimitives<TContaining, TContainingEmbeddable, TContained> _containing() {
		return containingPrimitives;
	}

	protected ContainingEmbeddablePrimitives<TContainingEmbeddable, TContained> _containingEmbeddable() {
		return containingEmbeddablePrimitives;
	}

	protected ContainedEntityPrimitives<TContained, TContainedEmbeddable, TContaining> _contained() {
		return containedPrimitives;
	}

	protected ContainedEmbeddablePrimitives<TContainedEmbeddable, TContaining> _containedEmbeddable() {
		return containedEmbeddablePrimitives;
	}

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext, ReusableOrmSetupHolder.DataClearConfig dataClearConfig) {
		Consumer<StubIndexSchemaDataNode.Builder> associationFieldContributor = b -> {
			if ( isAssociationMultiValuedOnContainingSide() ) {
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
				.objectField( "embeddedAssociations", b2 -> b2
					.objectField( "containedIndexedEmbedded",
							associationFieldContributor.andThen( b3 -> b3
									.field( "indexedField", String.class )
									.field( "indexedElementCollectionField", String.class, b4 -> b4.multiValued( true ) )
									.field( "containedDerivedField", String.class )
							)
					)
				)
				.with( isElementCollectionAssociationsOnContainingSide() ? bWith -> bWith
						.objectField( "elementCollectionAssociations", b2 -> b2
								.multiValued( true )
								.objectField( "containedIndexedEmbedded",
										associationFieldContributor.andThen( b3 -> b3
												.field( "indexedField", String.class )
												.field( "indexedElementCollectionField", String.class, b4 -> b4.multiValued( true ) )
												.field( "containedDerivedField", String.class )
										)
								)
						)
						: bWith -> { } )
				.with( isElementCollectionAssociationsOnContainedSide() ? bWith -> bWith
						.objectField( "containedElementCollectionAssociationsIndexedEmbedded",
								associationFieldContributor.andThen( b2 -> b2
										.field( "indexedField", String.class )
										.field( "indexedElementCollectionField", String.class, b3 -> b3.multiValued( true ) )
										.field( "containedDerivedField", String.class )
								)
						)
						: bWith -> { } )
				.field( "crossEntityDerivedField", String.class )
				.objectField( "child", bChild -> bChild
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
						.objectField( "embeddedAssociations", b2 -> b2
								.objectField( "containedIndexedEmbedded",
										associationFieldContributor.andThen( b3 -> b3
												.field( "indexedField", String.class )
												.field( "indexedElementCollectionField", String.class, b4 -> b4.multiValued( true ) )
												.field( "containedDerivedField", String.class )
										)
								)
						)
						.with( isElementCollectionAssociationsOnContainingSide() ? bWith -> bWith
								.objectField( "elementCollectionAssociations", b2 -> b2
										.multiValued( true )
										.objectField( "containedIndexedEmbedded",
												associationFieldContributor.andThen( b3 -> b3
														.field( "indexedField", String.class )
														.field( "indexedElementCollectionField", String.class, b4 -> b4.multiValued( true ) )
														.field( "containedDerivedField", String.class )
												)
										)
								)
								: bWith -> { } )
						.with( isElementCollectionAssociationsOnContainedSide() ? bWith -> bWith
								.objectField( "containedElementCollectionAssociationsIndexedEmbedded",
										associationFieldContributor.andThen( b2 -> b2
												.field( "indexedField", String.class )
												.field( "indexedElementCollectionField", String.class, b3 -> b3.multiValued( true ) )
												.field( "containedDerivedField", String.class )
										)
								)
								: bWith -> { } )
						.field( "crossEntityDerivedField", String.class )
				)
		);

		// Embedded deserialization rules in ORM are just weird:
		// when all columns are empty, an embedded is sometimes deserialized as `null`
		// (though that can be prevented with hibernate.create_empty_composites.enabled = true),
		// and this applies to embeddeds in @ElementCollections as well.
		// That's just too much noise when writing indexing assertions,
		// so we'll consider null equivalent to an empty instance for embeddeds.
		backendMock.documentDiffer( _indexed().indexName(), StubTreeNodeDiffer.<StubDocumentNode>builder()
				.missingEquivalentToEmptyForPath( "embeddedAssociations" )
				.missingEquivalentToEmptyForPath( "elementCollectionAssociations" )
				.missingEquivalentToEmptyForPath( "child.embeddedAssociations" )
				.missingEquivalentToEmptyForPath( "child.elementCollectionAssociations" )
				.build() );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

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
			field.set( containedEntity, "initialValue" );

			containingAssociation.set( entity1, containedEntity );
			containedAssociation.set( containedEntity, entity1 );

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
			field.set( containedEntity, "updatedValue" );

			TContained oldContained = containingAssociation.get( entity1 );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( entity1 );

				if ( setupHolder.areEntitiesProcessedInSession() ) {
					backendMock.expectWorks( _indexed().indexName() )
							.addOrUpdate( "1", b -> {
							} );
				}
				session.flush();
			}

			containingAssociation.set( entity1, containedEntity );
			containedAssociation.set( containedEntity, entity1 );

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

			TContained oldContained = containingAssociation.get( entity1 );
			containedAssociation.clear( oldContained );
			containingAssociation.clear( entity1 );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedNonIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsNonIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

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
			field.set( containedEntity, "initialValue" );

			containingAssociation.set( entity1, containedEntity );
			containedAssociation.set( containedEntity, entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained containedEntity = _contained().newInstance( 3 );
			field.set( containedEntity, "updatedValue" );

			TContained oldContained = containingAssociation.get( entity1 );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( entity1 );
				session.flush();
			}

			containingAssociation.set( entity1, containedEntity );
			containedAssociation.set( containedEntity, entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained oldContained = containingAssociation.get( entity1 );
			containedAssociation.clear( oldContained );
			containingAssociation.clear( entity1 );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbeddedShallowReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbeddedShallowReindexOnUpdate();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

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
			field.set( containedEntity, "initialValue" );

			containingAssociation.set( entity1, containedEntity );
			containedAssociation.set( containedEntity, entity1 );

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
			field.set( containedEntity, "updatedValue" );

			TContained oldContained = containingAssociation.get( entity1 );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( entity1 );

				if ( setupHolder.areEntitiesProcessedInSession() ) {
					backendMock.expectWorks( _indexed().indexName() )
							.addOrUpdate( "1", b -> {
							} );
				}
				session.flush();
			}

			containingAssociation.set( entity1, containedEntity );
			containedAssociation.set( containedEntity, entity1 );

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

			TContained oldContained = containingAssociation.get( entity1 );
			containedAssociation.clear( oldContained );
			containingAssociation.clear( entity1 );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbeddedNoReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbeddedNoReindexOnUpdate();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

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
			field.set( containedEntity, "initialValue" );

			containingAssociation.set( entity1, containedEntity );
			containedAssociation.set( containedEntity, entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained containedEntity = _contained().newInstance( 3 );
			field.set( containedEntity, "updatedValue" );

			TContained oldContained = containingAssociation.get( entity1 );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( entity1 );
				session.flush();
			}

			containingAssociation.set( entity1, containedEntity );
			containedAssociation.set( containedEntity, entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained oldContained = containingAssociation.get( entity1 );
			containedAssociation.clear( oldContained );
			containingAssociation.clear( entity1 );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public void directAssociationUpdate_embeddedAssociationsIndexedEmbedded() {
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().embeddedAssociations()
				.andThen( _containingEmbeddable()::newInstance, _containingEmbeddable().containedIndexedEmbedded() );
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().embeddedAssociations()
				.andThen( _containedEmbeddable()::newInstance, _containedEmbeddable().containingAsIndexedEmbedded() );
		PropertyAccessor<TContained, String> field = _contained().indexedField();

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
			field.set( containedEntity, "initialValue" );

			containingAssociation.set( entity1, containedEntity );
			containedAssociation.set( containedEntity, entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "embeddedAssociations", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "initialValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained containedEntity = _contained().newInstance( 3 );
			field.set( containedEntity, "updatedValue" );

			TContained oldContained = containingAssociation.get( entity1 );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( entity1 );

				if ( setupHolder.areEntitiesProcessedInSession() ) {
					backendMock.expectWorks( _indexed().indexName() )
							.addOrUpdate( "1", b -> {
							} );
				}
				session.flush();
			}

			containingAssociation.set( entity1, containedEntity );
			containedAssociation.set( containedEntity, entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "embeddedAssociations", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "updatedValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained oldContained = containingAssociation.get( entity1 );
			containedAssociation.clear( oldContained );
			containingAssociation.clear( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public void directAssociationUpdate_embeddedAssociationsNonIndexedEmbedded() {
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().embeddedAssociations()
				.andThen( _containingEmbeddable()::newInstance, _containingEmbeddable().containedNonIndexedEmbedded() );
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().embeddedAssociations()
				.andThen( _containedEmbeddable()::newInstance, _containedEmbeddable().containingAsNonIndexedEmbedded() );
		PropertyAccessor<TContained, String> field = _contained().indexedField();

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
			field.set( containedEntity, "initialValue" );

			containingAssociation.set( entity1, containedEntity );
			containedAssociation.set( containedEntity, entity1 );

			session.persist( containedEntity );

			if ( isAssociationOwnedByContainedSide() || isAssociationMultiValuedOnContainingSide() ) {
				// Do not expect any work
			}
			else {
				// HSEARCH-4718: we cannot distinguish between relevant and irrelevant properties
				// for changes within an embeddable.
				backendMock.expectWorks( _indexed().indexName() )
						.addOrUpdate( "1", b -> { } );
			}
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained containedEntity = _contained().newInstance( 3 );
			field.set( containedEntity, "updatedValue" );

			TContained oldContained = containingAssociation.get( entity1 );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( entity1 );
				if ( isEmbeddedAssociationChangeCausingWork() && setupHolder.areEntitiesProcessedInSession() ) {
					backendMock.expectWorks( _indexed().indexName() )
							.addOrUpdate( "1", b ->
									b.objectField( "embeddedAssociations", b2 -> { } ) );
				}
				session.flush();
			}

			containingAssociation.set( entity1, containedEntity );
			containedAssociation.set( containedEntity, entity1 );

			session.persist( containedEntity );

			if ( isAssociationOwnedByContainedSide() || isAssociationMultiValuedOnContainingSide() ) {
				// Do not expect any work
			}
			else {
				// HSEARCH-4718: we cannot distinguish between relevant and irrelevant properties
				// for changes within an embeddable.
				backendMock.expectWorks( _indexed().indexName() )
						.addOrUpdate( "1", b -> { } );
			}
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained oldContained = containingAssociation.get( entity1 );
			containedAssociation.clear( oldContained );
			containingAssociation.clear( entity1 );

			if ( isAssociationOwnedByContainedSide() || isAssociationMultiValuedOnContainingSide() ) {
				// Do not expect any work
			}
			else {
				// HSEARCH-4718: we cannot distinguish between relevant and irrelevant properties
				// for changes within an embeddable.
				backendMock.expectWorks( _indexed().indexName() )
						.addOrUpdate( "1", b -> { } );
			}
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

		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

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
			field.set( containedEntity, "initialValue" );

			// Do NOT set the association on the containing side; that's on purpose.
			// Only set it on the contained side.
			containedAssociation.set( containedEntity, entity1 );

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

		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );
			TContained containedEntity = _contained().newInstance( 2 );
			field.set( containedEntity, "initialValue" );

			session.persist( entity1 );
			session.persist( containedEntity );

			containingAssociation.set( entity1, containedEntity );
			containedAssociation.set( containedEntity, entity1 );

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
			containedAssociation.get( containedEntity );

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

		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );
			TContained containedEntity = _contained().newInstance( 2 );
			field.set( containedEntity, "initialValue" );

			session.persist( entity1 );
			session.persist( containedEntity );

			containingAssociation.set( entity1, containedEntity );
			containedAssociation.set( containedEntity, entity1 );

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
			TContained containedEntity = containingAssociation.get( entity1 );

			// Do NOT update the association on either side; that's on purpose.
			// But DO force loading on contained side:
			// if the association is lazy and unloaded, it won't be loadable after the deletion
			// and the deletion even will simply be ignored.
			containedAssociation.get( containedEntity );

			session.remove( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public void directEmbeddedAssociationReplace_embeddedAssociationsIndexedEmbedded() {
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().embeddedAssociations()
				.andThen( _containingEmbeddable()::newInstance, _containingEmbeddable().containedIndexedEmbedded() );
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().embeddedAssociations()
				.andThen( _containedEmbeddable()::newInstance, _containedEmbeddable().containingAsIndexedEmbedded() );
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, "initialValue" );

			containingAssociation.set( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "embeddedAssociations", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "initialValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, "updatedValue" );

			TContained oldContained = containingAssociation.get( entity1 );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( entity1 );

				if ( setupHolder.areEntitiesProcessedInSession() ) {
					backendMock.expectWorks( _indexed().indexName() )
							.addOrUpdate( "1", b -> { } );
				}
				session.flush();
			}

			_containing().embeddedAssociations().set( entity1, _containingEmbeddable().newInstance() );
			containingAssociation.set( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "embeddedAssociations", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", "updatedValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public void directEmbeddedAssociationReplace_embeddedAssociationsNonIndexedEmbedded() {
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().embeddedAssociations()
				.andThen( _containingEmbeddable()::newInstance, _containingEmbeddable().containedNonIndexedEmbedded() );
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().embeddedAssociations()
				.andThen( _containedEmbeddable()::newInstance, _containedEmbeddable().containingAsNonIndexedEmbedded() );
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, "initialValue" );

			containingAssociation.set( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );
			session.persist( entity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, "updatedValue" );

			TContained oldContained = containingAssociation.get( entity1 );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( entity1 );
				if ( isEmbeddedAssociationChangeCausingWork() && setupHolder.areEntitiesProcessedInSession() ) {
					backendMock.expectWorks( _indexed().indexName() )
							.addOrUpdate( "1", b ->
									b.objectField( "embeddedAssociations", b2 -> { } ) );
				}
				session.flush();
			}

			_containing().embeddedAssociations().set( entity1, _containingEmbeddable().newInstance() );
			containingAssociation.set( entity1, contained );
			containedAssociation.set( contained, entity1 );

			session.persist( contained );

			if ( isAssociationOwnedByContainedSide() && !isAssociationMultiValuedOnContainingSide() ) {
				// Do not expect any work
			}
			else {
				// For some reason we end up reindexing needlessly,
				// probably because we don't have enough information in the change events
				// (HSEARCH-3204: missing "role" for a replaced collection;
				// HSEARCH-4718: no information about which property changed within an embeddable,
				// ...)
				backendMock.expectWorks( _indexed().indexName() )
						.addOrUpdate( "1", b -> { } );
			}
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public final void directElementCollectionAssociationUpdate_containingSideElementCollectionAssociationsIndexedEmbedded() {
		assumeElementCollectionAssociationsOnContainingSide();

		MultiValuedPropertyAccessor<TContaining, TContainingEmbeddable, List<TContainingEmbeddable>> elementCollectionAssociations =
				_containing().elementCollectionAssociations();
		PropertyAccessor<TContainingEmbeddable, TContained> containingAssociation = _containingEmbeddable().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsElementCollectionAssociationsIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed indexed = _indexed().newInstance( 1 );

			session.persist( indexed );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			TContainingEmbeddable containingEmbeddable = _containingEmbeddable().newInstance();
			elementCollectionAssociations.add( indexed, containingEmbeddable );

			containingAssociation.set( containingEmbeddable, contained );
			containedAssociation.set( contained, indexed );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "elementCollectionAssociations", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			TContainingEmbeddable containingEmbeddable = _containingEmbeddable().newInstance();
			elementCollectionAssociations.add( indexed, containingEmbeddable );

			containingAssociation.set( containingEmbeddable, contained );
			containedAssociation.set( contained, indexed );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "elementCollectionAssociations", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
							.objectField( "elementCollectionAssociations", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_2 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_3 );

			TContainingEmbeddable containingEmbeddable = elementCollectionAssociations.getContainer( indexed ).get( 1 );

			TContained oldContained = containingAssociation.get( containingEmbeddable );
			containedAssociation.clear( oldContained );
			containingAssociation.set( containingEmbeddable, contained );
			containedAssociation.set( contained, indexed );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "elementCollectionAssociations", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
							.objectField( "elementCollectionAssociations", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_3 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing an embeddable
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 5 );
			field.set( contained, VALUE_4 );

			TContainingEmbeddable oldContainingEmbeddable = elementCollectionAssociations.getContainer( indexed ).remove( 1 );
			TContained oldContained = containingAssociation.get( oldContainingEmbeddable );
			containedAssociation.clear( oldContained );

			TContainingEmbeddable containingEmbeddable = _containingEmbeddable().newInstance();
			elementCollectionAssociations.add( indexed, containingEmbeddable );

			containingAssociation.set( containingEmbeddable, contained );
			containedAssociation.set( contained, indexed );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "elementCollectionAssociations", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
							.objectField( "elementCollectionAssociations", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_4 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing an embeddable
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContainingEmbeddable oldContainingEmbeddable = elementCollectionAssociations.getContainer( indexed ).remove( 1 );
			TContained oldContained = containingAssociation.get( oldContainingEmbeddable );
			containedAssociation.clear( oldContained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "elementCollectionAssociations", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContainingEmbeddable oldContainingEmbeddable = elementCollectionAssociations.get( indexed );
			TContained oldContained = containingAssociation.get( oldContainingEmbeddable );
			containingAssociation.clear( oldContainingEmbeddable );
			containedAssociation.clear( oldContained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "elementCollectionAssociations", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public final void directElementCollectionAssociationUpdate_containingSideElementCollectionAssociationsNonIndexedEmbedded() {
		assumeElementCollectionAssociationsOnContainingSide();

		MultiValuedPropertyAccessor<TContaining, TContainingEmbeddable, List<TContainingEmbeddable>> elementCollectionAssociations =
				_containing().elementCollectionAssociations();
		PropertyAccessor<TContainingEmbeddable, TContained> containingAssociation = _containingEmbeddable().containedNonIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsElementCollectionAssociationsNonIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed indexed = _indexed().newInstance( 1 );

			session.persist( indexed );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			TContainingEmbeddable containingEmbeddable = _containingEmbeddable().newInstance();
			elementCollectionAssociations.add( indexed, containingEmbeddable );

			containingAssociation.set( containingEmbeddable, contained );
			containedAssociation.set( contained, indexed );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "elementCollectionAssociations", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			TContainingEmbeddable containingEmbeddable = _containingEmbeddable().newInstance();
			elementCollectionAssociations.add( indexed, containingEmbeddable );

			containingAssociation.set( containingEmbeddable, contained );
			containedAssociation.set( contained, indexed );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "elementCollectionAssociations", b2 -> { } )
							.objectField( "elementCollectionAssociations", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_3 );

			TContainingEmbeddable containingEmbeddable = elementCollectionAssociations.getContainer( indexed ).get( 1 );

			TContained oldContained = containingAssociation.get( containingEmbeddable );
			containedAssociation.clear( oldContained );
			containingAssociation.set( containingEmbeddable, contained );
			containedAssociation.set( contained, indexed );

			session.persist( contained );

			// For some reason we end up reindexing needlessly,
			// probably because we don't have enough information in the change events
			// (HSEARCH-3204: missing "role" for a replaced collection;
			// HSEARCH-4718: no information about which property changed within an embeddable,
			// ...)
			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "elementCollectionAssociations", b2 -> { } )
							.objectField( "elementCollectionAssociations", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing an embeddable
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 5 );
			field.set( contained, VALUE_4 );

			TContainingEmbeddable oldContainingEmbeddable = elementCollectionAssociations.getContainer( indexed ).remove( 1 );
			TContained oldContained = containingAssociation.get( oldContainingEmbeddable );
			containedAssociation.clear( oldContained );

			TContainingEmbeddable containingEmbeddable = _containingEmbeddable().newInstance();
			elementCollectionAssociations.add( indexed, containingEmbeddable );

			containingAssociation.set( containingEmbeddable, contained );
			containedAssociation.set( contained, indexed );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "elementCollectionAssociations", b2 -> { } )
							.objectField( "elementCollectionAssociations", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing an embeddable
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContainingEmbeddable oldContainingEmbeddable = elementCollectionAssociations.getContainer( indexed ).remove( 1 );
			TContained oldContained = containingAssociation.get( oldContainingEmbeddable );
			containedAssociation.clear( oldContained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "elementCollectionAssociations", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = session.get( _indexed().entityClass(), 1 );

			TContainingEmbeddable oldContainingEmbeddable = elementCollectionAssociations.get( entity1 );
			TContained oldContained = containingAssociation.get( oldContainingEmbeddable );
			containedAssociation.clear( oldContained );
			containingAssociation.clear( oldContainingEmbeddable );

			// For some reason we end up reindexing needlessly,
			// probably because we don't have enough information in the change events
			// (HSEARCH-3204: missing "role" for a replaced collection;
			// HSEARCH-4718: no information about which property changed within an embeddable,
			// ...)
			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "elementCollectionAssociations", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public final void directElementCollectionAssociationUpdate_containedSideElementCollectionAssociationsIndexedEmbedded() {
		assumeElementCollectionAssociationsOnContainedSide();

		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedElementCollectionAssociationsIndexedEmbedded();
		MultiValuedPropertyAccessor<TContained, TContainedEmbeddable, List<TContainedEmbeddable>> elementCollectionAssociations =
				_contained().elementCollectionAssociations();
		PropertyAccessor<TContainedEmbeddable, TContaining> containedAssociation = _containedEmbeddable().containingAsIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed indexed = _indexed().newInstance( 1 );

			session.persist( indexed );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			TContainedEmbeddable containedEmbeddable = _containedEmbeddable().newInstance();
			elementCollectionAssociations.add( contained, containedEmbeddable );

			containingAssociation.set( indexed, contained );
			containedAssociation.set( containedEmbeddable, indexed );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedElementCollectionAssociationsIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value, clearing the association on the contained side
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained oldContained = containingAssociation.get( indexed );
			TContainedEmbeddable oldContainedEmbeddable = elementCollectionAssociations.get( oldContained );
			containedAssociation.clear( oldContainedEmbeddable );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			TContainedEmbeddable containedEmbeddable = _containedEmbeddable().newInstance();
			elementCollectionAssociations.add( contained, containedEmbeddable );

			containingAssociation.set( indexed, contained );
			containedAssociation.set( containedEmbeddable, indexed );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedElementCollectionAssociationsIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_2 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value, clearing the element collection on the contained side
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained oldContained = containingAssociation.get( indexed );
			elementCollectionAssociations.clear( oldContained );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_3 );

			TContainedEmbeddable containedEmbeddable = _containedEmbeddable().newInstance();
			elementCollectionAssociations.add( contained, containedEmbeddable );

			containingAssociation.set( indexed, contained );
			containedAssociation.set( containedEmbeddable, indexed );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedElementCollectionAssociationsIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_3 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value, clearing the association on the contained side
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained oldContained = containingAssociation.get( indexed );
			TContainedEmbeddable oldContainedEmbeddable = elementCollectionAssociations.get( oldContained );
			containedAssociation.clear( oldContainedEmbeddable );
			containingAssociation.clear( indexed );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Add back a value, just to remove it in the next test
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 5 );
			field.set( contained, VALUE_1 );

			TContainedEmbeddable containedEmbeddable = _containedEmbeddable().newInstance();
			elementCollectionAssociations.add( contained, containedEmbeddable );

			containingAssociation.set( indexed, contained );
			containedAssociation.set( containedEmbeddable, indexed );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "containedElementCollectionAssociationsIndexedEmbedded", b2 -> b2
									.field( "indexedField", VALUE_1 )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value, clearing the element collection on the contained side
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained oldContained = containingAssociation.get( indexed );
			elementCollectionAssociations.clear( oldContained );
			containingAssociation.clear( indexed );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public final void directElementCollectionAssociationUpdate_containedSideElementCollectionAssociationsNonIndexedEmbedded() {
		assumeElementCollectionAssociationsOnContainedSide();

		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedElementCollectionAssociationsNonIndexedEmbedded();
		MultiValuedPropertyAccessor<TContained, TContainedEmbeddable, List<TContainedEmbeddable>> elementCollectionAssociations =
				_contained().elementCollectionAssociations();
		PropertyAccessor<TContainedEmbeddable, TContaining> containedAssociation = _containedEmbeddable().containingAsNonIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed indexed = _indexed().newInstance( 1 );

			session.persist( indexed );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> { } );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			TContainedEmbeddable containedEmbeddable = _containedEmbeddable().newInstance();
			elementCollectionAssociations.add( contained, containedEmbeddable );

			containingAssociation.set( indexed, contained );
			containedAssociation.set( containedEmbeddable, indexed );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value, clearing the association on the contained side
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained oldContained = containingAssociation.get( indexed );
			TContainedEmbeddable oldContainedEmbeddable = elementCollectionAssociations.get( oldContained );
			containedAssociation.clear( oldContainedEmbeddable );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			TContainedEmbeddable containedEmbeddable = _containedEmbeddable().newInstance();
			elementCollectionAssociations.add( contained, containedEmbeddable );

			containingAssociation.set( indexed, contained );
			containedAssociation.set( containedEmbeddable, indexed );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value, clearing the element collection on the contained side
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained oldContained = containingAssociation.get( indexed );
			elementCollectionAssociations.clear( oldContained );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_3 );

			TContainedEmbeddable containedEmbeddable = _containedEmbeddable().newInstance();
			elementCollectionAssociations.add( contained, containedEmbeddable );

			containingAssociation.set( indexed, contained );
			containedAssociation.set( containedEmbeddable, indexed );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value, clearing the association on the contained side
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained oldContained = containingAssociation.get( indexed );
			TContainedEmbeddable oldContainedEmbeddable = elementCollectionAssociations.get( oldContained );
			containedAssociation.clear( oldContainedEmbeddable );
			containingAssociation.clear( indexed );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Add back a value, just to remove it in the next test
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained contained = _contained().newInstance( 5 );
			field.set( contained, VALUE_1 );

			TContainedEmbeddable containedEmbeddable = _containedEmbeddable().newInstance();
			elementCollectionAssociations.add( contained, containedEmbeddable );

			containingAssociation.set( indexed, contained );
			containedAssociation.set( containedEmbeddable, indexed );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value, clearing the element collection on the contained side
		setupHolder.runInTransaction( session -> {
			TIndexed indexed = session.get( _indexed().entityClass(), 1 );

			TContained oldContained = containingAssociation.get( indexed );
			elementCollectionAssociations.clear( oldContained );
			containingAssociation.clear( indexed );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4305")
	public void indirectAssociationUpdate_indexedEmbedded() {
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbedded();
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
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 4 );
			field.set( containedEntity, "initialValue" );

			containingAssociation.set( containingEntity1, containedEntity );
			containedAssociation.set( containedEntity, containingEntity1 );

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
			field.set( containedEntity, "updatedValue" );

			TContained oldContained = containingAssociation.get( containingEntity1 );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( containingEntity1 );

				if ( setupHolder.areEntitiesProcessedInSession() ) {
					backendMock.expectWorks( _indexed().indexName() )
							.addOrUpdate( "1", b -> b
									.objectField( "child", b2 -> {
									} )
							);
				}
				session.flush();
			}

			containingAssociation.set( containingEntity1, containedEntity );
			containedAssociation.set( containedEntity, containingEntity1 );

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
			field.set( containedEntity, "outOfScopeValue" );

			containingAssociation.set( deeplyNestedContainingEntity1, containedEntity );
			containedAssociation.set( containedEntity, deeplyNestedContainingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained oldContained = containingAssociation.get( containingEntity1 );
			containedAssociation.clear( oldContained );
			containingAssociation.clear( containingEntity1 );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedNonIndexedEmbedded();
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
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 4 );
			field.set( containedEntity, "initialValue" );

			containingAssociation.set( containingEntity1, containedEntity );
			containedAssociation.set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 5 );
			field.set( containedEntity, "updatedValue" );

			TContained oldContained = containingAssociation.get( containingEntity1 );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( containingEntity1 );
				session.flush();
			}

			containingAssociation.set( containingEntity1, containedEntity );
			containedAssociation.set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained oldContained = containingAssociation.get( containingEntity1 );
			containedAssociation.clear( oldContained );
			containingAssociation.clear( containingEntity1 );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbeddedShallowReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbeddedShallowReindexOnUpdate();
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
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 4 );
			field.set( containedEntity, "initialValue" );

			containingAssociation.set( containingEntity1, containedEntity );
			containedAssociation.set( containedEntity, containingEntity1 );

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
			field.set( containedEntity, "updatedValue" );

			TContained oldContained = containingAssociation.get( containingEntity1 );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( containingEntity1 );

				if ( setupHolder.areEntitiesProcessedInSession() ) {
					backendMock.expectWorks( _indexed().indexName() )
							.addOrUpdate( "1", b -> b
									.objectField( "child", b2 -> { } )
							);
				}
				session.flush();
			}

			containingAssociation.set( containingEntity1, containedEntity );
			containedAssociation.set( containedEntity, containingEntity1 );

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

			containingAssociation.clear( containingEntity1 );
			containedAssociation.clear( containedEntity );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbeddedNoReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbeddedNoReindexOnUpdate();
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
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 4 );
			field.set( containedEntity, "initialValue" );

			containingAssociation.set( containingEntity1, containedEntity );
			containedAssociation.set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 5 );
			field.set( containedEntity, "updatedValue" );

			TContained oldContained = containingAssociation.get( containingEntity1 );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( containingEntity1 );
				session.flush();
			}

			containingAssociation.set( containingEntity1, containedEntity );
			containedAssociation.set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );
			TContained containedEntity = session.get( _contained().entityClass(), 5 );

			containingAssociation.clear( containingEntity1 );
			containedAssociation.clear( containedEntity );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedUsedInCrossEntityDerivedProperty();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsUsedInCrossEntityDerivedProperty();
		PropertyAccessor<TContained, String> field1 = _contained().fieldUsedInCrossEntityDerivedField1();
		PropertyAccessor<TContained, String> field2 = _contained().fieldUsedInCrossEntityDerivedField2();

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
			field1.set( containedEntity, "field1_initialValue" );
			field2.set( containedEntity, "field2_initialValue" );

			containingAssociation.set( containingEntity1, containedEntity );
			containedAssociation.set( containedEntity, containingEntity1 );

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
			field1.set( containedEntity, "field1_updatedValue" );
			field2.set( containedEntity, "field2_updatedValue" );

			TContained oldContained = containingAssociation.get( containingEntity1 );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( containingEntity1 );

				if ( setupHolder.areEntitiesProcessedInSession() ) {
					backendMock.expectWorks( _indexed().indexName() )
							.addOrUpdate( "1", b -> b
									.objectField( "child", b2 -> { } )
							);
				}
				session.flush();
			}

			containingAssociation.set( containingEntity1, containedEntity );
			containedAssociation.set( containedEntity, containingEntity1 );

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
			field1.set( containedEntity, "field1_outOfScopeValue" );
			field2.set( containedEntity, "field2_outOfScopeValue" );

			containingAssociation.set( deeplyNestedContainingEntity1, containedEntity );
			containedAssociation.set( containedEntity, deeplyNestedContainingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained oldContained = containingAssociation.get( containingEntity1 );
			containedAssociation.clear( oldContained );
			containingAssociation.clear( containingEntity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public void indirectAssociationUpdate_embeddedAssociationsIndexedEmbedded() {
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().embeddedAssociations()
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
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 4 );
			field.set( containedEntity, "initialValue" );

			containingAssociation.set( containingEntity1, containedEntity );
			containedAssociation.set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "embeddedAssociations", b3 -> b3
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", "initialValue" )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 5 );
			field.set( containedEntity, "updatedValue" );

			TContained oldContained = containingAssociation.get( containingEntity1 );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( containingEntity1 );

				if ( setupHolder.areEntitiesProcessedInSession() ) {
					backendMock.expectWorks( _indexed().indexName() )
							.addOrUpdate( "1", b -> b
									.objectField( "child", b2 -> { } )
							);
				}
				session.flush();
			}

			containingAssociation.set( containingEntity1, containedEntity );
			containedAssociation.set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "embeddedAssociations", b3 -> b3
										.objectField( "containedIndexedEmbedded", b4 -> b4
												.field( "indexedField", "updatedValue" )
										)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		setupHolder.runInTransaction( session -> {
			TContaining deeplyNestedContainingEntity1 = session.get( _containing().entityClass(), 3 );

			TContained containedEntity = _contained().newInstance( 6 );
			field.set( containedEntity, "outOfScopeValue" );

			containingAssociation.set( deeplyNestedContainingEntity1, containedEntity );
			containedAssociation.set( containedEntity, deeplyNestedContainingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained oldContained = containingAssociation.get( containingEntity1 );
			containedAssociation.clear( oldContained );
			containingAssociation.clear( containingEntity1 );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public void indirectAssociationUpdate_embeddedAssociationsNonIndexedEmbedded() {
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().embeddedAssociations()
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
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 4 );
			field.set( containedEntity, "initialValue" );

			containingAssociation.set( containingEntity1, containedEntity );
			containedAssociation.set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			if ( isAssociationOwnedByContainedSide() || isAssociationMultiValuedOnContainingSide() ) {
				// Do not expect any work
			}
			else {
				// HSEARCH-4718: we cannot distinguish between relevant and irrelevant properties
				// for changes within an embeddable.
				backendMock.expectWorks( _indexed().indexName() )
						.addOrUpdate( "1", b -> b
								.objectField( "child", b2 -> { } )
						);
			}
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 5 );
			field.set( containedEntity, "updatedValue" );

			TContained oldContained = containingAssociation.get( containingEntity1 );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( containingEntity1 );
				if ( isEmbeddedAssociationChangeCausingWork() && setupHolder.areEntitiesProcessedInSession() ) {
					backendMock.expectWorks( _indexed().indexName() )
							.addOrUpdate( "1", b ->
									b.objectField( "child", b2 -> { } ) );
				}
				session.flush();
			}

			containingAssociation.set( containingEntity1, containedEntity );
			containedAssociation.set( containedEntity, containingEntity1 );

			session.persist( containedEntity );

			if ( isAssociationOwnedByContainedSide() || isAssociationMultiValuedOnContainingSide() ) {
				// Do not expect any work
			}
			else {
				// HSEARCH-4718: we cannot distinguish between relevant and irrelevant properties
				// for changes within an embeddable.
				backendMock.expectWorks( _indexed().indexName() )
						.addOrUpdate( "1", b -> b
								.objectField( "child", b2 -> { } )
						);
			}
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained oldContained = containingAssociation.get( containingEntity1 );
			containedAssociation.clear( oldContained );
			containingAssociation.clear( containingEntity1 );

			if ( isAssociationOwnedByContainedSide() || isAssociationMultiValuedOnContainingSide() ) {
				// Do not expect any work
			}
			else {
				// HSEARCH-4718: we cannot distinguish between relevant and irrelevant properties
				// for changes within an embeddable.
				backendMock.expectWorks( _indexed().indexName() )
						.addOrUpdate( "1", b -> b
								.objectField( "child", b2 -> { } )
						);
			}
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public void indirectEmbeddedAssociationReplace_embeddedAssociationsIndexedEmbedded() {
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().embeddedAssociations()
				.andThen( _containingEmbeddable()::newInstance, _containingEmbeddable().containedIndexedEmbedded() );
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().embeddedAssociations()
				.andThen( _containedEmbeddable()::newInstance, _containedEmbeddable().containingAsIndexedEmbedded() );
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed indexed = _indexed().newInstance( 1 );

			TContaining containing = _containing().newInstance( 2 );
			_containing().child().set( indexed, containing );
			_containing().parent().set( containing, indexed );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, "initialValue" );

			containingAssociation.set( containing, contained );
			containedAssociation.set( contained, containing );

			session.persist( contained );
			session.persist( containing );
			session.persist( indexed );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "embeddedAssociations", b3 -> b3
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", "initialValue" )
											)
									) )
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, "updatedValue" );

			TContained oldContained = containingAssociation.get( containing );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( containing );

				if ( setupHolder.areEntitiesProcessedInSession() ) {
					backendMock.expectWorks( _indexed().indexName() )
							.addOrUpdate( "1", b -> b
									.objectField( "child", b2 -> { } )
							);
				}
				session.flush();
			}

			_containing().embeddedAssociations().set( containing, _containingEmbeddable().newInstance() );
			containingAssociation.set( containing, contained );
			containedAssociation.set( contained, containing );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "embeddedAssociations", b3 -> b3
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", "updatedValue" )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public void indirectEmbeddedAssociationReplace_embeddedAssociationsNonIndexedEmbedded() {
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().embeddedAssociations()
				.andThen( _containingEmbeddable()::newInstance, _containingEmbeddable().containedNonIndexedEmbedded() );
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().embeddedAssociations()
				.andThen( _containedEmbeddable()::newInstance, _containedEmbeddable().containingAsNonIndexedEmbedded() );
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed indexed = _indexed().newInstance( 1 );

			TContaining containing = _containing().newInstance( 2 );
			_containing().child().set( indexed, containing );
			_containing().parent().set( containing, indexed );

			session.persist( containing );
			session.persist( indexed );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, "initialValue" );

			containingAssociation.set( containing, contained );
			containedAssociation.set( contained, containing );

			session.persist( contained );
			session.persist( containing );

			if ( isAssociationOwnedByContainedSide() || isAssociationMultiValuedOnContainingSide() ) {
				// Do not expect any work
			}
			else {
				// For some reason we end up reindexing needlessly,
				// probably because we don't have enough information in the change events
				// (HSEARCH-3204: missing "role" for a replaced collection;
				// HSEARCH-4718: no information about which property changed within an embeddable,
				// ...)
				backendMock.expectWorks( _indexed().indexName() )
						.addOrUpdate( "1", b -> b
								.objectField( "child", b2 -> { } ) );
			}
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, "updatedValue" );

			TContained oldContained = containingAssociation.get( containing );
			containedAssociation.clear( oldContained );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			if ( !isAssociationMultiValuedOnContainingSide() ) {
				containingAssociation.clear( containing );
				if ( isEmbeddedAssociationChangeCausingWork() && setupHolder.areEntitiesProcessedInSession() ) {
					backendMock.expectWorks( _indexed().indexName() )
							.addOrUpdate( "1", b -> b
									.objectField( "child", b2 ->
											b2.objectField( "embeddedAssociations", b3 -> { } ) )
							);
				}
				session.flush();
			}

			_containing().embeddedAssociations().set( containing, _containingEmbeddable().newInstance() );
			containingAssociation.set( containing, contained );
			containedAssociation.set( contained, containing );

			session.persist( contained );

			if ( isAssociationOwnedByContainedSide() && !isAssociationMultiValuedOnContainingSide() ) {
				// Do not expect any work
			}
			else {
				// For some reason we end up reindexing needlessly,
				// probably because we don't have enough information in the change events
				// (HSEARCH-3204: missing "role" for a replaced collection;
				// HSEARCH-4718: no information about which property changed within an embeddable,
				// ...)
				backendMock.expectWorks( _indexed().indexName() )
						.addOrUpdate( "1", b -> b
								.objectField( "child", b2 -> { } ) );
			}
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public final void indirectElementCollectionAssociationUpdate_containingSideElementCollectionAssociationsIndexedEmbedded() {
		assumeElementCollectionAssociationsOnContainingSide();

		MultiValuedPropertyAccessor<TContaining, TContainingEmbeddable, List<TContainingEmbeddable>> elementCollectionAssociations =
				_containing().elementCollectionAssociations();
		PropertyAccessor<TContainingEmbeddable, TContained> containingAssociation = _containingEmbeddable().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsElementCollectionAssociationsIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed indexed = _indexed().newInstance( 1 );

			TContaining containing = _containing().newInstance( 2 );
			_containing().child().set( indexed, containing );
			_containing().parent().set( containing, indexed );

			session.persist( containing );
			session.persist( indexed );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			TContainingEmbeddable containingEmbeddable = _containingEmbeddable().newInstance();
			elementCollectionAssociations.add( containing, containingEmbeddable );

			containingAssociation.set( containingEmbeddable, contained );
			containedAssociation.set( contained, containing );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "elementCollectionAssociations", b3 -> b3
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", VALUE_1 )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			TContainingEmbeddable containingEmbeddable = _containingEmbeddable().newInstance();
			elementCollectionAssociations.add( containing, containingEmbeddable );

			containingAssociation.set( containingEmbeddable, contained );
			containedAssociation.set( contained, containing );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "elementCollectionAssociations", b3 -> b3
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", VALUE_1 )
											)
									)
									.objectField( "elementCollectionAssociations", b3 -> b3
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", VALUE_2 )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_3 );

			TContainingEmbeddable containingEmbeddable = elementCollectionAssociations.getContainer( containing ).get( 1 );

			TContained oldContained = containingAssociation.get( containingEmbeddable );
			containedAssociation.clear( oldContained );
			containingAssociation.set( containingEmbeddable, contained );
			containedAssociation.set( contained, containing );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "elementCollectionAssociations", b3 -> b3
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", VALUE_1 )
											)
									)
									.objectField( "elementCollectionAssociations", b3 -> b3
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", VALUE_3 )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing an embeddable
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 5 );
			field.set( contained, VALUE_4 );

			TContainingEmbeddable oldContainingEmbeddable = elementCollectionAssociations.getContainer( containing ).remove( 1 );
			TContained oldContained = containingAssociation.get( oldContainingEmbeddable );
			containedAssociation.clear( oldContained );

			TContainingEmbeddable containingEmbeddable = _containingEmbeddable().newInstance();
			elementCollectionAssociations.add( containing, containingEmbeddable );

			containingAssociation.set( containingEmbeddable, contained );
			containedAssociation.set( contained, containing );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "elementCollectionAssociations", b3 -> b3
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", VALUE_1 )
											)
									)
									.objectField( "elementCollectionAssociations", b3 -> b3
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", VALUE_4 )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing an embeddable
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContainingEmbeddable oldContainingEmbeddable = elementCollectionAssociations.getContainer( containing ).remove( 1 );
			TContained oldContained = containingAssociation.get( oldContainingEmbeddable );
			containedAssociation.clear( oldContained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "elementCollectionAssociations", b3 -> b3
											.objectField( "containedIndexedEmbedded", b4 -> b4
													.field( "indexedField", VALUE_1 )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContainingEmbeddable oldContainingEmbeddable = elementCollectionAssociations.get( containing );
			TContained oldContained = containingAssociation.get( oldContainingEmbeddable );
			containedAssociation.clear( oldContained );
			containingAssociation.clear( oldContainingEmbeddable );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "elementCollectionAssociations", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public final void indirectElementCollectionAssociationUpdate_containingSideElementCollectionAssociationsNonIndexedEmbedded() {
		assumeElementCollectionAssociationsOnContainingSide();

		MultiValuedPropertyAccessor<TContaining, TContainingEmbeddable, List<TContainingEmbeddable>> elementCollectionAssociations =
				_containing().elementCollectionAssociations();
		PropertyAccessor<TContainingEmbeddable, TContained> containingAssociation = _containingEmbeddable().containedNonIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsElementCollectionAssociationsNonIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed indexed = _indexed().newInstance( 1 );

			TContaining containing = _containing().newInstance( 2 );
			_containing().child().set( indexed, containing );
			_containing().parent().set( containing, indexed );

			session.persist( containing );
			session.persist( indexed );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			TContainingEmbeddable containingEmbeddable = _containingEmbeddable().newInstance();
			elementCollectionAssociations.add( containing, containingEmbeddable );

			containingAssociation.set( containingEmbeddable, contained );
			containedAssociation.set( contained, containing );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "elementCollectionAssociations", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			TContainingEmbeddable containingEmbeddable = _containingEmbeddable().newInstance();
			elementCollectionAssociations.add( containing, containingEmbeddable );

			containingAssociation.set( containingEmbeddable, contained );
			containedAssociation.set( contained, containing );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "elementCollectionAssociations", b3 -> { } )
									.objectField( "elementCollectionAssociations", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_3 );

			TContainingEmbeddable containingEmbeddable = elementCollectionAssociations.getContainer( containing ).get( 1 );

			TContained oldContained = containingAssociation.get( containingEmbeddable );
			containedAssociation.clear( oldContained );
			containingAssociation.set( containingEmbeddable, contained );
			containedAssociation.set( contained, containing );

			session.persist( contained );

			// For some reason we end up reindexing needlessly,
			// probably because we don't have enough information in the change events
			// (HSEARCH-3204: missing "role" for a replaced collection;
			// HSEARCH-4718: no information about which property changed within an embeddable,
			// ...)
			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "elementCollectionAssociations", b3 -> { } )
									.objectField( "elementCollectionAssociations", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing an embeddable
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 5 );
			field.set( contained, VALUE_4 );

			TContainingEmbeddable oldContainingEmbeddable = elementCollectionAssociations.getContainer( containing ).remove( 1 );
			TContained oldContained = containingAssociation.get( oldContainingEmbeddable );
			containedAssociation.clear( oldContained );

			TContainingEmbeddable containingEmbeddable = _containingEmbeddable().newInstance();
			elementCollectionAssociations.add( containing, containingEmbeddable );

			containingAssociation.set( containingEmbeddable, contained );
			containedAssociation.set( contained, containing );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "elementCollectionAssociations", b3 -> { } )
									.objectField( "elementCollectionAssociations", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing an embeddable
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContainingEmbeddable oldContainingEmbeddable = elementCollectionAssociations.getContainer( containing ).remove( 1 );
			TContained oldContained = containingAssociation.get( oldContainingEmbeddable );
			containedAssociation.clear( oldContained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "elementCollectionAssociations", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContainingEmbeddable oldContainingEmbeddable = elementCollectionAssociations.get( containing );
			TContained oldContained = containingAssociation.get( oldContainingEmbeddable );
			containedAssociation.clear( oldContained );
			containingAssociation.clear( oldContainingEmbeddable );

			// For some reason we end up reindexing needlessly,
			// probably because we don't have enough information in the change events
			// (HSEARCH-3204: missing "role" for a replaced collection;
			// HSEARCH-4718: no information about which property changed within an embeddable,
			// ...)
			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "elementCollectionAssociations", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public final void indirectElementCollectionAssociationUpdate_containedSideElementCollectionAssociationsIndexedEmbedded() {
		assumeElementCollectionAssociationsOnContainedSide();

		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedElementCollectionAssociationsIndexedEmbedded();
		MultiValuedPropertyAccessor<TContained, TContainedEmbeddable, List<TContainedEmbeddable>> elementCollectionAssociations =
				_contained().elementCollectionAssociations();
		PropertyAccessor<TContainedEmbeddable, TContaining> containedAssociation = _containedEmbeddable().containingAsIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed indexed = _indexed().newInstance( 1 );

			TContaining containing = _containing().newInstance( 2 );
			_containing().child().set( indexed, containing );
			_containing().parent().set( containing, indexed );

			session.persist( containing );
			session.persist( indexed );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } ) );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			TContainedEmbeddable containedEmbeddable = _containedEmbeddable().newInstance();
			elementCollectionAssociations.add( contained, containedEmbeddable );

			containingAssociation.set( containing, contained );
			containedAssociation.set( containedEmbeddable, containing );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedElementCollectionAssociationsIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value, clearing the association on the contained side
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained oldContained = containingAssociation.get( containing );
			TContainedEmbeddable oldContainedEmbeddable = elementCollectionAssociations.get( oldContained );
			containedAssociation.clear( oldContainedEmbeddable );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			TContainedEmbeddable containedEmbeddable = _containedEmbeddable().newInstance();
			elementCollectionAssociations.add( contained, containedEmbeddable );

			containingAssociation.set( containing, contained );
			containedAssociation.set( containedEmbeddable, containing );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedElementCollectionAssociationsIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_2 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value, clearing the element collection on the contained side
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained oldContained = containingAssociation.get( containing );
			elementCollectionAssociations.clear( oldContained );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_3 );

			TContainedEmbeddable containedEmbeddable = _containedEmbeddable().newInstance();
			elementCollectionAssociations.add( contained, containedEmbeddable );

			containingAssociation.set( containing, contained );
			containedAssociation.set( containedEmbeddable, containing );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedElementCollectionAssociationsIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_3 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value, clearing the association on the contained side
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained oldContained = containingAssociation.get( containing );
			TContainedEmbeddable oldContainedEmbeddable = elementCollectionAssociations.get( oldContained );
			containedAssociation.clear( oldContainedEmbeddable );
			containingAssociation.clear( containing );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Add back a value, just to remove it in the next test
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 5 );
			field.set( contained, VALUE_1 );

			TContainedEmbeddable containedEmbeddable = _containedEmbeddable().newInstance();
			elementCollectionAssociations.add( contained, containedEmbeddable );

			containingAssociation.set( containing, contained );
			containedAssociation.set( containedEmbeddable, containing );

			session.persist( contained );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedElementCollectionAssociationsIndexedEmbedded", b3 -> b3
											.field( "indexedField", VALUE_1 )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value, clearing the element collection on the contained side
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained oldContained = containingAssociation.get( containing );
			elementCollectionAssociations.clear( oldContained );
			containingAssociation.clear( containing );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> { } )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4708")
	public final void indirectElementCollectionAssociationUpdate_containedSideElementCollectionAssociationsNonIndexedEmbedded() {
		assumeElementCollectionAssociationsOnContainedSide();

		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedElementCollectionAssociationsNonIndexedEmbedded();
		MultiValuedPropertyAccessor<TContained, TContainedEmbeddable, List<TContainedEmbeddable>> elementCollectionAssociations =
				_contained().elementCollectionAssociations();
		PropertyAccessor<TContainedEmbeddable, TContaining> containedAssociation = _containedEmbeddable().containingAsNonIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed indexed = _indexed().newInstance( 1 );

			TContaining containing = _containing().newInstance( 2 );
			_containing().child().set( indexed, containing );
			_containing().parent().set( containing, indexed );

			session.persist( containing );
			session.persist( indexed );

			backendMock.expectWorks( _indexed().indexName() )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } ) );
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 2 );
			field.set( contained, VALUE_1 );

			TContainedEmbeddable containedEmbeddable = _containedEmbeddable().newInstance();
			elementCollectionAssociations.add( contained, containedEmbeddable );

			containingAssociation.set( containing, contained );
			containedAssociation.set( containedEmbeddable, containing );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value, clearing the association on the contained side
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained oldContained = containingAssociation.get( containing );
			TContainedEmbeddable oldContainedEmbeddable = elementCollectionAssociations.get( oldContained );
			containedAssociation.clear( oldContainedEmbeddable );

			TContained contained = _contained().newInstance( 3 );
			field.set( contained, VALUE_2 );

			TContainedEmbeddable containedEmbeddable = _containedEmbeddable().newInstance();
			elementCollectionAssociations.add( contained, containedEmbeddable );

			containingAssociation.set( containing, contained );
			containedAssociation.set( containedEmbeddable, containing );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value, clearing the element collection on the contained side
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained oldContained = containingAssociation.get( containing );
			elementCollectionAssociations.clear( oldContained );

			TContained contained = _contained().newInstance( 4 );
			field.set( contained, VALUE_3 );

			TContainedEmbeddable containedEmbeddable = _containedEmbeddable().newInstance();
			elementCollectionAssociations.add( contained, containedEmbeddable );

			containingAssociation.set( containing, contained );
			containedAssociation.set( containedEmbeddable, containing );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value, clearing the association on the contained side
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained oldContained = containingAssociation.get( containing );
			TContainedEmbeddable oldContainedEmbeddable = elementCollectionAssociations.get( oldContained );
			containedAssociation.clear( oldContainedEmbeddable );
			containingAssociation.clear( containing );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Add back a value, just to remove it in the next test
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained contained = _contained().newInstance( 5 );
			field.set( contained, VALUE_1 );

			TContainedEmbeddable containedEmbeddable = _containedEmbeddable().newInstance();
			elementCollectionAssociations.add( contained, containedEmbeddable );

			containingAssociation.set( containing, contained );
			containedAssociation.set( containedEmbeddable, containing );

			session.persist( contained );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value, clearing the element collection on the contained side
		setupHolder.runInTransaction( session -> {
			TContaining containing = session.get( _containing().entityClass(), 2 );

			TContained oldContained = containingAssociation.get( containing );
			elementCollectionAssociations.clear( oldContained );
			containingAssociation.clear( containing );

			// Do not expect any work
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

		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
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
							.objectField( "child", b2 -> { } ) );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			TContaining containingEntity1 = session.get( _containing().entityClass(), 2 );

			TContained containedEntity = _contained().newInstance( 2 );
			field.set( containedEntity, "initialValue" );

			// Do NOT set the association on the containing side; that's on purpose.
			// Only set it on the contained side.
			containedAssociation.set( containedEntity, containingEntity1 );

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

		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );
			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );
			TContained containedEntity = _contained().newInstance( 2 );
			field.set( containedEntity, "initialValue" );

			session.persist( containingEntity1 );
			session.persist( entity1 );
			session.persist( containedEntity );

			containingAssociation.set( containingEntity1, containedEntity );
			containedAssociation.set( containedEntity, containingEntity1 );

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
			containedAssociation.get( containedEntity );

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

		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );
			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );
			TContained containedEntity = _contained().newInstance( 2 );
			field.set( containedEntity, "initialValue" );

			session.persist( containingEntity1 );
			session.persist( entity1 );
			session.persist( containedEntity );

			containingAssociation.set( containingEntity1, containedEntity );
			containedAssociation.set( containedEntity, containingEntity1 );

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
			TContained containedEntity = containingAssociation.get( containing );

			// Do NOT update the association on either side; that's on purpose.
			// But DO force loading on contained side:
			// if the association is lazy and unloaded, it won't be loadable after the deletion
			// and the deletion even will simply be ignored.
			containedAssociation.get( containedEntity );

			session.remove( containedEntity );

			backendMock.expectWorks( _indexed().indexName() )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> { } ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_indexedEmbedded_singleValue_indexed() {
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbedded();
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

			TContained contained1 = _contained().newInstance( 4 );
			field.set( contained1, "initialValue" );
			containingAssociation.set( containingEntity1, contained1 );
			containedAssociation.set( contained1, containingEntity1 );

			TContained contained2 = _contained().newInstance( 5 );
			field.set( contained2, "initialOutOfScopeValue" );
			containingAssociation.set( deeplyNestedContainingEntity, contained2 );
			containedAssociation.set( contained2, deeplyNestedContainingEntity );

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
			field.set( contained, "updatedValue" );

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
			field.set( contained, "updatedOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4137")
	public void directValueUpdate_nonIndexed_then_indirectValueUpdate_indexedEmbedded_singleValue_indexed() {
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		PropertyAccessor<TContaining, String> nonIndexedField = _containing().nonIndexedField();
		PropertyAccessor<TContained, String> indexedField = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );
			nonIndexedField.set( entity1, "initialValue" );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			indexedField.set( contained1, "initialValue" );
			containingAssociation.set( containingEntity1, contained1 );
			containedAssociation.set( contained1, containingEntity1 );

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
			nonIndexedField.set( indexed, "updatedValue" );
			TContained contained = session.get( _contained().entityClass(), 4 );
			indexedField.set( contained, "updatedValue" );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		PropertyAccessor<TContained, String> field = _contained().nonIndexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			field.set( contained1, "initialValue" );
			containingAssociation.set( containingEntity1, contained1 );
			containedAssociation.set( contained1, containingEntity1 );

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
			field.set( contained, "updatedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_indexedEmbedded_elementCollectionValue_indexed() {
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		MultiValuedPropertyAccessor<TContained, String, List<String>> field = _contained().indexedElementCollectionField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = _containing().newInstance( 3 );
			_containing().child().set( containingEntity1, deeplyNestedContainingEntity );
			_containing().parent().set( deeplyNestedContainingEntity, containingEntity1 );

			TContained contained1 = _contained().newInstance( 4 );
			field.add( contained1, "firstValue" );
			containingAssociation.set( containingEntity1, contained1 );
			containedAssociation.set( contained1, containingEntity1 );

			TContained contained2 = _contained().newInstance( 5 );
			field.add( contained2, "firstOutOfScopeValue" );
			containingAssociation.set( deeplyNestedContainingEntity, contained2 );
			containedAssociation.set( contained2, deeplyNestedContainingEntity );

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
			field.add( contained, "secondValue" );

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
			field.remove( contained, "firstValue" );

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
			field.add( contained, "secondOutOfScopeValue" );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		MultiValuedPropertyAccessor<TContained, String, List<String>> field = _contained().indexedElementCollectionField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = _containing().newInstance( 3 );
			_containing().child().set( containingEntity1, deeplyNestedContainingEntity );
			_containing().parent().set( deeplyNestedContainingEntity, containingEntity1 );

			TContained contained1 = _contained().newInstance( 4 );
			field.add( contained1, "firstValue" );
			containingAssociation.set( containingEntity1, contained1 );
			containedAssociation.set( contained1, containingEntity1 );

			TContained contained2 = _contained().newInstance( 5 );
			field.add( contained2, "firstOutOfScopeValue" );
			containingAssociation.set( deeplyNestedContainingEntity, contained2 );
			containedAssociation.set( contained2, deeplyNestedContainingEntity );

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
			field.setContainer( contained, new ArrayList<>( Arrays.asList(
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
			field.setContainer( contained, new ArrayList<>( Arrays.asList(
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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		MultiValuedPropertyAccessor<TContained, String, List<String>> field = _contained().nonIndexedElementCollectionField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			field.add( contained1, "firstValue" );
			containingAssociation.set( containingEntity1, contained1 );
			containedAssociation.set( contained1, containingEntity1 );

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
			field.add( contained, "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			field.remove( contained, "firstValue" );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		MultiValuedPropertyAccessor<TContained, String, List<String>> field = _contained().nonIndexedElementCollectionField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			field.add( contained1, "firstValue" );
			containingAssociation.set( containingEntity1, contained1 );
			containedAssociation.set( contained1, containingEntity1 );

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
			field.setContainer( contained, new ArrayList<>( Arrays.asList(
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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbedded();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbedded();
		PropertyAccessor<TContained, String> field1 = _contained().fieldUsedInContainedDerivedField1();
		PropertyAccessor<TContained, String> field2 = _contained().fieldUsedInContainedDerivedField2();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			field1.set( contained1, "field1_initialValue" );
			field2.set( contained1, "field2_initialValue" );
			containingAssociation.set( containingEntity1, contained1 );
			containedAssociation.set( contained1, containingEntity1 );

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
			field1.set( contained, "field1_updatedValue" );

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
			field2.set( contained, "field2_updatedValue" );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedUsedInCrossEntityDerivedProperty();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsUsedInCrossEntityDerivedProperty();
		PropertyAccessor<TContained, String> field1 = _contained().fieldUsedInCrossEntityDerivedField1();
		PropertyAccessor<TContained, String> field2 = _contained().fieldUsedInCrossEntityDerivedField2();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			field1.set( contained1, "field1_initialValue" );
			field2.set( contained1, "field2_initialValue" );
			containingAssociation.set( containingEntity1, contained1 );
			containedAssociation.set( contained1, containingEntity1 );

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
			field1.set( contained, "field1_updatedValue" );

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
			field2.set( contained, "field2_updatedValue" );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbeddedShallowReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbeddedShallowReindexOnUpdate();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			field.set( contained1, "initialValue" );
			containingAssociation.set( containingEntity1, contained1 );
			containedAssociation.set( contained1, containingEntity1 );

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
			field.set( contained, "updatedValue" );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbeddedShallowReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbeddedShallowReindexOnUpdate();
		MultiValuedPropertyAccessor<TContained, String, List<String>> field = _contained().indexedElementCollectionField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			field.add( contained1, "firstValue" );
			containingAssociation.set( containingEntity1, contained1 );
			containedAssociation.set( contained1, containingEntity1 );

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
			field.add( contained, "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			field.remove( contained, "firstValue" );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbeddedShallowReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbeddedShallowReindexOnUpdate();
		MultiValuedPropertyAccessor<TContained, String, List<String>> field = _contained().indexedElementCollectionField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			field.add( contained1, "firstValue" );
			containingAssociation.set( containingEntity1, contained1 );
			containedAssociation.set( contained1, containingEntity1 );

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
			field.setContainer( contained, new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3206")
	public void indirectValueUpdate_indexedEmbeddedNoReindexOnUpdate_singleValue_indexed() {
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbeddedNoReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbeddedNoReindexOnUpdate();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			field.set( contained1, "initialValue" );
			containingAssociation.set( containingEntity1, contained1 );
			containedAssociation.set( contained1, containingEntity1 );

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
			field.set( contained, "updatedValue" );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbeddedNoReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbeddedNoReindexOnUpdate();
		MultiValuedPropertyAccessor<TContained, String, List<String>> field = _contained().indexedElementCollectionField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			field.add( contained1, "firstValue" );
			containingAssociation.set( containingEntity1, contained1 );
			containedAssociation.set( contained1, containingEntity1 );

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
			field.add( contained, "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			TContained contained = session.get( _contained().entityClass(), 4 );
			field.remove( contained, "firstValue" );

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
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbeddedNoReindexOnUpdate();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbeddedNoReindexOnUpdate();
		MultiValuedPropertyAccessor<TContained, String, List<String>> field = _contained().indexedElementCollectionField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContained contained1 = _contained().newInstance( 4 );
			field.add( contained1, "firstValue" );
			containingAssociation.set( containingEntity1, contained1 );
			containedAssociation.set( contained1, containingEntity1 );

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
			field.setContainer( contained, new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3072")
	public void indirectValueUpdate_indexedEmbeddedWithCast_singleValue() {
		PropertyAccessor<TContaining, TContained> containingAssociation = _containing().containedIndexedEmbeddedWithCast();
		PropertyAccessor<TContained, TContaining> containedAssociation = _contained().containingAsIndexedEmbeddedWithCast();
		PropertyAccessor<TContained, String> field = _contained().indexedField();

		setupHolder.runInTransaction( session -> {
			TIndexed entity1 = _indexed().newInstance( 1 );

			TContaining containingEntity1 = _containing().newInstance( 2 );
			_containing().child().set( entity1, containingEntity1 );
			_containing().parent().set( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = _containing().newInstance( 3 );
			_containing().child().set( containingEntity1, deeplyNestedContainingEntity );
			_containing().parent().set( deeplyNestedContainingEntity, containingEntity1 );

			TContained contained1 = _contained().newInstance( 4 );
			field.set( contained1, "initialValue" );
			containingAssociation.set( containingEntity1, contained1 );
			containedAssociation.set( contained1, containingEntity1 );

			TContained contained2 = _contained().newInstance( 5 );
			field.set( contained2, "initialOutOfScopeValue" );
			containingAssociation.set( deeplyNestedContainingEntity, contained2 );
			containedAssociation.set( contained2, deeplyNestedContainingEntity );

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
			field.set( contained, "updatedValue" );

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
			field.set( contained, "updatedOutOfScopeValue" );

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

	protected interface ContainingEntityPrimitives<TContaining, TContainingEmbeddable, TContained> {

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

		PropertyAccessor<TContaining, TContainingEmbeddable> embeddedAssociations();

		default MultiValuedPropertyAccessor<TContaining, TContainingEmbeddable, List<TContainingEmbeddable>> elementCollectionAssociations() {
			throw primitiveNotSupported();
		}

		default PropertyAccessor<TContaining, TContained> containedElementCollectionAssociationsIndexedEmbedded() {
			throw primitiveNotSupported();
		}

		default PropertyAccessor<TContaining, TContained> containedElementCollectionAssociationsNonIndexedEmbedded() {
			throw primitiveNotSupported();
		}

	}

	protected interface ContainingEmbeddablePrimitives<TContainingEmbeddable, TContained> {

		TContainingEmbeddable newInstance();

		PropertyAccessor<TContainingEmbeddable, TContained> containedIndexedEmbedded();

		PropertyAccessor<TContainingEmbeddable, TContained> containedNonIndexedEmbedded();

	}

	protected interface ContainedEntityPrimitives<TContained, TContainedEmbeddable, TContaining> {

		Class<TContained> entityClass();

		TContained newInstance(int id);

		PropertyAccessor<TContained, TContaining> containingAsIndexedEmbedded();

		PropertyAccessor<TContained, TContaining> containingAsNonIndexedEmbedded();

		PropertyAccessor<TContained, TContaining> containingAsIndexedEmbeddedShallowReindexOnUpdate();

		PropertyAccessor<TContained, TContaining> containingAsIndexedEmbeddedNoReindexOnUpdate();

		PropertyAccessor<TContained, TContaining> containingAsUsedInCrossEntityDerivedProperty();

		PropertyAccessor<TContained, TContaining> containingAsIndexedEmbeddedWithCast();

		PropertyAccessor<TContained, TContainedEmbeddable> embeddedAssociations();

		default PropertyAccessor<TContained, TContaining> containingAsElementCollectionAssociationsIndexedEmbedded() {
			throw primitiveNotSupported();
		}

		default PropertyAccessor<TContained, TContaining> containingAsElementCollectionAssociationsNonIndexedEmbedded() {
			throw primitiveNotSupported();
		}

		default MultiValuedPropertyAccessor<TContained, TContainedEmbeddable, List<TContainedEmbeddable>> elementCollectionAssociations() {
			throw primitiveNotSupported();
		}

		PropertyAccessor<TContained, String> indexedField();

		PropertyAccessor<TContained, String> nonIndexedField();

		MultiValuedPropertyAccessor<TContained, String, List<String>> indexedElementCollectionField();

		MultiValuedPropertyAccessor<TContained, String, List<String>> nonIndexedElementCollectionField();

		PropertyAccessor<TContained, String> fieldUsedInContainedDerivedField1();

		PropertyAccessor<TContained, String> fieldUsedInContainedDerivedField2();

		PropertyAccessor<TContained, String> fieldUsedInCrossEntityDerivedField1();

		PropertyAccessor<TContained, String> fieldUsedInCrossEntityDerivedField2();

	}

	protected interface ContainedEmbeddablePrimitives<TContainedEmbeddable, TContaining> {

		TContainedEmbeddable newInstance();

		PropertyAccessor<TContainedEmbeddable, TContaining> containingAsIndexedEmbedded();

		PropertyAccessor<TContainedEmbeddable, TContaining> containingAsNonIndexedEmbedded();

	}
}
