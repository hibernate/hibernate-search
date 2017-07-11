/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.lucene;

import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.engineperformance.lucene.model.BookEntity;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

@Fork(1)
public class JMHBenchmarks {

	@Benchmark
	@Threads(20)
	public void simpleIndexing(QueryEngineHolder eh) {
		Worker worker = eh.si.getWorker();
		BookEntity book = new BookEntity();
		book.setId( 1l );
		book.setText( "Some very long text should be stored here. No, I mean long as in a book." );
		book.setTitle( "Naaa" );

		Work work = new Work( book, book.getId(), WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		worker.performWork( work, tc );
		tc.end();
	}

	@Benchmark
	@Threads(20)
	public void queryBooksByBestRating(QueryEngineHolder eh, Blackhole bh) {
		SearchIntegrator searchIntegrator = eh.si;
		Query luceneQuery = searchIntegrator.buildQueryBuilder()
				.forEntity( BookEntity.class )
				.get()
				.all()
				.createQuery();

		long expectedIndexSize = eh.getExpectedIndexSize();
		int maxResults = eh.getMaxResults();

		HSQuery hsQuery = searchIntegrator.createHSQuery( luceneQuery, BookEntity.class );
		hsQuery.sort( new Sort( new SortField( "rating", SortField.Type.FLOAT, true ) ) );
		hsQuery.maxResults( maxResults );
		int queryResultSize = hsQuery.queryResultSize();
		List<EntityInfo> queryEntityInfos = hsQuery.queryEntityInfos();
		if ( eh.isQuerySync() && queryResultSize != expectedIndexSize ) {
			throw new RuntimeException( "Unexpected index size" );
		}
		if ( maxResults != queryEntityInfos.size() ) {
			throw new RuntimeException( "Unexpected resultset size" );
		}
		bh.consume( queryEntityInfos );
	}

	@Benchmark
	@GroupThreads(5)
	@Group("concurrentReadWriteTest")
	public void readWriteTestWriter(QueryEngineHolder eh) {
		simpleIndexing( eh );
	}

	@Benchmark
	@GroupThreads(5)
	@Group("concurrentReadWriteTest")
	public void readWriteTestReader(QueryEngineHolder eh, Blackhole bh) {
		SearchIntegrator searchIntegrator = eh.si;
		Query luceneQuery = searchIntegrator.buildQueryBuilder()
				.forEntity( BookEntity.class )
				.get()
				.all()
				.createQuery();

		int maxResults = eh.getMaxResults();

		HSQuery hsQuery = searchIntegrator.createHSQuery( luceneQuery, BookEntity.class );
		hsQuery.maxResults( maxResults );
		int queryResultSize = hsQuery.queryResultSize();
		List<EntityInfo> queryEntityInfos = hsQuery.queryEntityInfos();
		bh.consume( queryEntityInfos );
		bh.consume( queryResultSize );
	}

}
