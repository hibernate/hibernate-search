/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

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
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * An abstract base for tests dealing with automatic indexing based on Hibernate ORM entity events
 * and involving an association.
 * <p>
 * We use a contrived design based on a {@link AssociationModelPrimitives} class that defines all the factory methods,
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
 *             <li>
 *                 indirectAssociationReplace: replace the entire value of a collection
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
public abstract class AbstractAutomaticIndexingAssociationIT<
		TIndexed extends TContaining, TContaining, TContained
		> {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	protected SessionFactory sessionFactory;

	private final AssociationModelPrimitives<TIndexed, TContaining, TContained> primitives;

	AbstractAutomaticIndexingAssociationIT(
			AssociationModelPrimitives<TIndexed, TContaining, TContained> primitives) {
		this.primitives = primitives;
	}

	@Before
	public void setup() {
		Consumer<StubIndexSchemaNode.Builder> associationFieldContributor = b -> {
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
				.objectField( "containedIndexedEmbeddedNoReindexOnUpdate",
						associationFieldContributor.andThen( b2 -> b2
								.field( "indexedField", String.class )
								.field( "indexedElementCollectionField", String.class, b3 -> b3.multiValued( true ) )
								.field( "containedDerivedField", String.class )
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
						.objectField( "containedIndexedEmbeddedNoReindexOnUpdate",
								associationFieldContributor.andThen( b2 -> b2
										.field( "indexedField", String.class )
										.field( "indexedElementCollectionField", String.class, b4 -> b4.multiValued( true ) )
										.field( "containedDerivedField", String.class )
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
			primitives.setContainedIndexedEmbeddedSingle( containingEntity1, contained1 );
			primitives.setContainingAsIndexedEmbeddedSingle( contained1, containingEntity1 );

			TContained contained2 = primitives.newContained( 5 );
			primitives.setIndexedField( contained2, "initialOutOfScopeValue" );
			primitives.setContainedIndexedEmbeddedSingle( deeplyNestedContainingEntity, contained2 );
			primitives.setContainingAsIndexedEmbeddedSingle( contained2, deeplyNestedContainingEntity );

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

	/**
	 * Test that updating a non-indexed, basic property in an entity
	 * whose properties are otherwise used in an IndexedEmbedded from an indexed entity
	 * does not trigger reindexing of the indexed entity.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void indirectValueUpdate_indexedEmbedded_singleValue_nonIndexed() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.setNonIndexedField( contained1, "initialValue" );
			primitives.setContainedIndexedEmbeddedSingle( containingEntity1, contained1 );
			primitives.setContainingAsIndexedEmbeddedSingle( contained1, containingEntity1 );

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
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.setNonIndexedField( contained, "updatedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_indexedEmbedded_elementCollectionValue_indexed() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = primitives.newContaining( 3 );
			primitives.setChild( containingEntity1, deeplyNestedContainingEntity );
			primitives.setParent( deeplyNestedContainingEntity, containingEntity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.getIndexedElementCollectionField( contained1 ).add( "firstValue" );
			primitives.setContainedIndexedEmbeddedSingle( containingEntity1, contained1 );
			primitives.setContainingAsIndexedEmbeddedSingle( contained1, containingEntity1 );

			TContained contained2 = primitives.newContained( 5 );
			primitives.getIndexedElementCollectionField( contained2 ).add( "firstOutOfScopeValue" );
			primitives.setContainedIndexedEmbeddedSingle( deeplyNestedContainingEntity, contained2 );
			primitives.setContainingAsIndexedEmbeddedSingle( contained2, deeplyNestedContainingEntity );

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
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.getIndexedElementCollectionField( contained ).add( "secondValue" );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
											.field(
													"indexedElementCollectionField",
													"firstValue", "secondValue"
											)
									)
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.getIndexedElementCollectionField( contained ).remove( 0 );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
											.field(
													"indexedElementCollectionField",
													"secondValue"
											)
									)
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 5 );
			primitives.getIndexedElementCollectionField( contained ).add( "secondOutOfScopeValue" );

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
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			TContaining deeplyNestedContainingEntity = primitives.newContaining( 3 );
			primitives.setChild( containingEntity1, deeplyNestedContainingEntity );
			primitives.setParent( deeplyNestedContainingEntity, containingEntity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.getIndexedElementCollectionField( contained1 ).add( "firstValue" );
			primitives.setContainedIndexedEmbeddedSingle( containingEntity1, contained1 );
			primitives.setContainingAsIndexedEmbeddedSingle( contained1, containingEntity1 );

			TContained contained2 = primitives.newContained( 5 );
			primitives.getIndexedElementCollectionField( contained2 ).add( "firstOutOfScopeValue" );
			primitives.setContainedIndexedEmbeddedSingle( deeplyNestedContainingEntity, contained2 );
			primitives.setContainingAsIndexedEmbeddedSingle( contained2, deeplyNestedContainingEntity );

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
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.setIndexedElementCollectionField( contained, new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
											.field(
													"indexedElementCollectionField",
													"newFirstValue", "newSecondValue"
											)
									)
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 5 );
			primitives.setIndexedElementCollectionField( contained, new ArrayList<>( Arrays.asList(
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
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.getNonIndexedElementCollectionField( contained1 ).add( "firstValue" );
			primitives.setContainedIndexedEmbeddedSingle( containingEntity1, contained1 );
			primitives.setContainingAsIndexedEmbeddedSingle( contained1, containingEntity1 );

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
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.getNonIndexedElementCollectionField( contained ).add( "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.getNonIndexedElementCollectionField( contained ).remove( 0 );

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
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.getNonIndexedElementCollectionField( contained1 ).add( "firstValue" );
			primitives.setContainedIndexedEmbeddedSingle( containingEntity1, contained1 );
			primitives.setContainingAsIndexedEmbeddedSingle( contained1, containingEntity1 );

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
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing the values
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.setNonIndexedElementCollectionField( contained, new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
									)
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_indexedEmbedded_containedDerivedValue_indexed() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.setFieldUsedInContainedDerivedField1( contained1, "field1_initialValue" );
			primitives.setFieldUsedInContainedDerivedField2( contained1, "field2_initialValue" );
			primitives.setContainedIndexedEmbeddedSingle( containingEntity1, contained1 );
			primitives.setContainingAsIndexedEmbeddedSingle( contained1, containingEntity1 );

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
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating one value the field depends on
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.setFieldUsedInContainedDerivedField1( contained, "field1_updatedValue" );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
											.field(
													"containedDerivedField",
													"field1_updatedValue field2_initialValue"
											)
									)
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the other value the field depends on
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.setFieldUsedInContainedDerivedField2( contained, "field2_updatedValue" );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedIndexedEmbedded", b3 -> b3
											.field( "indexedField", null )
											.field(
													"containedDerivedField",
													"field1_updatedValue field2_updatedValue"
											)
									)
							)
					)
					.processedThenExecuted();
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
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.setFieldUsedInCrossEntityDerivedField1( contained1, "field1_initialValue" );
			primitives.setFieldUsedInCrossEntityDerivedField2( contained1, "field2_initialValue" );
			primitives.setContainedUsedInCrossEntityDerivedPropertySingle( containingEntity1, contained1 );
			primitives.setContainingAsUsedInCrossEntityDerivedPropertySingle( contained1, containingEntity1 );

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
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating one value the field depends on
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.setFieldUsedInCrossEntityDerivedField1( contained, "field1_updatedValue" );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.field(
											"crossEntityDerivedField",
											"field1_updatedValue field2_initialValue"
									)
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the other value the field depends on
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.setFieldUsedInCrossEntityDerivedField2( contained, "field2_updatedValue" );

			backendMock.expectWorks( primitives.getIndexName() )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.field(
											"crossEntityDerivedField",
											"field1_updatedValue field2_updatedValue"
									)
							)
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3206")
	public void indirectValueUpdate_indexedEmbeddedNoReindexOnUpdate_singleValue_indexed() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.setIndexedField( contained1, "initialValue" );
			primitives.setContainedIndexedEmbeddedNoReindexOnUpdateSingle( containingEntity1, contained1 );
			primitives.setContainingAsIndexedEmbeddedNoReindexOnUpdateSingle( contained1, containingEntity1 );

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

	/**
	 * Test that updating an indexed ElementCollection property in an entity
	 * that is IndexedEmbedded in an indexed entity
	 * does not trigger reindexing of the indexed entity
	 * if the association is marked with ReindexOnUpdate = NO.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3206")
	public void indirectValueUpdate_indexedEmbeddedNoReindexOnUpdate_elementCollectionValue_indexed() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.getIndexedElementCollectionField( contained1 ).add( "firstValue" );
			primitives.setContainedIndexedEmbeddedNoReindexOnUpdateSingle( containingEntity1, contained1 );
			primitives.setContainingAsIndexedEmbeddedNoReindexOnUpdateSingle( contained1, containingEntity1 );

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
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.getIndexedElementCollectionField( contained ).add( "secondValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.getIndexedElementCollectionField( contained ).remove( 0 );

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
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TContaining containingEntity1 = primitives.newContaining( 2 );
			primitives.setChild( entity1, containingEntity1 );
			primitives.setParent( containingEntity1, entity1 );

			TContained contained1 = primitives.newContained( 4 );
			primitives.getIndexedElementCollectionField( contained1 ).add( "firstValue" );
			primitives.setContainedIndexedEmbeddedNoReindexOnUpdateSingle( containingEntity1, contained1 );
			primitives.setContainingAsIndexedEmbeddedNoReindexOnUpdateSingle( contained1, containingEntity1 );

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
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TContained contained = session.get( primitives.getContainedClass(), 4 );
			primitives.setIndexedElementCollectionField( contained, new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

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

	interface AssociationModelPrimitives<
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

		void setChild(TContaining parent, TContaining child);

		void setParent(TContaining child, TContaining parent);

		void setContainedIndexedEmbeddedSingle(TContaining containing, TContained contained);

		void setContainingAsIndexedEmbeddedSingle(TContained contained, TContaining containing);

		void setContainedIndexedEmbeddedNoReindexOnUpdateSingle(TContaining containing, TContained contained);

		void setContainingAsIndexedEmbeddedNoReindexOnUpdateSingle(TContained contained, TContaining containing);

		void setContainedUsedInCrossEntityDerivedPropertySingle(TContaining containing, TContained contained);

		void setContainingAsUsedInCrossEntityDerivedPropertySingle(TContained contained, TContaining containing);

		void setIndexedField(TContained contained, String value);

		void setNonIndexedField(TContained contained, String value);

		List<String> getIndexedElementCollectionField(TContained contained);

		void setIndexedElementCollectionField(TContained contained, List<String> value);

		List<String> getNonIndexedElementCollectionField(TContained contained);

		void setNonIndexedElementCollectionField(TContained contained, List<String> value);

		void setFieldUsedInContainedDerivedField1(TContained contained, String value);

		void setFieldUsedInContainedDerivedField2(TContained contained, String value);

		void setFieldUsedInCrossEntityDerivedField1(TContained contained, String value);

		void setFieldUsedInCrossEntityDerivedField2(TContained contained, String value);
	}

}
