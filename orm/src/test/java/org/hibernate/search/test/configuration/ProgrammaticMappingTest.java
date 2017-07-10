/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.bridge.builtin.ShortBridge;
import org.hibernate.search.bridge.util.impl.BridgeAdaptor;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.spatial.SpatialQueryBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.ElasticsearchSupportInProgress;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Emmanuel Bernard
 */
public class ProgrammaticMappingTest extends SearchTestBase {

	private static final Log log = LoggerFactory.make();

	@Test
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

		QueryParser parser = new QueryParser( "id", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "" + address.getAddressId() );
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

	@Test
	public void testNumeric() throws Exception {
		assertEquals(
				NumericFieldBridge.SHORT_FIELD_BRIDGE,
				getUnwrappedBridge( Item.class , "price", NumericFieldBridge.class )
				);

		assertNotNull(
				getUnwrappedBridge( Item.class , "price_string", ShortBridge.class )
				);
	}

	private Object getUnwrappedBridge(Class<?> clazz, String string, Class<?> expectedBridgeClass) {
		FieldBridge bridge = getExtendedSearchIntegrator().getIndexBindings().get( clazz ).getDocumentBuilder()
				.getTypeMetadata().getDocumentFieldMetadataFor( string ).getFieldBridge();
		return unwrapBridge( bridge, expectedBridgeClass );
	}

	private Object unwrapBridge(Object bridge, Class<?> expectedBridgeClass) {
		if ( bridge instanceof BridgeAdaptor ) {
			return ((BridgeAdaptor) bridge).unwrap( expectedBridgeClass );
		}
		else {
			return bridge;
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2651")
	public void testNumericDoesNotDuplicateField() throws Exception {
		/*
		 * Test that the store(Store.YES) call was taken into account;
		 * it used not to be, because the call to numericField used to
		 * create another, duplicate field, erasing all previous information.
		 */
		TypeMetadata metadata = getExtendedSearchIntegrator().getIndexBindings().get( Item.class )
				.getDocumentBuilder().getTypeMetadata();

		assertTrue( metadata.getDocumentFieldMetadataFor( "price" ).isNumeric() );
		assertEquals( Store.YES, metadata.getDocumentFieldMetadataFor( "price" ).getStore() );
	}

	@Test
	public void testSortableField() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Item item1 = new Item();
		item1.setId( 3 );
		item1.setPrice( (short) 3454 );
		s.persist( item1 );

		Item item2 = new Item();
		item2.setId( 2 );
		item2.setPrice( (short) 3354 );
		s.persist( item2 );

		Item item3 = new Item();
		item3.setId( 1 );
		item3.setPrice( (short) 3554 );
		s.persist( item3 );

		tx.commit();
		s.clear();

		tx = s.beginTransaction();

		Query q = s.getSearchFactory().buildQueryBuilder().forEntity( Item.class ).get().all().createQuery();
		FullTextQuery query = s.createFullTextQuery( q, Item.class );
		query.setSort( new Sort( new SortField( "price", SortField.Type.INT ) ) );

		List<?> results = query.list();
		assertThat( results ).onProperty( "price" )
			.describedAs( "Sortable field via programmatic config" )
			.containsExactly( (short) 3354, (short) 3454, (short) 3554 );

		query.setSort( new Sort( new SortField( "id", SortField.Type.STRING ) ) );

		results = query.list();
		assertThat( results ).onProperty( "id" )
			.describedAs( "Sortable field via programmatic config" )
			.containsExactly( 1, 2, 3 );

		s.delete( results.get( 0 ) );
		s.delete( results.get( 1 ) );
		s.delete( results.get( 2 ) );
		tx.commit();
		s.close();
	}

	@Test
	public void testFacet() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Item item1 = new Item();
		item1.setId( 3 );
		item1.setPrice( (short) 25 );
		s.persist( item1 );

		Item item2 = new Item();
		item2.setId( 2 );
		item2.setPrice( (short) 35 );
		s.persist( item2 );

		Item item3 = new Item();
		item3.setId( 1 );
		item3.setPrice( (short) 825 );
		s.persist( item3 );

		Item item4 = new Item();
		item4.setId( 4 );
		item4.setPrice( (short) 2089 );
		s.persist( item4 );

		tx.commit();
		s.clear();

		tx = s.beginTransaction();

		QueryBuilder qb = s.getSearchFactory().buildQueryBuilder().forEntity( Item.class ).get();
		Query q = qb.all().createQuery();
		FullTextQuery query = s.createFullTextQuery( q, Item.class );

		FacetingRequest facetingRequest = qb.facet()
				.name( "myFacet" )
				.onField( "price_facet" )
				.range()
						.below( 50.0 ).excludeLimit()
						.from( 50.0 ).to( 1000.0 ).excludeLimit()
						.above( 1000.0 )
				.createFacetingRequest();
		query.getFacetManager().enableFaceting( facetingRequest );

		List<Facet> results = query.list();

		List<Facet> facets = query.getFacetManager().getFacets( "myFacet" );
		assertThat( facets ).onProperty( "value" )
				.describedAs( "Retrieved facets - values" )
				.containsExactly( "[, 50.0)", "[50.0, 1000.0)", "[1000.0, ]" );
		assertThat( facets ).onProperty( "count" )
				.describedAs( "Retrieved facets - counts" )
				.containsExactly( 2, 1, 1 );

		query.setSort( new Sort( new SortField( "id", SortField.Type.STRING ) ) );

		s.delete( results.get( 0 ) );
		s.delete( results.get( 1 ) );
		s.delete( results.get( 2 ) );
		s.delete( results.get( 3 ) );
		tx.commit();
		s.close();
	}

	@Test
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

		QueryParser parser = new QueryParser( "id", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "street1_ngram:pea" );

		final FullTextQuery query = s.createFullTextQuery( luceneQuery );
		assertEquals( "Analyzer inoperant", 1, query.getResultSize() );

		s.delete( query.list().get( 0 ) );
		tx.commit();
		s.close();
	}

	@Test
	public void testNormalizerDef() throws Exception {
		Address address = new Address();
		address.setStreet1( "3340 Peachtree Rd NE" );
		address.setStreet2( "JBoss" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( address );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		org.apache.lucene.search.Query luceneQuery = new TermQuery( new Term( "street1_normalized", "3340 peachtree rd ne" ) );

		final FullTextQuery query = s.createFullTextQuery( luceneQuery );
		assertEquals( "Normalizer inoperant", 1, query.getResultSize() );

		s.delete( query.list().get( 0 ) );
		tx.commit();
		s.close();
	}

	@Test
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

		QueryParser parser = new QueryParser( "id", TestConstants.standardAnalyzer );
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

	@Test
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

		QueryParser parser = new QueryParser( "id", TestConstants.standardAnalyzer );
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

	@Test
	@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2428 Provide an alternative to org.hibernate.search.analyzer.Discriminator for Elasticsearch?
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

	@Test
	public void testDateBridgeMapping() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Address address = new Address();
		address.setStreet1( "Peachtree Rd NE" );
		address.setStreet2( "Peachtnot Rd NE" );
		Calendar c = GregorianCalendar.getInstance( TimeZone.getTimeZone( "GMT" ), Locale.ROOT ); //for the sake of tests
		c.set( 2009, Calendar.NOVEMBER, 15 );

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

		long searchTimeStamp = DateTools.round( date.getTime(), DateTools.Resolution.DAY );
		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
		booleanQueryBuilder.add(
				NumericRangeQuery.newLongRange(
						"date-created", searchTimeStamp, searchTimeStamp, true, true
				), BooleanClause.Occur.SHOULD
		);
		booleanQueryBuilder.add(
				NumericRangeQuery.newLongRange(
						"blog-entry-created", searchTimeStamp, searchTimeStamp, true, true
				), BooleanClause.Occur.SHOULD
		);

		FullTextQuery query = s.createFullTextQuery( booleanQueryBuilder.build() )
				.setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting 3 results", 3, query.getResultSize() );

		@SuppressWarnings( "unchecked" )
		List<Object[]> results = query.list();

		for ( Object[] result : results ) {
			s.delete( result[0] );
		}
		tx.commit();
		s.close();
	}

	@Test
	public void testCalendarBridgeMapping() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Address address = new Address();
		address.setStreet1( "Peachtree Rd NE" );
		address.setStreet2( "Peachtnot Rd NE" );
		Calendar calendar = GregorianCalendar.getInstance( TimeZone.getTimeZone( "GMT" ), Locale.ROOT ); //for the sake of tests
		calendar.set( 2009, Calendar.NOVEMBER, 15 );

		address.setLastUpdated( calendar );
		s.persist( address );

		address = new Address();
		address.setStreet1( "Peachtnot Rd NE" );
		address.setStreet2( "Peachtree Rd NE" );
		address.setLastUpdated( calendar );
		s.persist( address );

		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		long searchTimeStamp = DateTools.round( calendar.getTime().getTime(), DateTools.Resolution.DAY );
		org.apache.lucene.search.Query luceneQuery = NumericRangeQuery.newLongRange(
				"last-updated", searchTimeStamp, searchTimeStamp, true, true
		);

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

	@Test
	public void testProvidedIdMapping() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		SearchIntegrator sf = fullTextSession.getSearchFactory().unwrap( SearchIntegrator.class );

		ProvidedIdEntry person1 = new ProvidedIdEntry();
		person1.setName( "Big Goat" );
		person1.setBlurb( "Eats grass" );

		ProvidedIdEntry person2 = new ProvidedIdEntry();
		person2.setName( "Mini Goat" );
		person2.setBlurb( "Eats cheese" );

		ProvidedIdEntry person3 = new ProvidedIdEntry();
		person3.setName( "Regular goat" );
		person3.setBlurb( "Is anorexic" );

		SearchITHelper helper = new SearchITHelper( () -> sf );

		helper.index()
				.push( person1, 1 )
				.push( person2, 2 )
				.push( person3, 3 )
				.execute();

		Transaction transaction = fullTextSession.beginTransaction();

		//we cannot use FTQuery because @ProvidedId does not provide the getter id and Hibernate Hsearch Query extension
		//needs it. So we use plain HSQuery
		helper.assertThat( "providedidentry.name", "goat" )
				.from( ProvidedIdEntry.class )
				.hasResultSize( 3 );

		transaction.commit();
		getSession().close();
	}

	@Test
	public void testFullTextFilterDefAtMappingLevel() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		Address address = new Address();
		address.setStreet1( "Peachtree Rd NE" );
		address.setStreet2( "Peachtnot Rd NE" );
		address.setOwner( "test" );
		Calendar c = GregorianCalendar.getInstance( TimeZone.getTimeZone( "GMT" ), Locale.ROOT ); //for the sake of tests
		c.set( 2009, Calendar.NOVEMBER, 15 );

		address.setLastUpdated( c );
		s.persist( address );

		address = new Address();

		address.setStreet1( "Peachtnot Rd NE" );
		address.setStreet2( "Peachtree Rd NE" );
		address.setLastUpdated( c );
		address.setOwner( "testowner" );
		s.persist( address );

		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "street1:Peachtnot" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		query.enableFullTextFilter( "security" ).setParameter( "ownerName", "testowner" );
		assertEquals( "expecting 1 results", 1, query.getResultSize() );

		@SuppressWarnings( "unchecked" )
		List<Object[]> results = query.list();

		for ( Object[] result : results ) {
			s.delete( result[0] );
		}
		tx.commit();
		s.close();
	}

	@Test
	public void testIndexEmbedded() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		ProductCatalog productCatalog = new ProductCatalog();
		productCatalog.setName( "Cars" );
		Item item = new Item();
		item.setId( 1 );
		item.setDescription( "Ferrari" );
		item.setProductCatalog( productCatalog );
		productCatalog.addItem( item );

		s.persist( item );
		s.persist( productCatalog );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", TestConstants.standardAnalyzer );
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

	@Test
	public void testContainedIn() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		ProductCatalog productCatalog = new ProductCatalog();
		productCatalog.setName( "Cars" );
		Item item = new Item();
		item.setId( 1 );
		item.setDescription( "test" );
		item.setProductCatalog( productCatalog );
		productCatalog.addItem( item );

		s.persist( item );
		s.persist( productCatalog );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "items.description:test" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting 1 results", 1, query.getResultSize() );
		tx.commit();

		tx = s.beginTransaction();

		Item loaded = s.get( Item.class, item.getId() );
		loaded.setDescription( "Ferrari" );

		s.update( loaded );
		tx.commit();

		tx = s.beginTransaction();

		parser = new QueryParser( "id", TestConstants.standardAnalyzer );
		luceneQuery = parser.parse( "items.description:test" );
		query = s.createFullTextQuery( luceneQuery ).setProjection( FullTextQuery.THIS, FullTextQuery.SCORE );
		assertEquals( "expecting 0 results", 0, query.getResultSize() );

		parser = new QueryParser( "id", TestConstants.standardAnalyzer );
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
	@Test
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
		QueryParser parser = new QueryParser( "equipment", new SimpleAnalyzer() );

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
		parser = new QueryParser( "branchHead", new SimpleAnalyzer() );
		query = parser.parse( "branchHead:Kent Lewin" );
		hibQuery = session.createFullTextQuery( query, Departments.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertTrue( "incorrect entity returned, wrong branch head", result.size() == 1 );
		assertEquals( "incorrect entity returned", "Kent Lewin", ( result.get( 0 ) ).getBranchHead() );

		// Check other ClassBridge annotation.
		parser = new QueryParser( "branchnetwork", new SimpleAnalyzer() );
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

	@Test
	@Category(SkipOnElasticsearch.class) // Dynamic boosting is not supported on Elasticsearch
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
		assertEquals( "The scores should be equal", lib1Score, lib2Score, 0f );

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
		assertEquals( "lib1score should be 0 since term is not yet indexed.", 0.0f, lib1Score, 0f );

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

	@Test
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

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "location" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.query.Query hibQuery = session.createFullTextQuery( luceneQuery, MemberLevelTestPoI.class );
		List<?> results = hibQuery.list();
		assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onField( "location" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.query.Query hibQuery2 = session.createFullTextQuery( luceneQuery2, MemberLevelTestPoI.class );
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

		luceneQuery = SpatialQueryBuilder.buildSpatialQueryByHash(
				centerLatitude,
				centerLongitude,
				50,
				"location"
		);
		hibQuery = session.createFullTextQuery( luceneQuery, ClassLevelTestPoI.class );
		results = hibQuery.list();
		assertEquals( 0, results.size() );

		luceneQuery2 = SpatialQueryBuilder.buildSpatialQueryByHash(
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

		luceneQuery = builder.spatial().onField( "location" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		hibQuery = session.createFullTextQuery( luceneQuery, LatLongAnnTestPoi.class );
		results = hibQuery.list();
		assertEquals( 0, results.size() );

		luceneQuery2 = builder.spatial().onField( "location" )
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

	@Test
	public void testClassBridgeInstanceMapping() throws Exception {
		OrderLine orderLine = new OrderLine();
		orderLine.setName( "Sequoia" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( orderLine );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "id", TestConstants.standardAnalyzer );
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
		QueryParser parser = new QueryParser( field, TestConstants.standardAnalyzer );
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
	public void configure(Map<String,Object> cfg) {
		cfg.put( Environment.MODEL_MAPPING, ProgrammaticSearchMappingFactory.class.getName() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
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
