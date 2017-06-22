/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.objectloading.mixedhierarchy;

import java.util.List;
import java.util.Map;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Tests for using object loading with queries spanning across multiple id spaces
 * (i.e. inheritance hierarchies sharing the same id property).
 *
 * @author Gunnar Morling
 */
public class ObjectLoadingCrossHierarchyTest extends SearchTestBase {

	private FullTextSession fullTextSession;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		fullTextSession = Search.getFullTextSession( session );
		indexTestData();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1793")
	public void testQueryingForEntitiesFromDifferentIdSpaces() throws Exception {
		QueryBuilder queryBuilder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( EducationalInstitution.class ).get();

		Query query = queryBuilder.bool()
				// uses @Id Long identifier;
				.should(
						queryBuilder.keyword()
							.onField( "name" )
							.matching( "Southern Florida College of Golf" )
							.createQuery()
				)
				// uses @Id Long identifier, via SINGLE_TABLE shared with College
				.should(
						queryBuilder.keyword()
						.onField( "name" )
						.matching( "St. Lucie Community College" )
						.createQuery()
				)
				// uses @Id Integer id;
				.should(
						queryBuilder.keyword()
							.onField( "name" )
							.matching( "Wogharts" )
							.createQuery()
				)
				// uses @Id Short id; has mapped super-class School, using TABLE_PER_CLASS
				.should(
						queryBuilder.keyword()
							.onField( "name" )
							.matching( "Homestead Elementary School" )
							.createQuery()
				)
				// uses @Id Long identifier; also has mapped super-class School, using TABLE_PER_CLASS
				.should(
						queryBuilder.keyword()
							.onField( "name" )
							.matching( "Cutler Bay High School" )
							.createQuery()
				)
				.createQuery();

		@SuppressWarnings("unchecked")
		List<College> results = fullTextSession.createFullTextQuery( query, EducationalInstitution.class ).list();

		assertThat( results )
				.onProperty( "name" )
				.describedAs( "Can load results originating from different id spaces, using different id types and names" )
				.containsOnly(
				"Southern Florida College of Golf",
				"Wogharts",
				"St. Lucie Community College",
				"Homestead Elementary School",
				"Cutler Bay High School"
		);
	}

	private void indexTestData() {
		Transaction tx = fullTextSession.beginTransaction();

		University wogharts = new University( 1, "Wogharts" );
		fullTextSession.persist( wogharts );

		College golfCollege = new College( 1L, "Southern Florida College of Golf" );
		fullTextSession.persist( golfCollege );

		College stLucieCommunityCollege = new CommunityCollege( 2L, "St. Lucie Community College" );
		fullTextSession.persist( stLucieCommunityCollege );

		School homesteadPrimary = new PrimarySchool( (short) 1, "Homestead Elementary School" );
		fullTextSession.persist( homesteadPrimary );

		School cutlerBayHigh = new HighSchool( 1L, "Cutler Bay High School" );
		fullTextSession.persist( cutlerBayHigh );

		tx.commit();
		fullTextSession.clear();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				EducationalInstitution.class,
				University.class,
				College.class,
				CommunityCollege.class,
				School.class,
				PrimarySchool.class,
				HighSchool.class
		};
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		// make sure the criteria initializer is used, just in case
		cfg.put( Environment.DATABASE_RETRIEVAL_METHOD, "query" );
	}
}
