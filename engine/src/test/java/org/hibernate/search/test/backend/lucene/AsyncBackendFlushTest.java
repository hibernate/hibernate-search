/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend.lucene;

import java.util.Collections;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;

import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.search.MatchAllDocsQuery;

import static org.junit.Assert.assertEquals;

/**
 * Tests for flushing a backend in async mode.
 *
 * @author gustavonalle
 */
public class AsyncBackendFlushTest {

	private static final int ENTITIES = 100;

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( Quote.class )
			.withProperty( "hibernate.search.default.worker.execution", "async" );

	@Test
	public void testFlush() throws Exception {
		writeData( sfHolder, 0, ENTITIES / 2 );
		flushIndex();
		assertDocumentsIndexed( ENTITIES / 2 );
		writeData( sfHolder, ENTITIES / 2, ENTITIES );
		flushIndex();
		assertDocumentsIndexed( ENTITIES );
	}

	private void flushIndex() {
		sfHolder.extractIndexManager( Quote.class ).flushAndReleaseResources();
	}

	private void assertDocumentsIndexed(int number) {
		ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();
		HSQuery hsQuery = searchFactory.createHSQuery().luceneQuery( new MatchAllDocsQuery() )
				.targetedEntities( Collections.<Class<?>>singletonList( Quote.class ) );
		assertEquals( number, hsQuery.queryResultSize() );
	}

	private void writeData(SearchFactoryHolder sfHolder, int fromId, int toId) {
		for ( int i = fromId; i < toId; i++ ) {
			Quote quote = new Quote( i, Quote.class.getName() );
			Work work = new Work( quote, i, WorkType.ADD, false );
			TransactionContextForTest tc = new TransactionContextForTest();
			sfHolder.getSearchFactory().getWorker().performWork( work, tc );
			tc.end();
		}
	}

}
