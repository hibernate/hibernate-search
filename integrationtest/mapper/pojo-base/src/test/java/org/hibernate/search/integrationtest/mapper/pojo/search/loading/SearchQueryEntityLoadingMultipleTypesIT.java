/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading;

import java.util.Arrays;

import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy1_A_B;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy1_A_C;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy1_A__Abstract;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy2_A_B;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy2_A_C;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy2_A__NonAbstract_Indexed;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy3_A_B;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy3_A_C;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy3_A__NonAbstract_NonIndexed;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy4_A_B__integer1DocumentId;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy4_A_C__integer2DocumentId;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy4_A_D;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy4_A__NonAbstract_NonIndexed;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy5_A_B_C;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy5_A_B_D;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy5_A_B__MappedSuperClass;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy5_A__Abstract;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy7_A_B;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy7_A_C;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy7_A_D;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Hierarchy7_A__Abstract;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes.Interface1;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test entity loading when executing a search query
 * for cases involving multiple entity types.
 */
class SearchQueryEntityLoadingMultipleTypesIT extends AbstractSearchQueryEntityLoadingIT {

	private SearchMapping mapping;

	@Override
	protected SearchMapping mapping() {
		return mapping;
	}

	@BeforeEach
	void setup() {
		backendMock.expectAnySchema( Hierarchy1_A_B.NAME );
		backendMock.expectAnySchema( Hierarchy1_A_C.NAME );

		backendMock.expectAnySchema( Hierarchy2_A__NonAbstract_Indexed.NAME );
		backendMock.expectAnySchema( Hierarchy2_A_B.NAME );
		backendMock.expectAnySchema( Hierarchy2_A_C.NAME );

		backendMock.expectAnySchema( Hierarchy3_A_B.NAME );
		backendMock.expectAnySchema( Hierarchy3_A_C.NAME );

		backendMock.expectAnySchema( Hierarchy4_A_B__integer1DocumentId.NAME );
		backendMock.expectAnySchema( Hierarchy4_A_C__integer2DocumentId.NAME );
		backendMock.expectAnySchema( Hierarchy4_A_D.NAME );

		backendMock.expectAnySchema( Hierarchy5_A_B_C.NAME );
		backendMock.expectAnySchema( Hierarchy5_A_B_D.NAME );

		backendMock.expectAnySchema( Hierarchy7_A_B.NAME );
		backendMock.expectAnySchema( Hierarchy7_A_C.NAME );
		backendMock.expectAnySchema( Hierarchy7_A_D.NAME );

		mapping = setupHelper.start()
				.expectCustomBeans()
				.withAnnotatedTypes( Hierarchy1_A__Abstract.class, Hierarchy1_A_B.class, Hierarchy1_A_C.class,
						Hierarchy2_A__NonAbstract_Indexed.class, Hierarchy2_A_B.class, Hierarchy2_A_C.class,
						Hierarchy3_A__NonAbstract_NonIndexed.class, Hierarchy3_A_B.class, Hierarchy3_A_C.class,
						Hierarchy4_A__NonAbstract_NonIndexed.class, Hierarchy4_A_B__integer1DocumentId.class,
						Hierarchy4_A_C__integer2DocumentId.class, Hierarchy4_A_D.class,
						Hierarchy5_A__Abstract.class, Hierarchy5_A_B__MappedSuperClass.class,
						Hierarchy5_A_B_C.class, Hierarchy5_A_B_D.class,
						Hierarchy7_A__Abstract.class, Hierarchy7_A_B.class, Hierarchy7_A_C.class, Hierarchy7_A_D.class )
				.setup();

		backendMock.verifyExpectationsMet();

		initData();
	}

	/**
	 * Test loading multiple entity types from a single hierarchy.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	void singleHierarchy() {
		testLoading(
				Arrays.asList(
						Hierarchy1_A_B.class,
						Hierarchy1_A_C.class
				),
				Arrays.asList(
						Hierarchy1_A_B.NAME,
						Hierarchy1_A_C.NAME
				),
				c -> c
						.doc( Hierarchy1_A_B.NAME, "2" )
						.doc( Hierarchy1_A_C.NAME, "3" ),
				c -> c
						.entity( Hierarchy1_A_B.PERSISTENCE_KEY, 2 )
						.entity( Hierarchy1_A_C.PERSISTENCE_KEY, 3 ),
				c -> c.assertThat( loadingContext.loaderCalls() ).hasSize( 1 ) // Optimized: only one loader per entity hierarchy
		);
	}

	/**
	 * Test loading multiple entity types from a single hierarchy
	 * where a mapped superclass happens to stand between the targeted types
	 * and the most specific common *entity* superclass.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	void singleHierarchy_middleMappedSuperClass() {
		testLoading(
				Arrays.asList(
						Hierarchy5_A_B_C.class,
						Hierarchy5_A_B_D.class
				),
				Arrays.asList(
						Hierarchy5_A_B_C.NAME,
						Hierarchy5_A_B_D.NAME
				),
				c -> c
						.doc( Hierarchy5_A_B_C.NAME, "3" )
						.doc( Hierarchy5_A_B_D.NAME, "4" ),
				c -> c
						.entity( Hierarchy5_A_B_C.PERSISTENCE_KEY, 3 )
						.entity( Hierarchy5_A_B_D.PERSISTENCE_KEY, 4 ),
				c -> c.assertThat( loadingContext.loaderCalls() ).hasSize( 1 ) // Optimized: only one loader per entity hierarchy
		);
	}

	/**
	 * Test loading entity types from different hierarchies,
	 * some types being part of the same hierarchy,
	 * some not.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	void mixedHierarchies() {
		testLoading(
				Arrays.asList(
						Hierarchy1_A_B.class,
						Hierarchy1_A_C.class,
						Hierarchy2_A__NonAbstract_Indexed.class,
						Hierarchy2_A_B.class,
						Hierarchy2_A_C.class,
						Hierarchy3_A_B.class,
						Hierarchy3_A_C.class
				),
				Arrays.asList(
						Hierarchy1_A_B.NAME,
						Hierarchy1_A_C.NAME,
						Hierarchy2_A__NonAbstract_Indexed.NAME,
						Hierarchy2_A_B.NAME,
						Hierarchy2_A_C.NAME,
						Hierarchy3_A_B.NAME,
						Hierarchy3_A_C.NAME
				),
				c -> c
						.doc( Hierarchy1_A_B.NAME, "2" )
						.doc( Hierarchy1_A_C.NAME, "3" )
						.doc( Hierarchy2_A__NonAbstract_Indexed.NAME, "1" )
						.doc( Hierarchy2_A_B.NAME, "2" )
						.doc( Hierarchy2_A_C.NAME, "3" )
						.doc( Hierarchy3_A_B.NAME, "2" )
						.doc( Hierarchy3_A_C.NAME, "3" ),
				c -> c
						.entity( Hierarchy1_A_B.PERSISTENCE_KEY, 2 )
						.entity( Hierarchy1_A_C.PERSISTENCE_KEY, 3 )
						.entity( Hierarchy2_A__NonAbstract_Indexed.PERSISTENCE_KEY, 1 )
						.entity( Hierarchy2_A_B.PERSISTENCE_KEY, 2 )
						.entity( Hierarchy2_A_C.PERSISTENCE_KEY, 3 )
						.entity( Hierarchy3_A_B.PERSISTENCE_KEY, 2 )
						.entity( Hierarchy3_A_C.PERSISTENCE_KEY, 3 ),
				c -> c.assertThat( loadingContext.loaderCalls() ).hasSize( 3 ) // Optimized: only one loader per entity hierarchy
		);
	}

	/**
	 * Test loading two entity types from different entity hierarchies with a different document ID mapping:
	 * one mapped to the entity ID, the other to a property.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3203")
	void mixedDocumentIdMapping_entityIdAndProperty_mixedHierarchies() {
		testLoading(
				Arrays.asList(
						Hierarchy4_A_B__integer1DocumentId.class,
						Hierarchy1_A_B.class
				),
				Arrays.asList(
						Hierarchy4_A_B__integer1DocumentId.NAME,
						Hierarchy1_A_B.NAME
				),
				c -> c
						.doc( Hierarchy4_A_B__integer1DocumentId.NAME, "42" )
						.doc( Hierarchy1_A_B.NAME, "2" ),
				c -> c
						.entity( Hierarchy4_A_B__integer1DocumentId.PERSISTENCE_KEY, 42 )
						.entity( Hierarchy1_A_B.PERSISTENCE_KEY, 2 ),
				c -> c.assertThat( loadingContext.loaderCalls() ).hasSize( 2 ) // Different loading strategy => need two separate loaders
		);
	}

	/**
	 * Test loading two entity types from the same entity hierarchy with a different document ID mapping:
	 * one mapped to the entity ID, the other to a property.
	 */
	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3203", "HSEARCH-3349" })
	void mixedDocumentIdMapping_entityIdAndProperty_singleHierarchy() {
		testLoading(
				Arrays.asList(
						Hierarchy4_A_B__integer1DocumentId.class,
						Hierarchy4_A_D.class
				),
				Arrays.asList(
						Hierarchy4_A_B__integer1DocumentId.NAME,
						Hierarchy4_A_D.NAME
				),
				c -> c
						.doc( Hierarchy4_A_B__integer1DocumentId.NAME, "42" )
						.doc( Hierarchy4_A_D.NAME, "4" ),
				c -> c
						.entity( Hierarchy4_A_B__integer1DocumentId.PERSISTENCE_KEY, 42 )
						.entity( Hierarchy4_A_D.PERSISTENCE_KEY, 4 ),
				c -> c.assertThat( loadingContext.loaderCalls() ).hasSize( 2 ) // Different loading strategy => need two separate loaders
		);
	}

	/**
	 * Test loading two entity types where the document ID is mapped to a property that is not the entity ID,
	 * but to a different property in each type.
	 * one mapped to the entity ID, the other to a property.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3203")
	void mixedDocumentIdMapping_differentProperty() {
		testLoading(
				Arrays.asList(
						Hierarchy4_A_B__integer1DocumentId.class,
						Hierarchy4_A_C__integer2DocumentId.class
				),
				Arrays.asList(
						Hierarchy4_A_B__integer1DocumentId.NAME,
						Hierarchy4_A_C__integer2DocumentId.NAME
				),
				c -> c
						.doc( Hierarchy4_A_B__integer1DocumentId.NAME, "42" )
						.doc( Hierarchy4_A_C__integer2DocumentId.NAME, "43" ),
				c -> c
						.entity( Hierarchy4_A_B__integer1DocumentId.PERSISTENCE_KEY, 42 )
						.entity( Hierarchy4_A_C__integer2DocumentId.PERSISTENCE_KEY, 43 ),
				c -> c.assertThat( loadingContext.loaderCalls() ).hasSize( 2 ) // Different loading strategy => need two separate loaders
		);
	}

	/**
	 * Test loading of entities that are found in the database, but with a type that doesn't match the expected type.
	 *
	 * This can happen when the index is slightly out of sync and still has deleted entities in it.
	 * For example, with entity types A, B, C, D, with B, C and D extending A,
	 * an instance of type C and with id 4 may be deleted and replaced with an instance of type D and id 4.
	 * If the search query returns a reference to "C with id 4",
	 * it is possible that the loader will end up loading "D with id 4".
	 * This is problematic because the user may have requested a search on an interface implemented by B and C,
	 * but not D,
	 * in which case the hits will contain one object of type D that does not implement the expected interface.
	 *
	 * That is why the incompatible type should be detected
	 * and the entity should be deemed deleted,
	 * so the loader should return null,
	 * and the backend should skip the corresponding hits.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	void typeChanged() {
		testLoading(
				Arrays.asList( Interface1.class ), // Implemented by B and C, but not D
				Arrays.asList(
						Hierarchy7_A_B.NAME,
						Hierarchy7_A_C.NAME
				),
				c -> c
						/*
						 * The index contains "B with ID 2" (correct)
						 * and "C with ID 4" (incorrect, in the database we have "D with ID 4").
						 * Since loading focuses on the common supertype A, which is also a supertype of D,
						 * the loader will manage to load both two entities,
						 * but one of them will have the wrong type (D instead of the expected C).
						 * This should be detected and the entity with the wrong type should be replaced with null
						 * to avoid class cast exceptions in the user code.
						 */
						.doc( Hierarchy7_A_B.NAME, "2" )
						.doc( Hierarchy7_A_C.NAME, "4" ),
				c -> c
						.entity( Hierarchy7_A_B.PERSISTENCE_KEY, 2 ),
				c -> c.assertThat( loadingContext.loaderCalls() ).hasSize( 1 )
		);
	}

	private void initData() {
		persist( Hierarchy1_A_B.PERSISTENCE_KEY, 2, new Hierarchy1_A_B( 2 ) );
		persist( Hierarchy1_A_C.PERSISTENCE_KEY, 3, new Hierarchy1_A_C( 3 ) );

		persist( Hierarchy2_A__NonAbstract_Indexed.PERSISTENCE_KEY, 1, new Hierarchy2_A__NonAbstract_Indexed( 1 ) );
		persist( Hierarchy2_A_B.PERSISTENCE_KEY, 2, new Hierarchy2_A_B( 2 ) );
		persist( Hierarchy2_A_C.PERSISTENCE_KEY, 3, new Hierarchy2_A_C( 3 ) );

		persist( Hierarchy3_A__NonAbstract_NonIndexed.PERSISTENCE_KEY, 1, new Hierarchy3_A__NonAbstract_NonIndexed( 1 ) );
		persist( Hierarchy3_A_B.PERSISTENCE_KEY, 2, new Hierarchy3_A_B( 2 ) );
		persist( Hierarchy3_A_C.PERSISTENCE_KEY, 3, new Hierarchy3_A_C( 3 ) );

		persist( Hierarchy4_A__NonAbstract_NonIndexed.PERSISTENCE_KEY, 1, new Hierarchy4_A__NonAbstract_NonIndexed( 1 ) );
		persist( Hierarchy4_A_B__integer1DocumentId.PERSISTENCE_KEY, 42, new Hierarchy4_A_B__integer1DocumentId( 2, 42 ) );
		persist( Hierarchy4_A_C__integer2DocumentId.PERSISTENCE_KEY, 43, new Hierarchy4_A_C__integer2DocumentId( 3, 43 ) );
		persist( Hierarchy4_A_D.PERSISTENCE_KEY, 4, new Hierarchy4_A_D( 4 ) );

		persist( Hierarchy5_A_B_C.PERSISTENCE_KEY, 3, new Hierarchy5_A_B_C( 3 ) );
		persist( Hierarchy5_A_B_D.PERSISTENCE_KEY, 4, new Hierarchy5_A_B_D( 4 ) );

		persist( Hierarchy7_A_B.PERSISTENCE_KEY, 2, new Hierarchy7_A_B( 2 ) );
		persist( Hierarchy7_A_C.PERSISTENCE_KEY, 3, new Hierarchy7_A_C( 3 ) );
		persist( Hierarchy7_A_D.PERSISTENCE_KEY, 4, new Hierarchy7_A_D( 4 ) );
	}

	private <E, I> void persist(PersistenceTypeKey<E, I> key, I id, E entity) {
		loadingContext.persistenceMap( key ).put( id, entity );
	}

}
