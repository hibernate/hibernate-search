/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.SessionFactory;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	protected SessionFactory sessionFactory;

	private final ModelPrimitives<TIndexed, TContaining, TContained> primitives;

	public AbstractAutomaticIndexingAssociationBaseIT(
			ModelPrimitives<TIndexed, TContaining, TContained> primitives) {
		this.primitives = primitives;
	}

	@Before
	public void setup() {
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

		sessionFactory = ormSetupHelper.start()
				.setup( primitives.getIndexedClass(), primitives.getContainedClass() );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_indexedEmbedded_singleValue_indexed() {
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 5 );
			primitives.indexedField().set( contained, "updatedOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4137")
	public void directValueUpdate_nonIndexed_then_indirectValueUpdate_indexedEmbedded_singleValue_indexed() {
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.nonIndexedField().set( contained, "updatedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_indexedEmbedded_elementCollectionValue_indexed() {
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.nonIndexedElementCollectionField().add( contained, "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedField().set( contained1, "initialValue" );
			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().set( containingEntity1, contained1 );

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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedElementCollectionField().add( contained1, "firstValue" );
			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().set( containingEntity1, contained1 );

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
		withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.indexedElementCollectionField().add( contained, "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedElementCollectionField().add( contained1, "firstValue" );
			primitives.containedIndexedEmbeddedShallowReindexOnUpdate().set( containingEntity1, contained1 );

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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedField().set( contained1, "initialValue" );
			primitives.containedIndexedEmbeddedNoReindexOnUpdate().set( containingEntity1, contained1 );

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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedElementCollectionField().add( contained1, "firstValue" );
			primitives.containedIndexedEmbeddedNoReindexOnUpdate().set( containingEntity1, contained1 );

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
		withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.indexedElementCollectionField().add( contained, "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.child().set( entity1, containingEntity1 );
			primitives.parent().set( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.indexedElementCollectionField().add( contained1, "firstValue" );
			primitives.containedIndexedEmbeddedNoReindexOnUpdate().set( containingEntity1, contained1 );

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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
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

		PropertyAccessor<TContaining, TContained> containedIndexedEmbeddedShallowReindexOnUpdate();

		PropertyAccessor<TContaining, TContained> containedIndexedEmbeddedNoReindexOnUpdate();

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
