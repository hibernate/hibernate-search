/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.configuration.field;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.test.util.AnalyzerUtils;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * Tests related to the {@code Field} annotation and its options
 *
 * @author Hardy Ferentschik
 */
public class TokenizationTest extends SearchTestCase {

	private static final Log log = LoggerFactory.make();
	private static final String DEFAULT_FIELD_NAME = "default";
	private FullTextSession fullTextSession;

	public void testTokenizedAndUntokenizedIndexingIntoSameField() throws Exception {
		indexData();

		fullTextSession = Search.getFullTextSession( sessions.openSession() );
		Transaction tx = fullTextSession.beginTransaction();

		Query luceneQuery = new MatchAllDocsQuery();
		FullTextQuery fullTextQuery = fullTextSession
				.createFullTextQuery( luceneQuery, Product.class )
				.setProjection( ProjectionConstants.DOCUMENT );
		assertEquals( "There should be two documents", 2, fullTextQuery.getResultSize() );

		assertValuesAreIndexedWithDifferentAnalyzeSettings( fullTextQuery );

		// check terms via a term enum
		List<String> tokenTerms = AnalyzerUtils.tokenizedTermValues(
				TestConstants.standardAnalyzer,
				DEFAULT_FIELD_NAME,
				"This is the default product"
		);

		tokenTerms.addAll(
				AnalyzerUtils.tokenizedTermValues(
						TestConstants.standardAnalyzer,
						DEFAULT_FIELD_NAME,
						"This is a magic product"
				)
		);

		// add the untokenized product id
		tokenTerms.add( "1234.5678" );
		tokenTerms.add( "1234.magic" );

		IndexReaderAccessor readerAccessor = fullTextSession.getSearchFactory().getIndexReaderAccessor();
		IndexReader reader = readerAccessor.open( Product.class );
		TermEnum termEnum = reader.terms();
		while ( termEnum.next() ) {
			if ( DEFAULT_FIELD_NAME.equals( termEnum.term().field() ) ) {
				final String term = termEnum.term().text();
				log.debug( "Term: " + term );
				assertTrue( "Unknown term", tokenTerms.contains( term ) );
				tokenTerms.remove( term );
			}
		}
		readerAccessor.close( reader );

		assertTrue( "All terms should have been found", tokenTerms.isEmpty() );

		// use some Term queries
		runAndAssertTermQuery( "1234.5678", 1, "1234.5678" );
		runAndAssertTermQuery( "1234.magic", 1, "1234.magic" );
		runAndAssertTermQuery( "product", 2, "1234.magic", "1234.5678" );
		runAndAssertTermQuery( "default", 1, "1234.magic" );
		runAndAssertTermQuery( "magic", 1, "1234.5678" );

		tx.commit();
	}

	private void runAndAssertTermQuery(String queryTerm, int numberOfResults, String... productIds) {
		FullTextQuery fullTextQuery;
		TermQuery termQuery = new TermQuery( new Term( DEFAULT_FIELD_NAME, queryTerm ) );
		fullTextQuery = fullTextSession.createFullTextQuery( termQuery );
		fullTextQuery.setSort( new Sort( new SortField( DEFAULT_FIELD_NAME, SortField.STRING ) ) );
		assertEquals( "Wrong number of results", numberOfResults, fullTextQuery.getResultSize() );
		for ( int i = 0; i < numberOfResults; i++ ) {
			Product product = (Product) fullTextQuery.list().get( i );
			assertEquals( "wrong product", productIds[i], product.getProductId() );
		}
	}

	private void assertValuesAreIndexedWithDifferentAnalyzeSettings(FullTextQuery fullTextQuery) {
		// lets look at one of the indexed documents and make sure values are indexed as expected
		Object[] projectionArray = (Object[]) fullTextQuery.list().get( 0 );
		Document doc = (Document) projectionArray[0];

		Fieldable[] fields = doc.getFieldables( DEFAULT_FIELD_NAME );
		assertEquals( "Two fields should have been added", 2, fields.length );

		boolean fieldOneIsTokenized = fields[0].isTokenized();
		assertEquals(
				"The two fields should have different tokenization settings",
				!fieldOneIsTokenized,
				fields[1].isTokenized()
		);
	}

	private void indexData() {
		openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();

			Product productOne = new Product();
			productOne.setProductId( "1234.magic" );
			productOne.setDescription( "This is the default product" );
			session.persist( productOne );

			Product productTwo = new Product();
			productTwo.setProductId( "1234.5678" );
			productTwo.setDescription( "This is a magic product" );
			session.persist( productTwo );

			tx.commit();
		}
		catch ( Throwable t ) {
			if ( tx != null ) {
				tx.rollback();
			}
		}
		finally {
			session.close();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Product.class };
	}

	@Entity
	@Indexed
	public static class Product {
		@Id
		@GeneratedValue
		private long id;

		@Field(name = DEFAULT_FIELD_NAME, index = Index.YES, analyze = Analyze.NO, store = Store.YES)
		private String productId;

		@Field(name = DEFAULT_FIELD_NAME, index = Index.YES, analyze = Analyze.YES, store = Store.YES)
		private String description;

		public String getProductId() {
			return productId;
		}

		public void setProductId(String productId) {
			this.productId = productId;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}
