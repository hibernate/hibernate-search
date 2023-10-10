/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.facet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.Tags;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.apache.lucene.search.MatchAllDocsQuery;

/**
 * @author Elmer van Chastelet
 */
@TestForIssue(jiraKey = "HSEARCH-726")
@Tag(Tags.PORTED_TO_SEARCH_6)
class EmbeddedCollectionFacetingTest extends SearchTestBase {
	Author voltaire;
	Author hugo;
	Author moliere;
	Author proust;

	@BeforeEach
	void createTestData() {
		voltaire = new Author();
		voltaire.setName( "Voltaire" );

		hugo = new Author();
		hugo.setName( "Victor Hugo" );

		moliere = new Author();
		moliere.setName( "Moliere" );

		proust = new Author();
		proust.setName( "Proust" );

		Book book1 = new Book();
		book1.setName( "Candide" );
		book1.getAuthors().add( hugo );
		book1.getAuthors().add( voltaire );

		Book book2 = new Book();
		book2.setName( "Amphitryon" );
		book2.getAuthors().add( hugo );
		book2.getAuthors().add( moliere );

		Book book3 = new Book();
		book3.setName( "Hernani" );
		book3.getAuthors().add( hugo );
		book3.getAuthors().add( moliere );

		Session session = openSession();
		Transaction tx = session.beginTransaction();
		session.persist( voltaire );
		session.persist( hugo );
		session.persist( moliere );
		session.persist( proust );
		session.persist( book1 );
		session.persist( book2 );
		session.persist( book3 );

		tx.commit();
		session.close();
	}

	@Test
	void testFacetEmbeddedAndCollections() {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), Book.class );

		QueryBuilder builder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Book.class ).get();
		FacetingRequest facetReq = builder.facet()
				.name( "someFacet" )
				.onField( "authors.name_untokenized" )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.includeZeroCounts( false )
				.maxFacetCount( 10 )
				.createFacetingRequest();

		List<Facet> facets = fullTextQuery.getFacetManager().enableFaceting( facetReq ).getFacets( "someFacet" );
		assertThat( facets ).as( "There should be three facets" ).hasSize( 3 );
		assertFacet( facets.get( 0 ), hugo, 3 );
		assertFacet( facets.get( 1 ), moliere, 2 );
		assertFacet( facets.get( 2 ), voltaire, 1 );

		fullTextSession.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Author.class,
				Book.class
		};
	}

	private void assertFacet(Facet facet, Author expectedAuthor, int expectedCount) {
		assertThat( facet.getValue() ).as( "Wrong facet value" ).isEqualTo( expectedAuthor.getName() );
		assertThat( facet.getCount() ).as( "Wrong facet count" ).isEqualTo( expectedCount );
	}
}
