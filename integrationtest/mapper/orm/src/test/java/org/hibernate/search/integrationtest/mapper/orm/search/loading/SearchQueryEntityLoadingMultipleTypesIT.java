/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import java.util.Arrays;

import org.hibernate.SessionFactory;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy1_A__Abstract;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy1_A_B;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy1_A_C;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy2_A__NonAbstract_Indexed;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy2_A_B;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy2_A_C;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy3_A__NonAbstract_NonIndexed;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy3_A_B;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy3_A_C;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy4_A__NonAbstract_NonIndexed;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy4_A_B__integer1DocumentId;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy4_A_C__integer2DocumentId;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy4_A_D;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy5_A_B_C;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy5_A_B_D;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy5_A_B__MappedSuperClass;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes.Hierarchy5_A__Abstract;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Test;

/**
 * Test entity loading when executing a search query
 * for cases involving multiple entity types.
 */
public class SearchQueryEntityLoadingMultipleTypesIT extends AbstractSearchQueryEntityLoadingIT {

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
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

		sessionFactory = ormSetupHelper.withBackendMock( backendMock )
				.setup(
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
						Hierarchy5_A_B_D.class
				);

		backendMock.verifyExpectationsMet();

		initData();
	}

	/**
	 * Test loading multiple entity types from a single hierarchy.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3349")
	public void singleHierarchy() {
		// TODO HSEARCH-3349 check that we optimize this by running a single query even if multiple types are loaded

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
						.entity( Hierarchy1_A_C.class, 3 )
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
						.entity( Hierarchy5_A_B_D.class, 4 )
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
		// TODO HSEARCH-3349 check that we optimize this by running a single query for each hierarchy

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
						.entity( Hierarchy3_A_C.class, 3 )
		);
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
						.entity( Hierarchy1_A_B.class, 2 )
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
						.entity( Hierarchy4_A_D.class, 4 )
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
						.entity( Hierarchy4_A_C__integer2DocumentId.class, 3 )
		);
	}

	@Override
	protected SessionFactory sessionFactory() {
		return sessionFactory;
	}

	private void initData() {
		// We don't care about what is indexed exactly, so use the lenient mode
		backendMock.inLenientMode( () ->
				OrmUtils.withinTransaction( sessionFactory, session -> {
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
				} )
		);
	}

}
