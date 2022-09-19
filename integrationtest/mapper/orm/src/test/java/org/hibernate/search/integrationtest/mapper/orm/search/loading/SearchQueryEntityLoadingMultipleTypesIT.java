/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jakarta.persistence.SharedCacheMode;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy1_A_B;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy1_A_C;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy1_A__Abstract;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy2_A_B;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy2_A_C;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy2_A__NonAbstract_Indexed;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy3_A_B;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy3_A_C;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy3_A__NonAbstract_NonIndexed;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy4_A_B__integer1DocumentId;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy4_A_C__integer2DocumentId;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy4_A_D;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy4_A__NonAbstract_NonIndexed;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy5_A_B_C;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy5_A_B_D;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy5_A_B__MappedSuperClass;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy5_A__Abstract;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy6_A_B_Cacheable;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy6_A_C_Cacheable;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy6_A__Abstract;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy7_A_B;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy7_A_C;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy7_A_D;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy7_A__Abstract;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy8_A_B_Cacheable;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy8_A_C_Cacheable;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy8_A_D_Cacheable;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy8_A__Abstract;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Interface1;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Interface2;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSoftAssertions;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SlowerLoadingListener;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * Test entity loading when executing a search query
 * for cases involving multiple entity types.
 */
public class SearchQueryEntityLoadingMultipleTypesIT extends AbstractSearchQueryEntityLoadingIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@Override
	protected BackendMock backendMock() {
		return backendMock;
	}

	@Override
	protected SessionFactory sessionFactory() {
		return setupHolder.sessionFactory();
	}

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
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

		backendMock.expectAnySchema( Hierarchy6_A_B_Cacheable.NAME );
		backendMock.expectAnySchema( Hierarchy6_A_C_Cacheable.NAME );

		backendMock.expectAnySchema( Hierarchy7_A_B.NAME );
		backendMock.expectAnySchema( Hierarchy7_A_C.NAME );
		backendMock.expectAnySchema( Hierarchy7_A_D.NAME );

		backendMock.expectAnySchema( Hierarchy8_A_B_Cacheable.NAME );
		backendMock.expectAnySchema( Hierarchy8_A_C_Cacheable.NAME );
		backendMock.expectAnySchema( Hierarchy8_A_D_Cacheable.NAME );

		setupContext.withProperty( AvailableSettings.JPA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE.name() )
				.withAnnotatedTypes(
						Hierarchy1_A__Abstract.class,
						Hierarchy1_A_B.class,
						Hierarchy1_A_C.class,
						Hierarchy2_A__NonAbstract_Indexed.class,
						Hierarchy2_A_B.class,
						Hierarchy2_A_C.class,
						Hierarchy3_A__NonAbstract_NonIndexed.class,
						Hierarchy3_A_B.class,
						Hierarchy3_A_C.class,
						Hierarchy4_A__NonAbstract_NonIndexed.class,
						Hierarchy4_A_B__integer1DocumentId.class,
						Hierarchy4_A_C__integer2DocumentId.class,
						Hierarchy4_A_D.class,
						Hierarchy5_A__Abstract.class,
						Hierarchy5_A_B__MappedSuperClass.class,
						Hierarchy5_A_B_C.class,
						Hierarchy5_A_B_D.class,
						Hierarchy6_A__Abstract.class,
						Hierarchy6_A_B_Cacheable.class,
						Hierarchy6_A_C_Cacheable.class,
						Hierarchy7_A__Abstract.class,
						Hierarchy7_A_B.class,
						Hierarchy7_A_C.class,
						Hierarchy7_A_D.class,
						Hierarchy8_A__Abstract.class,
						Hierarchy8_A_B_Cacheable.class,
						Hierarchy8_A_C_Cacheable.class,
						Hierarchy8_A_D_Cacheable.class
				);
	}

	@Before
	public void initData() {
		// We don't care about what is indexed exactly, so use the lenient mode
		backendMock.inLenientMode( () -> setupHolder.runInTransaction( session -> {
			session.persist( new Hierarchy1_A_B( 2 ) );
			session.persist( new Hierarchy1_A_C( 3 ) );

			session.persist( new Hierarchy2_A__NonAbstract_Indexed( 1 ) );
			session.persist( new Hierarchy2_A_B( 2 ) );
			session.persist( new Hierarchy2_A_C( 3 ) );

			session.persist( new Hierarchy3_A__NonAbstract_NonIndexed( 1 ) );
			session.persist( new Hierarchy3_A_B( 2 ) );
			session.persist( new Hierarchy3_A_C( 3 ) );

			session.persist( new Hierarchy4_A__NonAbstract_NonIndexed( 1 ) );
			session.persist( new Hierarchy4_A_B__integer1DocumentId( 2, 42 ) );
			session.persist( new Hierarchy4_A_C__integer2DocumentId( 3, 43 ) );
			session.persist( new Hierarchy4_A_D( 4 ) );

			session.persist( new Hierarchy5_A_B_C( 3 ) );
			session.persist( new Hierarchy5_A_B_D( 4 ) );

			session.persist( new Hierarchy6_A_B_Cacheable( 2 ) );
			session.persist( new Hierarchy6_A_C_Cacheable( 3 ) );

			session.persist( new Hierarchy7_A_B( 2 ) );
			session.persist( new Hierarchy7_A_C( 3 ) );
			session.persist( new Hierarchy7_A_D( 4 ) );

			session.persist( new Hierarchy8_A_B_Cacheable( 2 ) );
			session.persist( new Hierarchy8_A_C_Cacheable( 3 ) );
			session.persist( new Hierarchy8_A_D_Cacheable( 4 ) );
		} ) );
	}

	/**
	 * Test loading multiple entity types from a single hierarchy.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void singleHierarchy() {
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
						.entity( Hierarchy1_A_B.class, 2 )
						.entity( Hierarchy1_A_C.class, 3 ),
				c -> c.assertStatementExecutionCount().isEqualTo( 1 ) // Optimized: only one query per entity hierarchy
		);
	}

	/**
	 * Test loading multiple entity types from a single hierarchy
	 * where a mapped superclass happens to stand between the targeted types
	 * and the most specific common *entity* superclass.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void singleHierarchy_middleMappedSuperClass() {
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
						.entity( Hierarchy5_A_B_C.class, 3 )
						.entity( Hierarchy5_A_B_D.class, 4 ),
				c -> c.assertStatementExecutionCount().isEqualTo( 1 ) // Optimized: only one query per entity hierarchy
		);
	}

	/**
	 * Test loading entity types from different hierarchies,
	 * some types being part of the same hierarchy,
	 * some not.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void mixedHierarchies() {
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
						.entity( Hierarchy1_A_B.class, 2 )
						.entity( Hierarchy1_A_C.class, 3 )
						.entity( Hierarchy2_A__NonAbstract_Indexed.class, 1 )
						.entity( Hierarchy2_A_B.class, 2 )
						.entity( Hierarchy2_A_C.class, 3 )
						.entity( Hierarchy3_A_B.class, 2 )
						.entity( Hierarchy3_A_C.class, 3 ),
				c -> c.assertStatementExecutionCount().isEqualTo( 3 ) // Optimized: only one query per entity hierarchy
		);
	}

	@Test
	public void mixedHierarchies_entityLoadingTimeout() {
		assertThatThrownBy( () -> testLoading(
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
						.entity( Hierarchy1_A_B.class, 2 )
						.entity( Hierarchy1_A_C.class, 3 )
						.entity( Hierarchy2_A__NonAbstract_Indexed.class, 1 )
						.entity( Hierarchy2_A_B.class, 2 )
						.entity( Hierarchy2_A_C.class, 3 )
						.entity( Hierarchy3_A_B.class, 2 )
						.entity( Hierarchy3_A_C.class, 3 ),
				c -> c.assertStatementExecutionCount().isEqualTo( 3 ), // Optimized: only one query per entity hierarchy
				1, TimeUnit.MICROSECONDS,
				session -> SlowerLoadingListener.registerSlowerLoadingListener( session, 100 )
		) )
				.isInstanceOf( SearchTimeoutException.class )
				.hasMessageContaining( "Operation exceeded the timeout of 0s, 0ms and 1000ns" );
	}

	/**
	 * Test loading two entity types from different entity hierarchies with a different document ID mapping:
	 * one mapped to the entity ID, the other to a property.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3203")
	public void mixedDocumentIdMapping_entityIdAndProperty_mixedHierarchies() {
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
						.entity( Hierarchy4_A_B__integer1DocumentId.class, 2 )
						.entity( Hierarchy1_A_B.class, 2 ),
				c -> c.assertStatementExecutionCount().isEqualTo( 2 ) // Different loading method => need two separate statements
		);
	}

	/**
	 * Test loading two entity types from the same entity hierarchy with a different document ID mapping:
	 * one mapped to the entity ID, the other to a property.
	 */
	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3203", "HSEARCH-3349" })
	public void mixedDocumentIdMapping_entityIdAndProperty_singleHierarchy() {
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
						.entity( Hierarchy4_A_B__integer1DocumentId.class, 2 )
						.entity( Hierarchy4_A_D.class, 4 ),
				c -> c.assertStatementExecutionCount().isEqualTo( 2 ) // Different loading method => need two separate statements
		);
	}

	/**
	 * Test loading two entity types where the document ID is mapped to a property that is not the entity ID,
	 * but to a different property in each type.
	 * one mapped to the entity ID, the other to a property.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3203")
	public void mixedDocumentIdMapping_differentProperty() {
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
						.entity( Hierarchy4_A_B__integer1DocumentId.class, 2 )
						.entity( Hierarchy4_A_C__integer2DocumentId.class, 3 ),
				c -> c.assertStatementExecutionCount().isEqualTo( 2 ) // Different loading method => need two separate statements
		);
	}

	/**
	 * Test loading multiple entity types from a single hierarchy
	 * with second level cache lookup enabled.
	 * We expect Hibernate Search to look into the cache for each targeted type,
	 * even though a single loader is created for the abstract supertype,
	 * which has no cache.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void secondLevelCacheLookup() {
		testLoading(
				session -> {}, // No particular session setup needed
				Arrays.asList(
						Hierarchy6_A_B_Cacheable.class,
						Hierarchy6_A_C_Cacheable.class
				),
				Arrays.asList(
						Hierarchy6_A_B_Cacheable.NAME,
						Hierarchy6_A_C_Cacheable.NAME
				),
				loadingOptions -> loadingOptions.cacheLookupStrategy(
						EntityLoadingCacheLookupStrategy.PERSISTENCE_CONTEXT_THEN_SECOND_LEVEL_CACHE
				),
				c -> c
						.doc( Hierarchy6_A_B_Cacheable.NAME, "2" )
						.doc( Hierarchy6_A_C_Cacheable.NAME, "3" ),
				c -> c
						.entity( Hierarchy6_A_B_Cacheable.class, 2 )
						.entity( Hierarchy6_A_C_Cacheable.class, 3 ),
				c -> {
					c.assertSecondLevelCacheHitCount()
							.isEqualTo( 2 );
					c.assertStatementExecutionCount()
							.isEqualTo( 0 );
				}
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
	public void typeChanged() {
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
						.entity( Hierarchy7_A_B.class, 2 ),
				c -> c.assertStatementExecutionCount().isEqualTo( 1 )
		);
	}

	/**
	 * Same as {@link #typeChanged()},
	 * but with the entity that changed its type retrieved from the second level cache instead of from a query.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void typeChanged_secondLevelCacheLookup() {
		testLoading(
				session -> {}, // No particular session setup needed
				Arrays.asList( Interface2.class ), // Implemented by B and C, but not D
				Arrays.asList(
						Hierarchy8_A_B_Cacheable.NAME,
						Hierarchy8_A_C_Cacheable.NAME
				),
				loadingOptions -> loadingOptions.cacheLookupStrategy(
						EntityLoadingCacheLookupStrategy.PERSISTENCE_CONTEXT_THEN_SECOND_LEVEL_CACHE
				),
				c -> c
						// See typeChanged for a detailed explanation.
						.doc( Hierarchy8_A_B_Cacheable.NAME, "2" )
						.doc( Hierarchy8_A_B_Cacheable.NAME, "4" ),
				c -> c
						.entity( Hierarchy8_A_B_Cacheable.class, 2 ),
				c -> {
					c.assertSecondLevelCacheHitCount()
							.isEqualTo( 2 );
					c.assertStatementExecutionCount()
							.isEqualTo( 0 );
				}
		);
	}

	protected <T> void testLoading(List<? extends Class<? extends T>> targetClasses,
			List<String> targetIndexes,
			Consumer<DocumentReferenceCollector> hitDocumentReferencesContributor,
			Consumer<EntityCollector<T>> expectedLoadedEntitiesContributor,
			Consumer<OrmSoftAssertions> assertionsContributor) {
		testLoading(
				session -> {}, // No particular session setup needed
				targetClasses, targetIndexes,
				o -> {}, // We don't use any particular loading option
				hitDocumentReferencesContributor,
				expectedLoadedEntitiesContributor,
				assertionsContributor
		);
	}

	protected <T> void testLoading(List<? extends Class<? extends T>> targetClasses,
			List<String> targetIndexes,
			Consumer<DocumentReferenceCollector> hitDocumentReferencesContributor,
			Consumer<EntityCollector<T>> expectedLoadedEntitiesContributor,
			Consumer<OrmSoftAssertions> assertionsContributor,
			Integer timeout, TimeUnit timeUnit, Consumer<Session> sessionSetup) {
		testLoading(
				sessionSetup, targetClasses, targetIndexes,
				o -> {}, // We don't use any particular loading option
				hitDocumentReferencesContributor,
				expectedLoadedEntitiesContributor,
				(assertions, ignored) -> assertionsContributor.accept( assertions ),
				timeout, timeUnit
		);
	}

}
