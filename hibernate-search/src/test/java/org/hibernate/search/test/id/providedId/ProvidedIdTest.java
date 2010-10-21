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
package org.hibernate.search.test.id.providedId;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

import org.hibernate.search.Environment;
import org.hibernate.search.backend.Work;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.RAMDirectoryProvider;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.ManualTransactionContext;
import org.hibernate.search.test.util.ManualConfiguration;

/**
 * @author Navin Surtani
 */
public class ProvidedIdTest extends junit.framework.TestCase {

	public void testProvidedId() throws Exception {
		final ManualConfiguration configuration = new ManualConfiguration()
				.addClass( ProvidedIdPerson.class )
				.addClass( ProvidedIdPersonSub.class )
				.addProperty( "hibernate.search.default.directory_provider", RAMDirectoryProvider.class.getName() )
				.addProperty(  Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() )
				.addProperty( "hibernate.search.default.transaction.merge_factor", "100" )
				.addProperty( "hibernate.search.default.batch.max_buffered_docs", "1000" );
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

		ManualTransactionContext tc = new ManualTransactionContext();

		Work<ProvidedIdPerson> work = new Work<ProvidedIdPerson>( person1, 1, WorkType.INDEX );
		sf.getWorker().performWork( work, tc );
		work = new Work<ProvidedIdPerson>( person2, 2, WorkType.INDEX );
		sf.getWorker().performWork( work, tc );
		Work<ProvidedIdPersonSub> work2 = new Work<ProvidedIdPersonSub>( person3, 3, WorkType.INDEX );
		sf.getWorker().performWork( work2, tc );

		tc.end();

		QueryParser parser = new QueryParser( SearchTestCase.getTargetLuceneVersion(), "name", SearchTestCase.standardAnalyzer );
		Query luceneQuery = parser.parse( "Goat" );

		//we cannot use FTQuery because @ProvidedId does not provide the getter id and Hibernate Hsearch Query extension
		//needs it. So we use plain Lucene

		//we know there is only one DP
		DirectoryProvider provider = sf
				.getDirectoryProviders( ProvidedIdPerson.class )[0];
		IndexSearcher searcher = new IndexSearcher( provider.getDirectory(), true );
		TopDocs hits = searcher.search( luceneQuery, 1000 );
		assertEquals( 3, hits.totalHits );
		searcher.close();
	}

}
