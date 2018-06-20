/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.service.ServiceRegistry;

import org.junit.After;
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
 */
public abstract class AbstractOrmAutomaticIndexingAssociationIT<
		TIndexed extends TContaining, TContaining, TContained
		> {

	private static final String PREFIX = SearchOrmSettings.PREFIX;

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	protected SessionFactory sessionFactory;

	private final AssociationModelPrimitives<TIndexed, TContaining, TContained> primitives;

	AbstractOrmAutomaticIndexingAssociationIT(
			AssociationModelPrimitives<TIndexed, TContaining, TContained> primitives) {
		this.primitives = primitives;
	}

	@Before
	public void setup() {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
				.applySetting( PREFIX + "backend.stubBackend.type", StubBackendFactory.class.getName() )
				.applySetting( PREFIX + "index.default.backend", "stubBackend" );

		ServiceRegistry serviceRegistry = registryBuilder.build();

		MetadataSources ms = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( primitives.getIndexedClass() )
				.addAnnotatedClass( primitives.getContainedClass() );

		Metadata metadata = ms.buildMetadata();

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();

		backendMock.expectSchema( primitives.getIndexName(), b -> b
				.objectField( "containedIndexedEmbedded", b2 -> b2
						.field( "indexedField", String.class )
						.field( "indexedElementCollectionField", String.class )
				)
				.objectField( "child", b3 -> b3
						.objectField( "containedIndexedEmbedded", b2 -> b2
								.field( "indexedField", String.class )
								.field( "indexedElementCollectionField", String.class )
						)
				)
		);

		sessionFactory = sfb.build();
		backendMock.verifyExpectationsMet();
	}

	@After
	public void cleanup() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@Test
	public void indirectValueUpdate_singleValue_indexed() {
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
					.preparedThenExecuted();
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
					.preparedThenExecuted();
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
	public void indirectValueUpdate_singleValue_nonIndexed() {
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
					.preparedThenExecuted();
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
	public void indirectValueUpdate_elementCollectionValue_indexed() {
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
					.preparedThenExecuted();
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
					.preparedThenExecuted();
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
					.preparedThenExecuted();
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
	 * Test that replacing a non-indexed, ElementCollection property in an entity
	 * whose properties are otherwise used in an IndexedEmbedded from an indexed entity
	 * does trigger reindexing of the indexed entity.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void indirectValueReplace_elementCollectionValue_indexed() {
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
					.preparedThenExecuted();
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
					.preparedThenExecuted();
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
	public void indirectValueUpdate_elementCollectionValue_nonIndexed() {
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
					.preparedThenExecuted();
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
	public void indirectValueReplace_elementCollectionValue_nonIndexed() {
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
					.preparedThenExecuted();
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
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	interface AssociationModelPrimitives<
			TIndexed extends TContaining,
			TContaining,
			TContained
			> {
		String getIndexName();

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

		void setIndexedField(TContained contained, String value);

		void setNonIndexedField(TContained contained, String value);

		List<String> getIndexedElementCollectionField(TContained contained);

		void setIndexedElementCollectionField(TContained contained, List<String> value);

		List<String> getNonIndexedElementCollectionField(TContained contained);

		void setNonIndexedElementCollectionField(TContained contained, List<String> value);
	}

}
