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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.query.engine.QueryTimeoutException;
import org.hibernate.search.query.engine.impl.DocumentExtractorImpl;
import org.hibernate.search.query.engine.impl.LazyQueryState;
import org.hibernate.search.query.engine.impl.QueryFilters;
import org.hibernate.search.query.engine.impl.QueryHits;
import org.hibernate.search.query.engine.impl.TimeoutManagerImpl;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Navin Surtani
 * @author Sanne Grinovero
 */
@Category(SkipOnElasticsearch.class) // This test is specific to Lucene
public class ProvidedIdTest {

	@Rule
	public final SearchFactoryHolder configuration = new SearchFactoryHolder( ProvidedIdPerson.class, ProvidedIdPersonSub.class );

	private final SearchITHelper helper = new SearchITHelper( configuration );

	@Test
	public void testProvidedId() throws Exception {
		ExtendedSearchIntegrator extendedIntegrator = configuration.getSearchFactory();

		ProvidedIdPerson person1 = new ProvidedIdPerson();
		person1.setName( "Big Goat" );
		person1.setBlurb( "Eats grass" );

		ProvidedIdPerson person2 = new ProvidedIdPerson();
		person2.setName( "Mini Goat" );
		person2.setBlurb( "Eats cheese" );

		ProvidedIdPersonSub person3 = new ProvidedIdPersonSub();
		person3.setName( "Regular goat" );
		person3.setBlurb( "Is anorexic" );

		helper.index()
				.push( person1, 1 )
				.push( person2, 2 )
				.push( person3, 3 )
				.execute();

		QueryParser parser = new QueryParser( "name", TestConstants.standardAnalyzer );
		Query luceneQuery = parser.parse( "Goat" );

		//we cannot use FTQuery because @ProvidedId does not provide the getter id and Hibernate Search Query extension
		//needs it. So we use plain Lucene

		IndexReader indexReader = extendedIntegrator.getIndexReaderAccessor().open( ProvidedIdPerson.class );
		IndexSearcher searcher = new IndexSearcher( indexReader );
		TopDocs hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 3, hits.totalHits );

		final Similarity defaultSimilarity = new ClassicSimilarity();

		//follows an example of what Infinispan Query actually needs to resolve a search request:
		LazyQueryState lowLevelSearcher = new LazyQueryState(
				luceneQuery,
				QueryFilters.EMPTY_FILTERSET,
				indexReader,
				defaultSimilarity,
				extendedIntegrator,
				extendedIntegrator.getIndexBindings().values(),
				false,
				false
		);

		QueryHits queryHits = new QueryHits(
				lowLevelSearcher, QueryFilters.EMPTY_FILTERSET, null,
				new TimeoutManagerImpl( luceneQuery, QueryTimeoutException.DEFAULT_TIMEOUT_EXCEPTION_FACTORY, extendedIntegrator.getTimingSource() ),
				null,
				null,
				null
		);
		Set<String> identifiers = new HashSet<String>();
		identifiers.add( "providedId" );
		Map<String, EntityIndexBinding> targetedEntityBindings = new HashMap<>();
		targetedEntityBindings.put( ProvidedIdPerson.class.getName(), extendedIntegrator.getIndexBindings().get( ProvidedIdPerson.class ) );
		targetedEntityBindings.put( ProvidedIdPersonSub.class.getName(), extendedIntegrator.getIndexBindings().get( ProvidedIdPersonSub.class ) );
		DocumentExtractor extractor = new DocumentExtractorImpl(
				queryHits, extendedIntegrator, new String[] { "name" },
				identifiers, false,
				lowLevelSearcher,
				0, 0, //not used in this case
				targetedEntityBindings
		);
		HashSet<String> titles = new HashSet<String>( 3 );
		for ( int id = 0; id < hits.totalHits; id++ ) {
			String projectedTitle = (String) extractor.extract( id ).getProjection()[0];
			assertNotNull( projectedTitle );
			titles.add( projectedTitle );
		}
		assertTrue( titles.contains( "Regular goat" ) );
		assertTrue( titles.contains( "Mini Goat" ) );
		assertTrue( titles.contains( "Big Goat" ) );
		extendedIntegrator.getIndexReaderAccessor().close( indexReader );
	}
}
