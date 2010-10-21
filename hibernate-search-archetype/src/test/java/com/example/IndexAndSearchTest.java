package com.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.util.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.Session;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;

import static org.junit.Assert.assertEquals;

/**
 * Example testcase for Hibernate Search
 */
public class IndexAndSearchTest {

	private EntityManagerFactory emf;

	private EntityManager em;

	private static Logger log = LoggerFactory.getLogger( IndexAndSearchTest.class );

	@Before
	public void setUp() {
		initHibernate();
	}

	@After
	public void tearDown() {
		purge();
	}

	@Test
	public void testIndexAndSearch() throws Exception {
		List<Book> books = search( "hibernate" );
		assertEquals( "Should get empty list since nothing is indexed yet", 0, books.size() );

		index();

		// search by title
		books = search( "hibernate" );
		assertEquals( "Should find one book", 1, books.size() );
		assertEquals( "Wrong title", "Java Persistence with Hibernate", books.get( 0 ).getTitle() );

		// search author
		books = search( "\"Gavin King\"" );
		assertEquals( "Should find one book", 1, books.size() );
		assertEquals( "Wrong title", "Java Persistence with Hibernate", books.get( 0 ).getTitle() );
	}

	@Test
	public void testStemming() throws Exception {

		index();

		List<Book> books = search( "refactor" );
		assertEquals( "Wrong title", "Refactoring: Improving the Design of Existing Code", books.get( 0 ).getTitle() );

		books = search( "refactors" );
		assertEquals( "Wrong title", "Refactoring: Improving the Design of Existing Code", books.get( 0 ).getTitle() );

		books = search( "refactored" );
		assertEquals( "Wrong title", "Refactoring: Improving the Design of Existing Code", books.get( 0 ).getTitle() );

		books = search( "refactoring" );
		assertEquals( "Wrong title", "Refactoring: Improving the Design of Existing Code", books.get( 0 ).getTitle() );
	}


	private void initHibernate() {
		Ejb3Configuration config = new Ejb3Configuration();
		config.configure( "hibernate-search-example", new HashMap() );
		emf = config.buildEntityManagerFactory();
		em = emf.createEntityManager();
	}

	private void index() {
		FullTextSession ftSession = org.hibernate.search.Search.getFullTextSession( ( Session ) em.getDelegate() );
		try {
			ftSession.createIndexer().startAndWait();
		}
		catch ( InterruptedException e ) {
			log.error( "Was interrupted during indexing", e );
		}
	}

	private void purge() {
		FullTextSession ftSession = org.hibernate.search.Search.getFullTextSession( ( Session ) em.getDelegate() );
		ftSession.purgeAll( Book.class );
		ftSession.flushToIndexes();
	}

	private List<Book> search(String searchQuery) throws ParseException {
		Query query = searchQuery( searchQuery );

		List<Book> books = query.getResultList();

		for ( Book b : books ) {
			log.info( "Title: " + b.getTitle() );
		}
		return books;
	}

	private Query searchQuery(String searchQuery) throws ParseException {

		String[] bookFields = { "title", "subtitle", "authors.name", "publicationDate" };

		//lucene part
		Map<String, Float> boostPerField = new HashMap<String, Float>( 4 );
		boostPerField.put( bookFields[0], ( float ) 4 );
		boostPerField.put( bookFields[1], ( float ) 3 );
		boostPerField.put( bookFields[2], ( float ) 4 );
		boostPerField.put( bookFields[3], ( float ) .5 );

		FullTextEntityManager ftEm = org.hibernate.search.jpa.Search.getFullTextEntityManager( ( EntityManager ) em );
		Analyzer customAnalyzer = ftEm.getSearchFactory().getAnalyzer( "customanalyzer" );
		QueryParser parser = new MultiFieldQueryParser(
				Version.LUCENE_29, bookFields,
				customAnalyzer, boostPerField
		);

		org.apache.lucene.search.Query luceneQuery;
		luceneQuery = parser.parse( searchQuery );

		final FullTextQuery query = ftEm.createFullTextQuery( luceneQuery, Book.class );

		return query;
	}

}
