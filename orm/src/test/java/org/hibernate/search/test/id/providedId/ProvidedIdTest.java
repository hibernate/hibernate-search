/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id.providedId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.engine.QueryTimeoutException;
import org.hibernate.search.query.engine.impl.DocumentExtractorImpl;
import org.hibernate.search.query.engine.impl.LazyQueryState;
import org.hibernate.search.query.engine.impl.QueryHits;
import org.hibernate.search.query.engine.impl.TimeoutManagerImpl;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.test.util.HibernateManualConfiguration;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;

import org.junit.Test;

/**
 * @author Navin Surtani
 * @author Sanne Grinovero
 */
public class ProvidedIdTest {

	@Test
	public void testProvidedId() throws Exception {
		final SearchConfigurationForTest configuration = new HibernateManualConfiguration()
				.addClass( ProvidedIdPerson.class )
				.addClass( ProvidedIdPersonSub.class )
				.addProperty( "hibernate.search.default.directory_provider", "ram" )
				.addProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() )
				.addProperty( "hibernate.search.default.indexwriter.merge_factor", "100" )
				.addProperty( "hibernate.search.default.indexwriter.max_buffered_docs", "1000" );
		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();

		ProvidedIdPerson person1 = new ProvidedIdPerson();
		person1.setName( "Big Goat" );
		person1.setBlurb( "Eats grass" );

		ProvidedIdPerson person2 = new ProvidedIdPerson();
		person2.setName( "Mini Goat" );
		person2.setBlurb( "Eats cheese" );

		ProvidedIdPersonSub person3 = new ProvidedIdPersonSub();
		person3.setName( "Regular goat" );
		person3.setBlurb( "Is anorexic" );

		TransactionContextForTest tc = new TransactionContextForTest();

		Work work = new Work( person1, 1, WorkType.INDEX );
		sf.getWorker().performWork( work, tc );
		work = new Work( person2, 2, WorkType.INDEX );
		sf.getWorker().performWork( work, tc );
		Work work2 = new Work( person3, 3, WorkType.INDEX );
		sf.getWorker().performWork( work2, tc );

		tc.end();

		QueryParser parser = new QueryParser(
				TestConstants.getTargetLuceneVersion(), "name", TestConstants.standardAnalyzer
		);
		Query luceneQuery = parser.parse( "Goat" );

		//we cannot use FTQuery because @ProvidedId does not provide the getter id and Hibernate Search Query extension
		//needs it. So we use plain Lucene

		IndexReader indexReader = sf.getIndexReaderAccessor().open( ProvidedIdPerson.class );
		IndexSearcher searcher = new IndexSearcher( indexReader );
		TopDocs hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 3, hits.totalHits );

		final Similarity defaultSimilarity = new DefaultSimilarity();

		//follows an example of what Infinispan Query actually needs to resolve a search request:
		LazyQueryState lowLevelSearcher = new LazyQueryState( luceneQuery, indexReader, defaultSimilarity, false, false );

		QueryHits queryHits = new QueryHits(
				lowLevelSearcher, null, null,
				new TimeoutManagerImpl( luceneQuery, QueryTimeoutException.DEFAULT_TIMEOUT_EXCEPTION_FACTORY, sf.getTimingSource() ),
				null,
				false,
				null,
				null,
				null,
				null
		);
		Set<String> identifiers = new HashSet<String>();
		identifiers.add( "providedId" );
		Set<Class<?>> targetedClasses = new HashSet<Class<?>>();
		targetedClasses.add( ProvidedIdPerson.class );
		targetedClasses.add( ProvidedIdPersonSub.class );
		DocumentExtractor extractor = new DocumentExtractorImpl(
				queryHits, sf, new String[] { "name" },
				identifiers, false,
				lowLevelSearcher,
				luceneQuery,
				0, 0, //not used in this case
				targetedClasses
		);
		HashSet<String> titles = new HashSet<String>( 3 );
		for ( int id = 0; id < hits.totalHits; id++ ) {
			Long documentId = (Long) extractor.extract( id ).getId();
			String projectedTitle = (String) extractor.extract( id ).getProjection()[0];
			assertNotNull( projectedTitle );
			titles.add( projectedTitle );
		}
		assertTrue( titles.contains( "Regular goat" ) );
		assertTrue( titles.contains( "Mini Goat" ) );
		assertTrue( titles.contains( "Big Goat" ) );
		sf.getIndexReaderAccessor().close( indexReader );
	}
}
