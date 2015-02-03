/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.lang.annotation.ElementType;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the programmatic configuration of embedded index elements.
 *
 * @author Gunnar Morling
 */
@TestForIssue(jiraKey = "HSEARCH-1764")
public class IndexEmbeddedProgrammaticallyMappedTest {

	@Test
	public void canSetIncludeEmbeddedObjectIdProgrammatically() throws Exception {
		try (FullTextSessionBuilder builder = getFullTextSessionBuilder() ) {
			// given
			builder.fluentMapping()
				.entity( Address.class)
					.indexed()
					.property( "addressId", ElementType.METHOD )
						.documentId()
							.name( "id" )
					.property( "country", ElementType.METHOD )
						.indexEmbedded()
							.includeEmbeddedObjectId( true );

			setupTestData( builder );

			FullTextSession s = builder.openFullTextSession();

			// when
			QueryParser parser = new QueryParser( "id", TestConstants.standardAnalyzer );
			org.apache.lucene.search.Query luceneQuery = parser.parse( "country.id:1" );

			Transaction tx = s.beginTransaction();

			// then
			FullTextQuery query = s.createFullTextQuery( luceneQuery );
			assertEquals( 1, query.getResultSize() );
			assertEquals( "Bob McRobb", ( (Address) query.list().iterator().next() ).getOwner() );

			tx.commit();
			s.close();
		}
	}

	@Test
	public void canSetIndexNullAsProgrammatically() throws Exception {
		try (FullTextSessionBuilder builder = getFullTextSessionBuilder() ) {
			// given
			builder.fluentMapping()
				.entity( Address.class)
					.indexed()
					.property( "addressId", ElementType.METHOD )
						.documentId()
							.name( "id" )
					.property( "country", ElementType.METHOD )
						.indexEmbedded()
							.indexNullAs( IndexedEmbedded.DEFAULT_NULL_TOKEN );

			setupTestData( builder );

			FullTextSession s = builder.openFullTextSession();

			// when
			QueryParser parser = new QueryParser( "id", TestConstants.standardAnalyzer );
			org.apache.lucene.search.Query luceneQuery = parser.parse( "country:" + "_null_" );

			Transaction tx = s.beginTransaction();

			// then
			FullTextQuery query = s.createFullTextQuery( luceneQuery );
			assertEquals( 1, query.getResultSize() );
			assertEquals( "Alice Donellis", ( (Address) query.list().iterator().next() ).getOwner() );

			tx.commit();
			s.close();
		}
	}

	@Test
	public void canSetIncludePathsProgrammatically() throws Exception {
		try (FullTextSessionBuilder builder = getFullTextSessionBuilder() ) {
			// given
			builder.fluentMapping()
				.entity( Address.class)
					.indexed()
					.property( "addressId", ElementType.METHOD )
						.documentId()
							.name( "id" )
					.property( "country", ElementType.METHOD )
						.indexEmbedded()
							.includePaths( "id", "name" )
				.entity( Country.class)
					.indexed()
						.property( "name", ElementType.METHOD )
							.field();

			setupTestData( builder );

			FullTextSession s = builder.openFullTextSession();

			// when
			QueryParser parser = new QueryParser( "id", TestConstants.standardAnalyzer );
			org.apache.lucene.search.Query luceneQuery = parser.parse( "country.id:1" );

			Transaction tx = s.beginTransaction();

			// then
			FullTextQuery query = s.createFullTextQuery( luceneQuery );
			assertEquals( 1, query.getResultSize() );
			assertEquals( "Bob McRobb", ( (Address) query.list().iterator().next() ).getOwner() );

			// when
			luceneQuery = parser.parse( "country.name:Scotland" );

			// then
			query = s.createFullTextQuery( luceneQuery );
			assertEquals( 1, query.getResultSize() );
			assertEquals( "Bob McRobb", ( (Address) query.list().iterator().next() ).getOwner() );

			tx.commit();
			s.close();
		}
	}

	@SuppressWarnings("resource")
	private FullTextSessionBuilder getFullTextSessionBuilder() {
		return new FullTextSessionBuilder()
			.addAnnotatedClass( Address.class )
			.addAnnotatedClass( Country.class );
	}

	public void setupTestData(FullTextSessionBuilder builder) {
		FullTextSession s = builder.openFullTextSession();
		Transaction tx = s.beginTransaction();

		Address bobsPlace = new Address();
		bobsPlace.setOwner( "Bob McRobb" );

		Address alicesPlace = new Address();
		alicesPlace.setOwner( "Alice Donellis" );

		Country scotland = new Country();
		scotland.setName( "Scotland" );
		bobsPlace.setCountry( scotland );
		scotland.getAddresses().add( bobsPlace );

		s.persist( bobsPlace );
		s.persist( alicesPlace );
		s.persist( scotland );

		tx.commit();
		s.close();
	}
}
