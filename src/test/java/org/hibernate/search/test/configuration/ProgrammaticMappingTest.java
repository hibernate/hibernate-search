/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.configuration;

import java.lang.annotation.ElementType;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.analysis.EnglishPorterFilterFactory;
import org.apache.solr.analysis.GermanStemFilterFactory;
import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.NGramFilterFactory;
import org.apache.solr.analysis.SnowballPorterFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.FilterCacheModeType;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.Work;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.bridge.builtin.LongBridge;
import org.hibernate.search.cfg.ConcatStringBridge;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.analyzer.inheritance.ISOLatin1Analyzer;
import org.hibernate.search.test.id.providedId.ManualTransactionContext;

/**
 * @author Emmanuel Bernard
 */
public class ProgrammaticMappingTest extends SearchTestCase {

	public void testMapping() throws Exception{
		Address address = new Address();
		address.setStreet1( "3340 Peachtree Rd NE" );
		address.setStreet2( "JBoss" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( address );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", new StandardAnalyzer( ) );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "" + address.getAddressId() );
		System.out.println(luceneQuery.toString(  ));
		FullTextQuery query = s.createFullTextQuery( luceneQuery );
		assertEquals( "documenId does not work properly", 1, query.getResultSize() );

		luceneQuery = parser.parse( "street1:peachtree" );
		query = s.createFullTextQuery( luceneQuery ).setProjection( "idx_street2", FullTextQuery.THIS );
		assertEquals( "Not properly indexed", 1, query.getResultSize() );
		Object[] firstResult = (Object[]) query.list().get( 0 );
		assertEquals( "@Field.store not respected", "JBoss", firstResult[0] );

		s.delete( firstResult[1] );
		tx.commit();
		s.close();
	}

	public void testAnalyzerDef() throws Exception{
		Address address = new Address();
		address.setStreet1( "3340 Peachtree Rd NE" );
		address.setStreet2( "JBoss" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( address );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", new StandardAnalyzer( ) );
		org.apache.lucene.search.Query luceneQuery =  parser.parse( "street1_ngram:pea" );

		final FullTextQuery query = s.createFullTextQuery( luceneQuery );
		assertEquals( "Analyzer inoperant", 1, query.getResultSize() );

		s.delete( query.list().get( 0 ));
		tx.commit();
		s.close();

	}

	public void testBridgeMapping() throws Exception{
		Address address = new Address();
		address.setStreet1( "Peachtree Rd NE" );
		address.setStreet2( "JBoss" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( address );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", new StandardAnalyzer( ) );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "street1:peac" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery );
		assertEquals( "PrefixQuery should not be on", 0, query.getResultSize() );

		luceneQuery = parser.parse( "street1_abridged:peac" );
		query = s.createFullTextQuery( luceneQuery );
		assertEquals( "Bridge not used", 1, query.getResultSize() );

		s.delete( query.list().get( 0 ) );
		tx.commit();
		s.close();
	}

	public void testBoost() throws Exception{
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Address address = new Address();
		address.setStreet1( "Peachtree Rd NE" );
		address.setStreet2( "Peachtnot Rd NE" );
		s.persist( address );

		address = new Address();
		address.setStreet1( "Peachtnot Rd NE" );
		address.setStreet2( "Peachtree Rd NE" );
		s.persist( address );

		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", new StandardAnalyzer( ) );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "street1:peachtree OR idx_street2:peachtree" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting two results", 2, query.getResultSize() );

		@SuppressWarnings( "unchecked" )
		List<Object[]> results = query.list();

		assertTrue( "first result should be strictly higher", (Float) results.get( 0 )[1] > (Float) results.get( 1 )[1]*1.9f );
		assertEquals( "Wrong result ordered", address.getStreet1(), ( (Address) results.get( 0 )[0] ).getStreet1() );
		for( Object[] result : results ) {
			s.delete( result[0] );
		}
		tx.commit();
		s.close();
	}

	
	
	
	public void testAnalyzerDiscriminator() throws Exception{
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		BlogEntry deEntry = new BlogEntry();
		deEntry.setTitle( "aufeinanderschl\u00FCgen" );
		deEntry.setDescription( "aufeinanderschl\u00FCgen" );
		deEntry.setLanguage( "de" );
		s.persist( deEntry );
		
		BlogEntry enEntry = new BlogEntry();
		enEntry.setTitle( "acknowledgment" );
		enEntry.setDescription( "acknowledgment" );
		enEntry.setLanguage( "en" );
		s.persist( enEntry );

		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		// at query time we use a standard analyzer. We explicitly search for tokens which can only be found if the
		// right language specific stemmer was used at index time
		assertEquals( 1, nbrOfMatchingResults( "description", "aufeinanderschlug", s ) );
		assertEquals( 1, nbrOfMatchingResults( "description", "acknowledg", s ) );
		assertEquals( 0, nbrOfMatchingResults( "title", "aufeinanderschlug", s ) );
		assertEquals( 1, nbrOfMatchingResults( "title", "acknowledgment", s ) );

		for( Object result : s.createQuery( "from " + BlogEntry.class.getName() ).list() ) {
			s.delete( result );
		}
		tx.commit();
		s.close();
	}

	
	public void testDateBridgeMapping() throws Exception{
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Address address = new Address();
		address.setStreet1( "Peachtree Rd NE" );
		address.setStreet2( "Peachtnot Rd NE" );
		Calendar c = GregorianCalendar.getInstance();
		c.setTimeZone( TimeZone.getTimeZone( "GMT" ) ); //for the sake of tests
		c.set( 2009, Calendar.NOVEMBER, 15);

		Date date = new Date( c.getTimeInMillis() );
		address.setDateCreated(date);
		s.persist( address );

		address = new Address();
		address.setStreet1( "Peachtnot Rd NE" );
		address.setStreet2( "Peachtree Rd NE" );
		address.setDateCreated(date);
		s.persist( address );
		
		BlogEntry enEntry = new BlogEntry();
		enEntry.setTitle( "acknowledgment" );
		enEntry.setDescription( "acknowledgment" );
		enEntry.setLanguage( "en" );
		enEntry.setDateCreated(date);
		s.persist( enEntry );
		
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", new StandardAnalyzer( ) );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "date-created:20091115 OR blog-entry-created:20091115" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting 3 results", 3, query.getResultSize() );

		@SuppressWarnings( "unchecked" )
		List<Object[]> results = query.list();

		for( Object[] result : results ) {
			s.delete( result[0] );
		}
		tx.commit();
		s.close();
	}
	
	public void testCalendarBridgeMapping() throws Exception{
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Address address = new Address();
		address.setStreet1( "Peachtree Rd NE" );
		address.setStreet2( "Peachtnot Rd NE" );
		Calendar c = GregorianCalendar.getInstance();
		c.setTimeZone( TimeZone.getTimeZone( "GMT" ) ); //for the sake of tests
		c.set( 2009, Calendar.NOVEMBER, 15);

		address.setLastUpdated(c);
		s.persist( address );

		address = new Address();
		address.setStreet1( "Peachtnot Rd NE" );
		address.setStreet2( "Peachtree Rd NE" );
		address.setLastUpdated(c);
		s.persist( address );
		
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", new StandardAnalyzer( ) );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "last-updated:20091115" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting 2 results", 2, query.getResultSize() );

		@SuppressWarnings( "unchecked" )
		List<Object[]> results = query.list();

		for( Object[] result : results ) {
			s.delete( result[0] );
		}
		tx.commit();
		s.close();
	}
	
	
	public void testProvidedIdMapping() throws Exception{
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		SearchFactoryImplementor sf = (SearchFactoryImplementor) fullTextSession.getSearchFactory();
		
		ProvidedIdEntry person1 = new ProvidedIdEntry();
		person1.setName( "Big Goat" );
		person1.setBlurb( "Eats grass" );

		ProvidedIdEntry person2 = new ProvidedIdEntry();
		person2.setName( "Mini Goat" );
		person2.setBlurb( "Eats cheese" );

		ProvidedIdEntry person3 = new ProvidedIdEntry();
		person3.setName( "Regular goat" );
		person3.setBlurb( "Is anorexic" );

		ManualTransactionContext tc = new ManualTransactionContext();

		Work<ProvidedIdEntry> work = new Work<ProvidedIdEntry>( person1, 1, WorkType.INDEX );
		sf.getWorker().performWork( work, tc );
		work = new Work<ProvidedIdEntry>( person2, 2, WorkType.INDEX );
		sf.getWorker().performWork( work, tc );
		Work<ProvidedIdEntry> work2 = new Work<ProvidedIdEntry>( person3, 3, WorkType.INDEX );
		sf.getWorker().performWork( work2, tc );

		tc.end();
		
		Transaction transaction = fullTextSession.beginTransaction();

		QueryParser parser = new QueryParser( "providedidentry.name", new StandardAnalyzer() );
		Query luceneQuery = parser.parse( "Goat" );

		//we cannot use FTQuery because @ProvidedId does not provide the getter id and Hibernate Hsearch Query extension
		//needs it. So we use plain Lucene 

		//we know there is only one DP
		DirectoryProvider<?> provider = fullTextSession.getSearchFactory()
				.getDirectoryProviders( ProvidedIdEntry.class )[0];
		IndexSearcher searcher = new IndexSearcher( provider.getDirectory() );
		TopDocs hits = searcher.search( luceneQuery, 1000 );
		searcher.close();
		transaction.commit();
		session.close();

		assertEquals( 3, hits.totalHits );
	}
	
	
	
	public void testFullTextFilterDefAtMappingLevel() throws Exception{
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Address address = new Address();
		address.setStreet1( "Peachtree Rd NE" );
		address.setStreet2( "Peachtnot Rd NE" );
		address.setOwner("test");
		Calendar c = GregorianCalendar.getInstance();
		c.setTimeZone( TimeZone.getTimeZone( "GMT" ) ); //for the sake of tests
		c.set( 2009, Calendar.NOVEMBER, 15);

		address.setLastUpdated(c);
		s.persist( address );

		address = new Address();
		address.setStreet1( "Peachtnot Rd NE" );
		address.setStreet2( "Peachtree Rd NE" );
		address.setLastUpdated(c);
		address.setOwner("test2");
		s.persist( address );
		
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", new StandardAnalyzer( ) );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "street1:Peachtnot" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		query.enableFullTextFilter("security").setParameter("ownerName", "test");
		assertEquals( "expecting 1 results", 1, query.getResultSize() );

		@SuppressWarnings( "unchecked" )
		List<Object[]> results = query.list();

		for( Object[] result : results ) {
			s.delete( result[0] );
		}
		tx.commit();
		s.close();
	}
	
	public void testIndexEmbedded() throws Exception{
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		ProductCatalog productCatalog = new ProductCatalog();
		productCatalog.setName("Cars");
		Item item = new Item();
		item.setDescription("Ferrari");
		item.setProductCatalog(productCatalog);
		productCatalog.addItem(item);
		
		s.persist(item);
		s.persist(productCatalog);
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", new StandardAnalyzer( ) );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "items.description:Ferrari" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting 1 results", 1, query.getResultSize() );

		@SuppressWarnings( "unchecked" )
		List<Object[]> results = query.list();

		for( Object[] result : results ) {
			s.delete( result[0] );
		}
		tx.commit();
		s.close();
	}
	
	public void testContainedIn() throws Exception{
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		ProductCatalog productCatalog = new ProductCatalog();
		productCatalog.setName("Cars");
		Item item = new Item();
		item.setDescription("test");
		item.setProductCatalog(productCatalog);
		productCatalog.addItem(item);
		
		s.persist(item);
		s.persist(productCatalog);
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", new StandardAnalyzer( ) );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "items.description:test" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting 1 results", 1, query.getResultSize() );
		tx.commit();
		
		tx = s.beginTransaction();
		
		Item loaded = (Item) s.get(Item.class, item.getId());
		loaded.setDescription("Ferrari");
		
		s.update(loaded);
		tx.commit();
		
		
		tx = s.beginTransaction();

		parser = new QueryParser( "id", new StandardAnalyzer( ) );
		luceneQuery = parser.parse( "items.description:test" );
		query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting 0 results", 0, query.getResultSize() );

		parser = new QueryParser( "id", new StandardAnalyzer( ) );
		luceneQuery = parser.parse( "items.description:Ferrari" );
		query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting 1 results", 1, query.getResultSize() );
		tx.commit();
		
		tx = s.beginTransaction();
		@SuppressWarnings( "unchecked" )
		List<Object[]> results = query.list();

		for( Object[] result : results ) {
			s.delete( result[0] );
		}
		tx.commit();
		s.close();
	}
	
	private int nbrOfMatchingResults(String field, String token, FullTextSession s) throws ParseException {
		QueryParser parser = new QueryParser( field, new StandardAnalyzer() );
		org.apache.lucene.search.Query luceneQuery = parser.parse( token );
		FullTextQuery query = s.createFullTextQuery( luceneQuery );
		return query.getResultSize();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		//cfg.setProperty( "hibernate.search.default.directory_provider", FSDirectoryProvider.class.getName() );
		SearchMapping mapping = new SearchMapping();

		mapping.fullTextFilterDef("security", SecurityFilterFactory.class).cache(FilterCacheModeType.INSTANCE_ONLY)
				.analyzerDef( "ngram", StandardTokenizerFactory.class )
					.filter( LowerCaseFilterFactory.class )
					.filter( NGramFilterFactory.class )
						.param( "minGramSize", "3" )
						.param( "maxGramSize", "3" )
				.analyzerDef( "en", StandardTokenizerFactory.class )
					.filter( LowerCaseFilterFactory.class )
					.filter( EnglishPorterFilterFactory.class )
				.analyzerDef( "de", StandardTokenizerFactory.class )
					.filter( LowerCaseFilterFactory.class )
					.filter( GermanStemFilterFactory.class )
				.entity( Address.class )
					.indexed()
					.similarity( DefaultSimilarity.class )
					.boost( 2 )
					.property( "addressId", ElementType.FIELD ).documentId().name( "id" )
					.property("lastUpdated", ElementType.FIELD)
						.field().name("last-updated")
								.analyzer("en").store(Store.YES)
								.calendarBridge(Resolution.DAY)
					.property("dateCreated", ElementType.FIELD)
						.field().name("date-created").index(Index.TOKENIZED)
								.analyzer("en").store( Store.YES )
								.dateBridge(Resolution.DAY)
					.property("owner", ElementType.FIELD)
						.field()
					.property( "street1", ElementType.FIELD )
						.field()
						.field().name( "street1_ngram" ).analyzer( "ngram" )
						.field()
							.name( "street1_abridged" )
							.bridge( ConcatStringBridge.class ).param( ConcatStringBridge.SIZE, "4" )
					.property( "street2", ElementType.METHOD )
						.field().name( "idx_street2" ).store( Store.YES ).boost( 2 )
				.entity(ProvidedIdEntry.class).indexed()
						.providedId().name("providedidentry").bridge(LongBridge.class)
						.property("name", ElementType.FIELD)
							.field().name("providedidentry.name").analyzer("en").index(Index.TOKENIZED).store(Store.YES)
						.property("blurb", ElementType.FIELD)
							.field().name("providedidentry.blurb").analyzer("en").index(Index.TOKENIZED).store(Store.YES)
						.property("age", ElementType.FIELD)
							.field().name("providedidentry.age").analyzer("en").index(Index.TOKENIZED).store(Store.YES)
				.entity(ProductCatalog.class).indexed()
					.similarity( DefaultSimilarity.class )
					.boost( 2 )
					.property( "id", ElementType.FIELD ).documentId().name( "id" )
					.property("name", ElementType.FIELD)
						.field().name("productCatalogName").index(Index.TOKENIZED).analyzer("en").store(Store.YES)
					.property("items", ElementType.FIELD)
						.indexEmbedded()
				.entity(Item.class)
						.property("description", ElementType.FIELD)
							.field().name("description").analyzer("en").index(Index.TOKENIZED).store(Store.YES)
						.property("productCatalog", ElementType.FIELD)
							.containedIn()
				.entity( BlogEntry.class ).indexed()
					.property( "title", ElementType.METHOD )
						.field()
					.property( "description", ElementType.METHOD )
						.field()
					.property( "language", ElementType.METHOD )
						.analyzerDiscriminator(BlogEntry.BlogLangDiscriminator.class)
					.property("dateCreated", ElementType.METHOD)
						.field()
							.name("blog-entry-created")
								.analyzer("en")
								.store(Store.YES)
								.dateBridge(Resolution.DAY);
		cfg.getProperties().put( "hibernate.search.mapping_model", mapping );
	}

	public void NotUseddefineMapping() {
		SearchMapping mapping = new SearchMapping();
		mapping.analyzerDef( "stem", StandardTokenizerFactory.class )
					.tokenizerParam( "name", "value" )
					.tokenizerParam(  "name2", "value2" )
					.filter( LowerCaseFilterFactory.class )
					.filter( SnowballPorterFilterFactory.class)
						.param("language", "English")
				.analyzerDef( "ngram", StandardTokenizerFactory.class )
					.tokenizerParam( "name", "value" )
					.tokenizerParam(  "name2", "value2" )
					.filter( LowerCaseFilterFactory.class )
					.filter( NGramFilterFactory.class)
						.param("minGramSize", "3")
						.param("maxGramSize", "3")
				.entity(Address.class).indexed().indexName("Address_Index")
					.property("street1", ElementType.FIELD)
						.field()
						.field()
							.name("street1_iso")
							.store( Store.YES )
							.index( Index.TOKENIZED )
							.analyzer( ISOLatin1Analyzer.class)
						.field()
							.name("street1_ngram")
							.analyzer("ngram")
				.entity(User.class).indexed()
					.property("name", ElementType.METHOD)
						.field()
				.analyzerDef( "minimal", StandardTokenizerFactory.class  );

	}

	protected Class<?>[] getMappings() {
		return new Class<?>[] {
				Address.class,
				Country.class,
				BlogEntry.class,
				ProvidedIdEntry.class,
				ProductCatalog.class,
				Item.class
				
		};
	}
}
