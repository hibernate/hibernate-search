/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.ManagedAssert.assertThatManaged;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecodeEnhancerRunner.class)
public class BytecodeEnhancementIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

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

		sessionFactory = ormSetupHelper.start()
				/*
				 * This is necessary in order for the BytecodeEnhancerRunner to work correctly.
				 * Otherwise classes can be successfully loaded from the application classloader
				 * and the "bytecode-enhancing" TCCL won't even be tried.
				 */
				.withTcclLookupPrecedenceBefore()
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
	public void testBytecodeEnhancementWorked() {
		assertThat( IndexedEntity.class.getDeclaredMethods() )
				.extracting( Method::getName )
				.anyMatch( name -> name.startsWith( "$$_hibernate_" ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3581")
	public void test() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			// This cast is necessary to work around https://hibernate.atlassian.net/browse/HHH-14006
			( (IndexedEntitySuperClass) entity1 ).id = 1;
			// This cast is necessary to work around https://hibernate.atlassian.net/browse/HHH-14006
			( (IndexedMappedSuperClass) entity1 ).mappedSuperClassText = "initialValue";
			// This cast is necessary to work around https://hibernate.atlassian.net/browse/HHH-14006
			( (IndexedEntitySuperClass) entity1 ).entitySuperClassText = "initialValue";
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
					);
			} );

			AtomicReference<IndexedEntity> entityFromTransaction = new AtomicReference<>();

			with( sessionFactory ).runInTransaction( session -> {
				IndexedEntity entity = session.getReference( IndexedEntity.class, 1 );
				entityFromTransaction.set( entity );

				assertOnlyLoadedPropertiesAre( entity, "id" );
				// Because of HHH-10480, lazy loading through bytecode enhancement doesn't work on embeddables
				assertOnlyLoadedPropertiesAre( entity.containedEmbeddable, "text" );

				// Trigger reindexing
				entity.text1 = "updatedValue";

				assertOnlyLoadedPropertiesAre( entity, "id", "text1" );
				// Because of HHH-10480, lazy loading through bytecode enhancement doesn't work on embeddables
				assertOnlyLoadedPropertiesAre( entity.containedEmbeddable, "text" );

				// Expect all properties to be correctly loaded, even though we're using bytecode enhancement
				backendMock.expectWorks( IndexedEntity.INDEX )
						.addOrUpdate( "1", b -> b
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
						);
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
		Set<String> expectedLoadedPropertyNames = CollectionHelper.asImmutableSet( expectedLoadedProperties );
		Collections.addAll( expectedNotLoadedPropertyNames, allProperties );
		expectedNotLoadedPropertyNames.removeAll( expectedLoadedPropertyNames );

		assertPropertiesAreNotLoaded( object, expectedNotLoadedPropertyNames );
		assertPropertiesAreLoaded( object, expectedLoadedPropertyNames );
	}

	private static void assertPropertiesAreNotLoaded(Object object, String expectedNotLoadedProperties) {
		assertPropertiesAreNotLoaded( object, CollectionHelper.asImmutableSet( expectedNotLoadedProperties ) );
	}

	private static void assertPropertiesAreLoaded(Object object, Set<String> expectedLoadedProperties) {
		for ( String propertyName : expectedLoadedProperties ) {
			assertThatManaged( object ).hasPropertyInitialized( propertyName );
		}
	}

	private static void assertPropertiesAreNotLoaded(Object object, Set<String> expectedNotLoadedProperties) {
		for ( String propertyName : expectedNotLoadedProperties ) {
			assertThatManaged( object ).hasPropertyNotInitialized( propertyName );
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

		// "containedEntityList" is not listed here,
		// because collection properties are initialized eagerly on ORM 6.2+
		// (even if the collection themselves are initialized lazily).
		// See HHH-15473 / https://github.com/hibernate/hibernate-orm/pull/5252
		private static final String[] LAZY_PROPERTY_NAMES = new String[] {
				"mappedSuperClassText",
				"entitySuperClassText",
				"notIndexedText",
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
