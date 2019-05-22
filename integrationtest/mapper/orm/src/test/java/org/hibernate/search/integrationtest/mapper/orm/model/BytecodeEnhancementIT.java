/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.Basic;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence;
import org.hibernate.search.mapper.pojo.dirtiness.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecodeEnhancerRunner.class)
public class BytecodeEnhancementIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = new OrmSetupHelper();

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "mappedSuperClassText", String.class )
				.field( "entitySuperClassText", String.class )
				.field( "id", Integer.class )
				.objectField( "containedEntityList", b2 -> b2
						.multiValued( true )
						.field( "text", String.class )
				)
				.objectField( "containedEmbeddable", b2 -> b2
						.field( "text", String.class )
				)
				.field( "text1", String.class )
				.field( "text2", String.class )
				.field( "primitiveInteger", Integer.class )
				.field( "primitiveLong", Long.class )
				.field( "primitiveBoolean", Boolean.class )
				.field( "primitiveFloat", Float.class )
				.field( "primitiveDouble", Double.class )
				.field( "transientText", String.class )
		);

		sessionFactory = ormSetupHelper.withBackendMock( backendMock )
				/*
				 * This is necessary in order for the BytecodeEnhancerRunner to work correctly.
				 * Otherwise classes can be successfully loaded from the application classloader
				 * and the "bytecode-enhancing" TCCL won't even be tried.
				 */
				.withTcclLookupPrecedence( TcclLookupPrecedence.BEFORE )
				.setup(
						IndexedMappedSuperClass.class,
						IndexedEntitySuperClass.class,
						IndexedEntity.class,
						ContainedEntity.class,
						ContainedEmbeddable.class
				);
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void test() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.id = 1;
			entity1.mappedSuperClassText = "initialValue";
			entity1.entitySuperClassText = "initialValue";
			entity1.containedEntityList = new ArrayList<>();
			entity1.text1 = "initialValue";
			entity1.text2 = "initialValue";
			entity1.primitiveInteger = 42;
			entity1.primitiveLong = 42L;
			entity1.primitiveFloat = 42.0f;
			entity1.primitiveDouble = 42.0d;
			entity1.primitiveBoolean = true;
			entity1.transientText = "initialValue";

			ContainedEntity containedEntity1 = new ContainedEntity();
			entity1.containedEntityList.add( containedEntity1 );
			containedEntity1.containingEntity = entity1;
			containedEntity1.id = 2;
			containedEntity1.text = "initialValue1";

			ContainedEntity containedEntity2 = new ContainedEntity();
			entity1.containedEntityList.add( containedEntity2 );
			containedEntity2.containingEntity = entity1;
			containedEntity2.id = 3;
			containedEntity2.text = "initialValue2";

			ContainedEmbeddable containedEmbeddable = new ContainedEmbeddable();
			entity1.containedEmbeddable = containedEmbeddable;
			containedEmbeddable.text = "initialValue";

			session.persist( containedEntity1 );
			session.persist( containedEntity2 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "mappedSuperClassText", "initialValue" )
							.field( "entitySuperClassText", "initialValue" )
							.field( "id", 1 )
							.objectField( "containedEntityList", b2 -> b2
									.field( "text", "initialValue1" )
							)
							.objectField( "containedEntityList", b2 -> b2
									.field( "text", "initialValue2" )
							)
							.objectField( "containedEmbeddable", b2 -> b2
									.field( "text", "initialValue" )
							)
							.field( "text1", "initialValue" )
							.field( "text2", "initialValue" )
							.field( "primitiveInteger", 42 )
							.field( "primitiveLong", 42L )
							.field( "primitiveBoolean", true )
							.field( "primitiveFloat", 42.0f )
							.field( "primitiveDouble", 42.0d )
							.field( "transientText", "initialValue" )
					)
					.preparedThenExecuted();
			} );

			AtomicReference<IndexedEntity> entityFromTransaction = new AtomicReference<>();

			OrmUtils.withinTransaction( sessionFactory, session -> {
				IndexedEntity entity = session.load( IndexedEntity.class, 1 );
				entityFromTransaction.set( entity );

				assertOnlyLoadedPropertiesAre( entity, "id" );
				assertOnlyLoadedPropertiesAre( entity.containedEmbeddable );

				// Trigger reindexing
				entity.text1 = "updatedValue";

				assertOnlyLoadedPropertiesAre( entity, "id", "text1" );
				assertOnlyLoadedPropertiesAre( entity.containedEmbeddable );

				// Expect all properties to be correctly loaded, even though we're using bytecode enhancement
				backendMock.expectWorks( IndexedEntity.INDEX )
						.update( "1", b -> b
								.field( "mappedSuperClassText", "initialValue" )
								.field( "entitySuperClassText", "initialValue" )
								.field( "id", 1 )
								.objectField( "containedEntityList", b2 -> b2
										.field( "text", "initialValue1" )
								)
								.objectField( "containedEntityList", b2 -> b2
										.field( "text", "initialValue2" )
								)
								.objectField( "containedEmbeddable", b2 -> b2
										.field( "text", "initialValue" )
								)
								.field( "text1", "updatedValue" )
								.field( "text2", "initialValue" )
								.field( "primitiveInteger", 42 )
								.field( "primitiveLong", 42L )
								.field( "primitiveBoolean", true )
								.field( "primitiveFloat", 42.0f )
								.field( "primitiveDouble", 42.0d )
								.field( "transientText", null )
						)
						.preparedThenExecuted();
		} );

		assertPropertiesAreNotLoaded( entityFromTransaction.get(), "notIndexedText" );
	}

	private static void assertOnlyLoadedPropertiesAre(IndexedEntity entity, String ... expectedLoadedProperties) {
		assertOnlyLoadedPropertiesAre( entity, IndexedEntity.LAZY_PROPERTY_NAMES, expectedLoadedProperties );
	}

	private static void assertOnlyLoadedPropertiesAre(ContainedEmbeddable embeddable, String ... expectedLoadedProperties) {
		assertOnlyLoadedPropertiesAre( embeddable, ContainedEmbeddable.LAZY_PROPERTY_NAMES, expectedLoadedProperties );
	}

	private static void assertOnlyLoadedPropertiesAre(Object object, String[] allProperties,
			String ... expectedLoadedProperties) {
		Set<String> expectedNotLoadedPropertyNames = new HashSet<>();
		Collections.addAll( expectedNotLoadedPropertyNames, allProperties );
		expectedNotLoadedPropertyNames.removeAll( CollectionHelper.asImmutableSet( expectedLoadedProperties ) );
		assertPropertiesAreNotLoaded( object, expectedNotLoadedPropertyNames );
	}

	private static void assertPropertiesAreNotLoaded(Object object, String expectedNotLoadedProperties) {
		assertPropertiesAreNotLoaded( object, CollectionHelper.asImmutableSet( expectedNotLoadedProperties ) );
	}

	private static void assertPropertiesAreNotLoaded(Object object, Set<String> expectedNotLoadedProperties) {
		for ( String propertyName : expectedNotLoadedProperties ) {
			Object loadedValue = EnhancerTestUtils.getFieldByReflection( object, propertyName );
			assertThat( loadedValue )
					.as(
							"Loaded value of '" + propertyName + "' in object of type '"
							+ object.getClass().getSimpleName() + "'"
					)
					.satisfiesAnyOf(
							obj -> assertThat( obj ).isNull(),
							// Primitive fields cannot be null
							obj -> assertThat( obj ).isIn( 0, 0L, 0.0f, 0.0d, false )
					);
		}
	}

	@MappedSuperclass
	public static class IndexedMappedSuperClass {

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("mappedSuper_group1")
		@GenericField
		public String mappedSuperClassText;

	}

	@Entity(name = "entitySuper")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class IndexedEntitySuperClass extends IndexedMappedSuperClass {

		@Id
		@GenericField
		public Integer id;

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("entitySuper_group1")
		@GenericField
		public String entitySuperClassText;

	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity
			extends IndexedEntitySuperClass {
		public static final String INDEX = "IndexedEntity";

		private static final String[] LAZY_PROPERTY_NAMES = new String[] {
				"mappedSuperClassText",
				"entitySuperClassText",
				"notIndexedText",
				"containedEntityList",
				"text1",
				"text2",
				"primitiveInteger",
				"primitiveLong",
				"primitiveFloat",
				"primitiveDouble",
				"primitiveBoolean"
		};

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("group1")
		public String notIndexedText;

		@OneToMany(mappedBy = "containingEntity", fetch = FetchType.LAZY)
		@LazyGroup("group2")
		@IndexedEmbedded
		public List<ContainedEntity> containedEntityList;

		@Embedded
		@LazyGroup("group3")
		@IndexedEmbedded
		public ContainedEmbeddable containedEmbeddable;

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("group4")
		@GenericField
		public String text1;

		// Add another text property in order to change the first one and check lazy loading on the second one
		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("group5")
		@GenericField
		public String text2;

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("group6")
		@GenericField
		public int primitiveInteger;

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("group7")
		@GenericField
		public long primitiveLong;

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("group8")
		@GenericField
		public float primitiveFloat;

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("group9")
		@GenericField
		public double primitiveDouble;

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("group10")
		@GenericField
		public boolean primitiveBoolean;

		@Transient
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
		@GenericField
		public String transientText;
	}

	@Entity(name = "contained")
	public static class ContainedEntity {
		@Id
		public Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		@LazyGroup("group2_1")
		public IndexedEntity containingEntity;

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("group2_2")
		@GenericField
		public String text;
	}

	@Embeddable
	public static class ContainedEmbeddable {
		private static final String[] LAZY_PROPERTY_NAMES = new String[] {
				"text"
		};

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("group3_1")
		@GenericField
		public String text;
	}

}
