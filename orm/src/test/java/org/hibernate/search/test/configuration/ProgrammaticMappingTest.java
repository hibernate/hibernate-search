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
package org.hibernate.search.test.configuration;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.Search;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.spatial.SpatialQueryBuilder;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.test.util.ManualTransactionContext;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class ProgrammaticMappingTest extends SearchTestCase {

	private static final Log log = LoggerFactory.make();

	public void testMapping() throws Exception {
		Address address = new Address();
		address.setStreet1( "3340 Peachtree Rd NE" );
		address.setStreet2( "JBoss" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( address );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "" + address.getAddressId() );
		System.out.println( luceneQuery.toString() );
		FullTextQuery query = s.createFullTextQuery( luceneQuery );
		assertEquals( "documentId does not work properly", 1, query.getResultSize() );

		luceneQuery = parser.parse( "street1:peachtree" );
		query = s.createFullTextQuery( luceneQuery ).setProjection( "idx_street2", FullTextQuery.THIS );
		assertEquals( "Not properly indexed", 1, query.getResultSize() );
		Object[] firstResult = (Object[]) query.list().get( 0 );
		assertEquals( "@Field.store not respected", "JBoss", firstResult[0] );

		// Verify that AddressClassBridge was applied as well:
		luceneQuery = parser.parse( "AddressClassBridge:Applied\\!" );
		assertEquals( 1, s.createFullTextQuery( luceneQuery ).getResultSize() );

		s.delete( firstResult[1] );
		tx.commit();
		s.close();
	}

	public void testNumeric() throws Exception {
		Item item = new Item();
		item.setPrice( 34.54d );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( item );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();

		NumericRangeQuery<Double> q = NumericRangeQuery.newDoubleRange( "price", 34.5d, 34.6d, true, true );
		FullTextQuery query = s.createFullTextQuery( q, Item.class );
		assertEquals( "Numeric field via programmatic config", 1, query.getResultSize() );

		s.delete( query.list().get( 0 ) );
		tx.commit();
		s.close();

	}

	public void testAnalyzerDef() throws Exception {
		Address address = new Address();
		address.setStreet1( "3340 Peachtree Rd NE" );
		address.setStreet2( "JBoss" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( address );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "street1_ngram:pea" );

		final FullTextQuery query = s.createFullTextQuery( luceneQuery );
		assertEquals( "Analyzer inoperant", 1, query.getResultSize() );

		s.delete( query.list().get( 0 ) );
		tx.commit();
		s.close();
	}

	public void testBridgeMapping() throws Exception {
		Address address = new Address();
		address.setStreet1( "Peachtree Rd NE" );
		address.setStreet2( "JBoss" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( address );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
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

	public void testBoost() throws Exception {
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

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "street1:peachtree OR idx_street2:peachtree" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting two results", 2, query.getResultSize() );

		@SuppressWarnings( "unchecked" )
		List<Object[]> results = query.list();

		assertTrue( "first result should be strictly higher", (Float) results.get( 0 )[1] > (Float) results.get( 1 )[1] * 1.9f );
		assertEquals( "Wrong result ordered", address.getStreet1(), ( (Address) results.get( 0 )[0] ).getStreet1() );
		for ( Object[] result : results ) {
			s.delete( result[0] );
		}
		tx.commit();
		s.close();
	}

	public void testAnalyzerDiscriminator() throws Exception {
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

		for ( Object result : s.createQuery( "from " + BlogEntry.class.getName() ).list() ) {
			s.delete( result );
		}
		tx.commit();
		s.close();
	}

	public void testDateBridgeMapping() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Address address = new Address();
		address.setStreet1( "Peachtree Rd NE" );
		address.setStreet2( "Peachtnot Rd NE" );
		Calendar c = GregorianCalendar.getInstance();
		c.setTimeZone( TimeZone.getTimeZone( "GMT" ) ); //for the sake of tests
		c.set( 2009, Calendar.NOVEMBER, 15);

		Date date = new Date( c.getTimeInMillis() );
		address.setDateCreated( date );
		s.persist( address );

		address = new Address();
		address.setStreet1( "Peachtnot Rd NE" );
		address.setStreet2( "Peachtree Rd NE" );
		address.setDateCreated( date );
		s.persist( address );

		BlogEntry enEntry = new BlogEntry();
		enEntry.setTitle( "acknowledgment" );
		enEntry.setDescription( "acknowledgment" );
		enEntry.setLanguage( "en" );
		enEntry.setDateCreated( date );
		s.persist( enEntry );

		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "date-created:20091115 OR blog-entry-created:20091115" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting 3 results", 3, query.getResultSize() );

		@SuppressWarnings( "unchecked" )
		List<Object[]> results = query.list();

		for ( Object[] result : results ) {
			s.delete( result[0] );
		}
		tx.commit();
		s.close();
	}

	public void testCalendarBridgeMapping() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Address address = new Address();
		address.setStreet1( "Peachtree Rd NE" );
		address.setStreet2( "Peachtnot Rd NE" );
		Calendar c = GregorianCalendar.getInstance();
		c.setTimeZone( TimeZone.getTimeZone( "GMT" ) ); //for the sake of tests
		c.set( 2009, Calendar.NOVEMBER, 15);

		address.setLastUpdated( c );
		s.persist( address );

		address = new Address();
		address.setStreet1( "Peachtnot Rd NE" );
		address.setStreet2( "Peachtree Rd NE" );
		address.setLastUpdated( c );
		s.persist( address );

		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "last-updated:20091115" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting 2 results", 2, query.getResultSize() );

		@SuppressWarnings( "unchecked" )
		List<Object[]> results = query.list();

		for ( Object[] result : results ) {
			s.delete( result[0] );
		}
		tx.commit();
		s.close();
	}

	public void testProvidedIdMapping() throws Exception {
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

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "providedidentry.name", TestConstants.standardAnalyzer );
		Query luceneQuery = parser.parse( "Goat" );

		//we cannot use FTQuery because @ProvidedId does not provide the getter id and Hibernate Hsearch Query extension
		//needs it. So we use plain Lucene

		IndexReader indexReader = fullTextSession.getSearchFactory().getIndexReaderAccessor().open( ProvidedIdEntry.class );
		IndexSearcher searcher = new IndexSearcher( indexReader );
		TopDocs hits = searcher.search( luceneQuery, 1000 );
		searcher.close();
		fullTextSession.getSearchFactory().getIndexReaderAccessor().close( indexReader );
		transaction.commit();
		getSession().close();

		assertEquals( 3, hits.totalHits );
	}

	public void testFullTextFilterDefAtMappingLevel() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Address address = new Address();
		address.setStreet1( "Peachtree Rd NE" );
		address.setStreet2( "Peachtnot Rd NE" );
		address.setOwner( "test" );
		Calendar c = GregorianCalendar.getInstance();
		c.setTimeZone( TimeZone.getTimeZone( "GMT" ) ); //for the sake of tests
		c.set( 2009, Calendar.NOVEMBER, 15);

		address.setLastUpdated( c );
		s.persist( address );

		address = new Address();
		address.setStreet1( "Peachtnot Rd NE" );
		address.setStreet2( "Peachtree Rd NE" );
		address.setLastUpdated( c );
		address.setOwner( "test2" );
		s.persist( address );

		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "street1:Peachtnot" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		query.enableFullTextFilter( "security" ).setParameter( "ownerName", "test" );
		assertEquals( "expecting 1 results", 1, query.getResultSize() );

		@SuppressWarnings( "unchecked" )
		List<Object[]> results = query.list();

		for ( Object[] result : results ) {
			s.delete( result[0] );
		}
		tx.commit();
		s.close();
	}

	public void testIndexEmbedded() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		ProductCatalog productCatalog = new ProductCatalog();
		productCatalog.setName( "Cars" );
		Item item = new Item();
		item.setDescription( "Ferrari" );
		item.setProductCatalog( productCatalog );
		productCatalog.addItem( item );

		s.persist( item );
		s.persist( productCatalog );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "items.description:Ferrari" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting 1 results", 1, query.getResultSize() );

		@SuppressWarnings( "unchecked" )
		List<Object[]> results = query.list();

		for ( Object[] result : results ) {
			s.delete( result[0] );
		}
		tx.commit();
		s.close();
	}

	public void testContainedIn() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		ProductCatalog productCatalog = new ProductCatalog();
		productCatalog.setName( "Cars" );
		Item item = new Item();
		item.setDescription( "test" );
		item.setProductCatalog( productCatalog );
		productCatalog.addItem( item );

		s.persist( item );
		s.persist( productCatalog );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "items.description:test" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting 1 results", 1, query.getResultSize() );
		tx.commit();

		tx = s.beginTransaction();

		Item loaded = (Item) s.get( Item.class, item.getId() );
		loaded.setDescription( "Ferrari" );

		s.update( loaded );
		tx.commit();

		tx = s.beginTransaction();

		parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		luceneQuery = parser.parse( "items.description:test" );
		query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting 0 results", 0, query.getResultSize() );

		parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		luceneQuery = parser.parse( "items.description:Ferrari" );
		query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting 1 results", 1, query.getResultSize() );
		tx.commit();

		tx = s.beginTransaction();
		@SuppressWarnings( "unchecked" )
		List<Object[]> results = query.list();

		for ( Object[] result : results ) {
			s.delete( result[0] );
		}
		tx.commit();
		s.close();
	}

	@SuppressWarnings("unchecked")
	public void testClassBridgeMapping() throws Exception {
		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( getDepts1() );
		s.persist( getDepts2() );
		s.persist( getDepts3() );
		s.persist( getDepts4() );
		s.flush();
		tx.commit();

		tx = s.beginTransaction();
		FullTextSession session = Search.getFullTextSession( s );

		// The equipment field is the manufacturer field  in the
		// Departments entity after being massaged by passing it
		// through the EquipmentType class. This field is in
		// the Lucene document but not in the Department entity itself.
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "equipment", new SimpleAnalyzer( TestConstants.getTargetLuceneVersion() ) );

		// Check the second ClassBridge annotation
		Query query = parser.parse( "equiptype:Cisco" );
		org.hibernate.search.FullTextQuery hibQuery = session.createFullTextQuery( query, Departments.class );
		List<Departments> result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "incorrect number of results returned", 2, result.size() );
		for ( Departments d : result ) {
			assertEquals( "incorrect manufacturer", "C", d.getManufacturer() );
		}

		// No data cross-ups.
		query = parser.parse( "branchnetwork:Kent Lewin" );
		hibQuery = session.createFullTextQuery( query, Departments.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertTrue( "problem with field cross-ups", result.size() == 0 );

		// Non-ClassBridge field.
		parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "branchHead", new SimpleAnalyzer( TestConstants.getTargetLuceneVersion() ) );
		query = parser.parse( "branchHead:Kent Lewin" );
		hibQuery = session.createFullTextQuery( query, Departments.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertTrue( "incorrect entity returned, wrong branch head", result.size() == 1 );
		assertEquals( "incorrect entity returned", "Kent Lewin", ( result.get( 0 ) ).getBranchHead() );

		// Check other ClassBridge annotation.
		parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "branchnetwork", new SimpleAnalyzer( TestConstants.getTargetLuceneVersion() ) );
		query = parser.parse( "branchnetwork:st. george 1D" );
		hibQuery = session.createFullTextQuery( query, Departments.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "incorrect entity returned, wrong network", "1D", ( result.get( 0 ) ).getNetwork() );
		assertEquals( "incorrect entity returned, wrong branch", "St. George", ( result.get( 0 ) ).getBranch() );
		assertEquals( "incorrect number of results returned", 1, result.size() );

		//cleanup
		for ( Object element : s.createQuery( "from " + Departments.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	public void testDynamicBoosts() throws Exception {

		Session session = openSession();
		session.beginTransaction();

		DynamicBoostedDescLibrary lib1 = new DynamicBoostedDescLibrary();
		lib1.setName( "one" );
		session.persist( lib1 );

		DynamicBoostedDescLibrary lib2 = new DynamicBoostedDescLibrary();
		lib2.setName( "two" );
		session.persist( lib2 );

		session.getTransaction().commit();
		session.close();

		float lib1Score = getScore( new TermQuery( new Term( "name", "one" ) ) );
		float lib2Score = getScore( new TermQuery( new Term( "name", "two" ) ) );
		assertEquals( "The scores should be equal", lib1Score, lib2Score );

		// set dynamic score and reindex!
		session = openSession();
		session.beginTransaction();

		session.refresh( lib2 );
		lib2.setDynScore( 2.0f );

		session.getTransaction().commit();
		session.close();

		lib1Score = getScore( new TermQuery( new Term( "name", "one" ) ) );
		lib2Score = getScore( new TermQuery( new Term( "name", "two" ) ) );
		assertTrue( "lib2score should be greater than lib1score", lib1Score < lib2Score );



		lib1Score = getScore( new TermQuery( new Term( "name", "foobar" ) ) );
		assertEquals( "lib1score should be 0 since term is not yet indexed.", 0.0f, lib1Score );

		// index foobar
		session = openSession();
		session.beginTransaction();

		session.refresh( lib1 );
		lib1.setName( "foobar" );

		session.getTransaction().commit();
		session.close();

		lib1Score = getScore( new TermQuery( new Term( "name", "foobar" ) ) );
		lib2Score = getScore( new TermQuery( new Term( "name", "two" ) ) );
		assertTrue( "lib1score should be greater than lib2score", lib1Score > lib2Score );
	}

	public void testSpatial() {
		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		MemberLevelTestPoI memberLevelTestPoI = new MemberLevelTestPoI( "test", 24.0, 32.0d );
		s.persist( memberLevelTestPoI );
		s.flush();
		tx.commit();

		tx = s.beginTransaction();
		FullTextSession session = Search.getFullTextSession( s );

		QueryBuilder builder = session.getSearchFactory()
				.buildQueryBuilder().forEntity( MemberLevelTestPoI.class ).get();

		double centerLatitude = 24;
		double centerLongitude = 31.5;

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onCoordinates( "location" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery = session.createFullTextQuery( luceneQuery, MemberLevelTestPoI.class );
		List<?> results = hibQuery.list();
		assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onCoordinates( "location" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery2 = session.createFullTextQuery( luceneQuery2, MemberLevelTestPoI.class );
		List<?> results2 = hibQuery2.list();
		assertEquals( 1, results2.size() );

		List<?> testPoIs = session.createQuery( "from " + MemberLevelTestPoI.class.getName() ).list();
		for ( Object entity : testPoIs ) {
			session.delete( entity );
		}
		tx.commit();
		session.close();

		s = openSession();
		tx = s.beginTransaction();
		ClassLevelTestPoI classLevelTestPoI = new ClassLevelTestPoI( "test", 24.0, 32.0d );
		s.persist( classLevelTestPoI );
		s.flush();
		tx.commit();

		tx = s.beginTransaction();
		session = Search.getFullTextSession( s );

		builder = session.getSearchFactory()
				.buildQueryBuilder().forEntity( ClassLevelTestPoI.class ).get();

		centerLatitude = 24;
		centerLongitude = 31.5;

		luceneQuery = SpatialQueryBuilder.buildSpatialQueryByQuadTree(
				centerLatitude,
				centerLongitude,
				50,
				"location"
		);
		hibQuery = session.createFullTextQuery( luceneQuery, ClassLevelTestPoI.class );
		results = hibQuery.list();
		assertEquals( 0, results.size() );

		luceneQuery2 = SpatialQueryBuilder.buildSpatialQueryByQuadTree(
				centerLatitude,
				centerLongitude,
				51,
				"location"
		);
		hibQuery2 = session.createFullTextQuery( luceneQuery2, ClassLevelTestPoI.class );
		results2 = hibQuery2.list();
		assertEquals( 1, results2.size() );

		testPoIs = session.createQuery( "from " + ClassLevelTestPoI.class.getName() ).list();
		for ( Object entity : testPoIs ) {
			session.delete( entity );
		}
		tx.commit();
		session.close();

		s = openSession();
		tx = s.beginTransaction();
		LatLongAnnTestPoi latLongAnnTestPoi = new LatLongAnnTestPoi( "test", 24.0, 32.0d );
		s.persist( latLongAnnTestPoi );
		s.flush();
		tx.commit();
		tx = s.beginTransaction();
		session = Search.getFullTextSession( s );

		builder = session.getSearchFactory()
				.buildQueryBuilder().forEntity( LatLongAnnTestPoi.class ).get();

		centerLatitude = 24;
		centerLongitude = 31.5;

		luceneQuery = builder.spatial().onCoordinates( "location" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		hibQuery = session.createFullTextQuery( luceneQuery, LatLongAnnTestPoi.class );
		results = hibQuery.list();
		assertEquals( 0, results.size() );

		luceneQuery2 = builder.spatial().onCoordinates( "location" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		hibQuery2 = session.createFullTextQuery( luceneQuery2, LatLongAnnTestPoi.class );
		results2 = hibQuery2.list();
		assertEquals( 1, results2.size() );


		testPoIs = session.createQuery( "from " + LatLongAnnTestPoi.class.getName() ).list();
		for ( Object entity : testPoIs ) {
			session.delete( entity );
		}
		tx.commit();

		session.close();
	}

	public void testClassBridgeInstanceMapping() throws Exception {
		OrderLine orderLine = new OrderLine();
		orderLine.setName( "Sequoia" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( orderLine );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "orderLineName:Sequoia" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery );
		assertEquals( "Bridge not used", 1, query.getResultSize() );

		luceneQuery = parser.parse( "orderLineName_ngram:quo" );
		query = s.createFullTextQuery( luceneQuery );
		assertEquals( "Analyzer configuration not applied", 1, query.getResultSize() );

		luceneQuery = parser.parse( "orderLineNameViaParam:Sequoia" );
		query = s.createFullTextQuery( luceneQuery );
		assertEquals( "Parameter configuration not applied", 1, query.getResultSize() );

		s.delete( query.list().get( 0 ) );
		tx.commit();
		s.close();
	}

	private float getScore(Query query) {
		Session session = openSession();
		Object[] queryResult;
		float score;
		try {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			List<?> resultList = fullTextSession
					.createFullTextQuery( query, DynamicBoostedDescLibrary.class )
					.setProjection( ProjectionConstants.SCORE, ProjectionConstants.EXPLANATION )
					.setMaxResults( 1 )
					.list();

			if ( resultList.size() == 0 ) {
				score = 0.0f;
			}
			else {
				queryResult = (Object[]) resultList.get( 0 );
				score = (Float) queryResult[0];
				String explanation = queryResult[1].toString();
				log.debugf( "score: %f explanation: %s", score, explanation );
			}
		}
		finally {
			session.close();
		}
		return score;
	}

	private int nbrOfMatchingResults(String field, String token, FullTextSession s) throws ParseException {
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), field, TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( token );
		FullTextQuery query = s.createFullTextQuery( luceneQuery );
		return query.getResultSize();
	}


	private Departments getDepts1() {
		Departments depts = new Departments();

		depts.setBranch( "Salt Lake City" );
		depts.setBranchHead( "Kent Lewin" );
		depts.setMaxEmployees( 100 );
		depts.setNetwork( "1A" );
		depts.setManufacturer( "C" );

		return depts;
	}

	private Departments getDepts2() {
		Departments depts = new Departments();

		depts.setBranch( "Layton" );
		depts.setBranchHead( "Terry Poperszky" );
		depts.setMaxEmployees( 20 );
		depts.setNetwork( "2B" );
		depts.setManufacturer( "3" );

		return depts;
	}

	private Departments getDepts3() {
		Departments depts = new Departments();

		depts.setBranch( "West Valley" );
		depts.setBranchHead( "Pat Kelley" );
		depts.setMaxEmployees( 15 );
		depts.setNetwork( "3C" );
		depts.setManufacturer( "D" );

		return depts;
	}

	private Departments getDepts4() {
		Departments depts = new Departments();

		depts.setBranch( "St. George" );
		depts.setBranchHead( "Spencer Stajskal" );
		depts.setMaxEmployees( 10 );
		depts.setNetwork( "1D" );
		depts.setManufacturer( "C" );
		return depts;
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getProperties().put( Environment.MODEL_MAPPING, ProgrammaticSearchMappingFactory.class.getName() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Address.class,
				Country.class,
				BlogEntry.class,
				ProvidedIdEntry.class,
				ProductCatalog.class,
				Item.class,
				Departments.class,
				DynamicBoostedDescLibrary.class,
				MemberLevelTestPoI.class,
				ClassLevelTestPoI.class,
				LatLongAnnTestPoi.class,
				OrderLine.class
		};
	}
}
