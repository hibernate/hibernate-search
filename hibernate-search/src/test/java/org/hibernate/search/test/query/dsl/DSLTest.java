package org.hibernate.search.test.query.dsl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.Query;
import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.NGramFilterFactory;
import org.apache.solr.analysis.SnowballPorterFilterFactory;
import org.apache.solr.analysis.StandardFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.apache.solr.analysis.StopFilterFactory;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Emmanuel Bernard
 */
public class DSLTest extends SearchTestCase {

	public void testUseOfFieldBridge() throws Exception {
		FullTextSession fts = initData();

		Transaction transaction = fts.beginTransaction();
		final QueryBuilder monthQb = fts.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();
		Query

		query = monthQb.keyword().onField( "monthValue" ).matching( 2 ).createQuery();
		assertEquals( 1, fts.createFullTextQuery( query, Month.class ).getResultSize() );

		query = monthQb.keyword()
						.onField( "monthValue" )
							.ignoreFieldBridge()
						.matching( "2" )
						.createQuery();
		assertEquals( 1, fts.createFullTextQuery( query, Month.class ).getResultSize() );

		transaction.commit();

		cleanData( fts );
	}

	public void testTermQueryOnAnalyzer() throws Exception {
		FullTextSession fts = initData();

		Transaction transaction = fts.beginTransaction();
		final QueryBuilder monthQb = fts.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();
		Query
		//regular term query
		query = monthQb.keyword().onField( "mythology" ).matching( "cold" ).createQuery();

		assertEquals( 0, fts.createFullTextQuery( query, Month.class ).getResultSize() );

		//term query based on several words
		query = monthQb.keyword().onField( "mythology" ).matching( "colder darker" ).createQuery();

		assertEquals( 1, fts.createFullTextQuery( query, Month.class ).getResultSize() );

		//term query applying the analyzer and generating one term per word
		query = monthQb.keyword().onField( "mythology_stem" ).matching( "snowboard" ).createQuery();

		assertEquals( 1, fts.createFullTextQuery( query, Month.class ).getResultSize() );

		//term query applying the analyzer and generating several terms per word
		query = monthQb.keyword().onField( "mythology_ngram" ).matching( "snobored" ).createQuery();

		assertEquals( 1, fts.createFullTextQuery( query, Month.class ).getResultSize() );

		//term query not using analyzers
		query = monthQb.keyword().onField( "mythology" ).ignoreAnalyzer().matching( "Month" ).createQuery();

		assertEquals( 0, fts.createFullTextQuery( query, Month.class ).getResultSize() );

		transaction.commit();

		cleanData( fts );
	}

	public void testFuzzyAndWildcardQuery() throws Exception {
		FullTextSession fts = initData();

		Transaction transaction = fts.beginTransaction();
		final QueryBuilder monthQb = fts.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();
		Query

		//fuzzy search with custom threshold and prefix
		query = monthQb
				.keyword()
					.fuzzy()
						.withThreshold( .8f )
						.withPrefixLength( 1 )
					.onField( "mythology" )
						.matching( "calder" )
						.createQuery();

		assertEquals( 1, fts.createFullTextQuery( query, Month.class ).getResultSize() );

		//wildcard query
		query = monthQb
				.keyword()
					.wildcard()
					.onField( "mythology" )
						.matching( "mon*" )
						.createQuery();

		assertEquals( 2, fts.createFullTextQuery( query, Month.class ).getResultSize() );

		transaction.commit();

		cleanData( fts );
	}

	public void testQueryCustomization() throws Exception {
		FullTextSession fts = initData();

		Transaction transaction = fts.beginTransaction();
		final QueryBuilder monthQb = fts.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();
		Query

		//combined query, January and february both contain whitening but February in a longer text
		query = monthQb
				.bool()
					.should( monthQb.keyword().onField( "mythology" ).matching( "whitening" ).createQuery() )
					.should( monthQb.keyword().onField( "history" ).matching( "whitening" ).createQuery() )
				.createQuery();

		List<Month> results = fts.createFullTextQuery( query, Month.class ).list();
		assertEquals( 2, results.size() );
		assertEquals( "January", results.get( 0 ).getName() );

		//boosted query, January and february both contain whitening but February in a longer text
		//since history is boosted, February should come first though
		query = monthQb
				.bool()
					.should( monthQb.keyword().onField( "mythology" ).matching( "whitening" ).createQuery() )
					.should( monthQb.keyword().onField( "history" ).boostedTo( 30 ).matching( "whitening" ).createQuery() )
				.createQuery();

		results = fts.createFullTextQuery( query, Month.class ).list();
		assertEquals( 2, results.size() );
		assertEquals( "February", results.get( 0 ).getName() );

		//FIXME add other method tests besides boostedTo

		transaction.commit();

		cleanData( fts );
	}

	public void testMultipleFields() throws Exception {
		FullTextSession fts = initData();

		Transaction transaction = fts.beginTransaction();
		final QueryBuilder monthQb = fts.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();
		Query

		//combined query, January and february both contain whitening but February in a longer text
		query = monthQb.keyword()
						.onField( "mythology" )
						.andField( "history" )
						.matching( "whitening" ).createQuery();

		List<Month> results = fts.createFullTextQuery( query, Month.class ).list();
		assertEquals( 2, results.size() );
		assertEquals( "January", results.get( 0 ).getName() );

		//combined query, January and february both contain whitening but February in a longer text
		query = monthQb.keyword()
						.onFields( "mythology", "history" )
							.boostedTo( 30 )
						.matching( "whitening" ).createQuery();

		results = fts.createFullTextQuery( query, Month.class ).list();
		assertEquals( 2, results.size() );
		assertEquals( "January", results.get( 0 ).getName() );

		//boosted query, January and february both contain whitening but February in a longer text
		//since history is boosted, February should come first though
		query = monthQb.keyword()
							.onField( "mythology" )
							.andField( "history" )
								.boostedTo( 30 )
							.matching( "whitening" )
							.createQuery();

		results = fts.createFullTextQuery( query, Month.class ).list();
		assertEquals( 2, results.size() );
		assertEquals( "February", results.get( 0 ).getName() );

		transaction.commit();

		cleanData( fts );
	}

	public void testBoolean() throws Exception {
		FullTextSession fts = initData();

		Transaction transaction = fts.beginTransaction();
		final QueryBuilder monthQb = fts.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();
		Query

		//must
		query = monthQb
				.bool()
					.must( monthQb.keyword().onField( "mythology" ).matching( "colder" ).createQuery() )
					.createQuery();

		List<Month> results = fts.createFullTextQuery( query, Month.class ).list();
		assertEquals( 1, results.size() );
		assertEquals( "January", results.get( 0 ).getName() );

		//must not + all
		query = monthQb
				.bool()
					.should( monthQb.all().createQuery() )
					.must( monthQb.keyword().onField( "mythology" ).matching( "colder" ).createQuery() )
						.not()
					.createQuery();
		results = fts.createFullTextQuery( query, Month.class ).list();
		assertEquals( 1, results.size() );
		assertEquals( "February", results.get( 0 ).getName() );

		//implicit must not + all (not recommended)
		query = monthQb
				.bool()
					.must( monthQb.keyword().onField( "mythology" ).matching( "colder" ).createQuery() )
						.not()
					.createQuery();
		results = fts.createFullTextQuery( query, Month.class ).list();
		assertEquals( 1, results.size() );
		assertEquals( "February", results.get( 0 ).getName() );

		//all except (recommended)
		query = monthQb
				.all()
					.except( monthQb.keyword().onField( "mythology" ).matching( "colder" ).createQuery() )
					.createQuery();

		results = fts.createFullTextQuery( query, Month.class ).list();
		assertEquals( 1, results.size() );
		assertEquals( "February", results.get( 0 ).getName() );


		transaction.commit();

		cleanData( fts );
	}

	public void testRangeQuery() throws Exception {
		FullTextSession fts = initData();

		Transaction transaction = fts.beginTransaction();
		final QueryBuilder monthQb = fts.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		calendar.set(0 + 1900, 2, 12, 0, 0, 0);
		Date from = calendar.getTime();
		calendar.set(10 + 1900, 2, 12, 0, 0, 0);
		Date to = calendar.getTime();

		Query

		query = monthQb.
				range()
					.onField( "estimatedCreation" )
					.andField( "justfortest" )
						.ignoreFieldBridge().ignoreAnalyzer()
					.from( from )
					.to( to ).excludeLimit()
					.createQuery();

		assertEquals( 1, fts.createFullTextQuery( query, Month.class ).getResultSize() );

		query = monthQb.
				range()
					.onField( "estimatedCreation" )
						.ignoreFieldBridge()
					.andField( "justfortest" )
						.ignoreFieldBridge().ignoreAnalyzer()
					.from( DateTools.dateToString( from, DateTools.Resolution.MINUTE ) )
					.to( DateTools.dateToString( to, DateTools.Resolution.MINUTE ) ).excludeLimit()
					.createQuery();
		assertEquals( 1, fts.createFullTextQuery( query, Month.class ).getResultSize() );

		query = monthQb.
				range()
					.onField( "estimatedCreation" )
					.andField( "justfortest" )
						.ignoreFieldBridge().ignoreAnalyzer()
					.below( to )
					.createQuery();

		FullTextQuery hibQuery = fts.createFullTextQuery( query, Month.class );
		assertEquals( 1, hibQuery.getResultSize() );
		assertEquals( "January", ( (Month) hibQuery.list().get( 0 ) ).getName() );

		query = monthQb.
				range()
					.onField( "estimatedCreation" )
						.ignoreFieldBridge()
					.andField( "justfortest" )
						.ignoreFieldBridge().ignoreAnalyzer()
					.below( DateTools.dateToString( to, DateTools.Resolution.MINUTE ) )
					.createQuery();

		hibQuery = fts.createFullTextQuery( query, Month.class );
		assertEquals( 1, hibQuery.getResultSize() );
		assertEquals( "January", ( (Month) hibQuery.list().get( 0 ) ).getName() );

		query = monthQb.
				range()
					.onField( "estimatedCreation" )
					.andField( "justfortest" )
						.ignoreFieldBridge().ignoreAnalyzer()
					.above( to )
					.createQuery();
		hibQuery = fts.createFullTextQuery( query, Month.class );
		assertEquals( 1, hibQuery.getResultSize() );
		assertEquals( "February", ( (Month) hibQuery.list().get( 0 ) ).getName() );

		query = monthQb.
				range()
					.onField( "estimatedCreation" )
						.ignoreFieldBridge()
					.andField( "justfortest" )
						.ignoreFieldBridge().ignoreAnalyzer()
					.above( DateTools.dateToString( to, DateTools.Resolution.MINUTE ) )
					.createQuery();
		hibQuery = fts.createFullTextQuery( query, Month.class );
		assertEquals( 1, hibQuery.getResultSize() );
		assertEquals( "February", ( (Month) hibQuery.list().get( 0 ) ).getName() );

		transaction.commit();

		cleanData( fts );
	}

	public void testPhraseQuery() throws Exception {
		FullTextSession fts = initData();

		Transaction transaction = fts.beginTransaction();
		final QueryBuilder monthQb = fts.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		Query

		query = monthQb.
				phrase()
					.onField( "mythology" )
					.sentence( "colder and whitening" )
					.createQuery();

		assertEquals( "test exact phrase", 1, fts.createFullTextQuery( query, Month.class ).getResultSize() );

		query = monthQb.
				phrase()
					.onField( "mythology" )
					.sentence( "Month whitening" )
					.createQuery();

		assertEquals( "test slop", 0, fts.createFullTextQuery( query, Month.class ).getResultSize() );

		query = monthQb.
				phrase()
					.withSlop( 1 )
					.onField( "mythology" )
					.sentence( "Month whitening" )
					.createQuery();

//		assertEquals( "test slop", 1, fts.createFullTextQuery( query, Month.class ).getResultSize() );

		query = monthQb.
				phrase()
					.onField( "mythology" )
					.sentence( "whitening" )
					.createQuery();

		assertEquals( "test one term optimization", 1, fts.createFullTextQuery( query, Month.class ).getResultSize() );



		//Does not work as the NGram filter does not seem to be skipping posiional increment between ngrams.
//		query = monthQb
//				.phrase()
//					.onField( "mythology_ngram" )
//					.sentence( "snobored" )
//					.createQuery();
//
//		assertEquals( 1, fts.createFullTextQuery( query, Month.class ).getResultSize() );

		transaction.commit();

		cleanData( fts );
	}


//	public void testTermQueryOnAnalyzer() throws Exception {
//		FullTextSession fts = initData();
//
//		Transaction transaction = fts.beginTransaction();
//		final QueryBuilder monthQb = fts.getSearchFactory()
//				.buildQueryBuilder().forEntity( Month.class ).get();
//		Query
//		//regular term query
//		query = monthQb.term().on( "mythology" ).matches( "cold" ).createQuery();
//
//		assertEquals( 0, fts.createFullTextQuery( query, Month.class ).getResultSize() );
//
//		//term query based on several words
//		query = monthQb.term().on( "mythology" ).matches( "colder darker" ).createQuery();
//
//		assertEquals( 1, fts.createFullTextQuery( query, Month.class ).getResultSize() );
//
//		//term query applying the analyzer and generating one term per word
//		query = monthQb.term().on( "mythology_stem" ).matches( "snowboard" ).createQuery();
//
//		assertEquals( 1, fts.createFullTextQuery( query, Month.class ).getResultSize() );
//
//		//term query applying the analyzer and generating several terms per word
//		query = monthQb.term().on( "mythology_ngram" ).matches( "snobored" ).createQuery();
//
//		assertEquals( 1, fts.createFullTextQuery( query, Month.class ).getResultSize() );
//
//		//term query not using analyzers
//		query = monthQb.term().on( "mythology" ).matches( "Month" ).ignoreAnalyzer().createQuery();
//
//		assertEquals( 0, fts.createFullTextQuery( query, Month.class ).getResultSize() );
//
//		query = monthQb.term().on( "mythology" ).matches( "Month" ).createQuery();
//
//		transaction.commit();
//
//		cleanData( fts );
//	}
//
//	public void testFuzzyAndWildcardQuery() throws Exception {
//		FullTextSession fts = initData();
//
//		Transaction transaction = fts.beginTransaction();
//		final QueryBuilder monthQb = fts.getSearchFactory()
//				.buildQueryBuilder().forEntity( Month.class ).get();
//		Query
//		//fuzzy search with custom threshold and prefix
//		query = monthQb
//				.term().on( "mythology" ).matches( "calder" )
//					.fuzzy()
//						.threshold( .8f )
//						.prefixLength( 1 )
//				.createQuery();
//
//		assertEquals( 1, fts.createFullTextQuery( query, Month.class ).getResultSize() );
//
//		//wildcard query
//		query = monthQb
//				.term().on( "mythology" ).matches( "mon*" )
//					.wildcard()
//				.createQuery();
//		System.out.println(query.toString(  ));
//		assertEquals( 2, fts.createFullTextQuery( query, Month.class ).getResultSize() );
//
//		transaction.commit();
//
//		cleanData( fts );
//	}
//
//	public void testQueryCustomization() throws Exception {
//		FullTextSession fts = initData();
//
//		Transaction transaction = fts.beginTransaction();
//		final QueryBuilder monthQb = fts.getSearchFactory()
//				.buildQueryBuilder().forEntity( Month.class ).get();
//		Query
//
//		//combined query, January and february both contain whitening but February in a longer text
//		query = monthQb
//				.bool()
//					.should( monthQb.term().on( "mythology" ).matches( "whitening" ).createQuery() )
//					.should( monthQb.term().on( "history" ).matches( "whitening" ).createQuery() )
//				.createQuery();
//
//		List<Month> results = fts.createFullTextQuery( query, Month.class ).list();
//		assertEquals( 2, results.size() );
//		assertEquals( "January", results.get( 0 ).getName() );
//
//		//boosted query, January and february both contain whitening but February in a longer text
//		//since history is boosted, February should come first though
//		query = monthQb
//				.bool()
//					.should( monthQb.term().on( "mythology" ).matches( "whitening" ).createQuery() )
//					.should( monthQb.term().on( "history" ).matches( "whitening" ).boostedTo( 30 ).createQuery() )
//				.createQuery();
//
//		results = fts.createFullTextQuery( query, Month.class ).list();
//		assertEquals( 2, results.size() );
//		assertEquals( "February", results.get( 0 ).getName() );
//
//		//FIXME add other method tests besides boostedTo
//
//		transaction.commit();
//
//		cleanData( fts );
//	}

	//FIXME add boolean tests

	private void cleanData(FullTextSession fts) {
		Transaction tx = fts.beginTransaction();
		final List<Month> results = fts.createQuery( "from " + Month.class.getName() ).list();
		assertEquals( 2, results.size() );

		for (Month entity : results) {
			fts.delete( entity );
		}
		tx.commit();
		fts.close();
	}

	private FullTextSession initData() {
		Session session = openSession();
		FullTextSession fts = Search.getFullTextSession( session );
		Transaction tx = fts.beginTransaction();
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		calendar.set(0 + 1900, 2, 12, 0, 0, 0);
		fts.persist( new Month(
				"January",
				1,
				"Month of colder and whitening",
				"Historically colder than any other month in the northern hemisphere",
				 calendar.getTime() ) );
		calendar.set(100 + 1900, 2, 12, 0, 0, 0);
		fts.persist( new Month(
				"February",
				2,
				"Month of snowboarding",
				"Historically, the month where we make babies while watching the whitening landscape",
				calendar.getTime() ) );
		tx.commit();
		fts.clear();
		return fts;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Month.class
		};
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getProperties().put( Environment.MODEL_MAPPING, MappingFactory.class.getName() );
	}

	public static class MappingFactory {
		@Factory
			public SearchMapping build() {
				SearchMapping mapping = new SearchMapping();
			mapping
				.analyzerDef( "stemmer", StandardTokenizerFactory.class )
					.filter( StandardFilterFactory.class )
					.filter( LowerCaseFilterFactory.class )
					.filter( StopFilterFactory.class )
					.filter( SnowballPorterFilterFactory.class )
						.param( "language", "English" )
				.analyzerDef( "ngram", StandardTokenizerFactory.class )
					.filter( StandardFilterFactory.class )
					.filter( LowerCaseFilterFactory.class )
					.filter( StopFilterFactory.class )
					.filter( NGramFilterFactory.class )
						.param( "minGramSize", "3" )
						.param( "maxGramSize", "3" );
			return mapping;
		}
	}
}
