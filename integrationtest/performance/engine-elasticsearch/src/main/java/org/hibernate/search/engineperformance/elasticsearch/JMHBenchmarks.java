/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch;

import java.util.List;
import java.util.Random;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.engineperformance.elasticsearch.datasets.Dataset;
import org.hibernate.search.engineperformance.elasticsearch.model.BookEntity;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

@Fork(1)
public class JMHBenchmarks {

	private static final IndexedTypeIdentifier BOOK_TYPE = new PojoIndexedTypeIdentifier( BookEntity.class );

	@Benchmark
	@Threads(20)
	public void changeset(EngineHolder eh, RandomHolder rh) {
		Worker worker = eh.getSearchIntegrator().getWorker();
		Dataset dataset = eh.getDataset();

		int initialIndexSize = eh.getInitialIndexSize();
		int addDeletesPerChangeset = eh.getAddsDeletesPerChangeset();
		int updatesPerChangeset = eh.getUpdatesPerChangeset();

		Random random = rh.get();

		TransactionContextForTest tc = new TransactionContextForTest();
		random.ints( addDeletesPerChangeset, initialIndexSize, Integer.MAX_VALUE )
				.forEach( id -> {
					BookEntity book = dataset.create( id );
					Work work = new Work( book, id, WorkType.ADD );
					worker.performWork( work, tc );
				} );
		random.ints( updatesPerChangeset, 0, initialIndexSize )
				.forEach( id -> {
					BookEntity book = dataset.create( id );
					Work work = new Work( book, id, WorkType.UPDATE );
					worker.performWork( work, tc );
				});
		random.ints( addDeletesPerChangeset, 0, initialIndexSize )
				.forEach( id -> {
					Work work = new Work( BOOK_TYPE, id, WorkType.DELETE );
					worker.performWork( work, tc );
				});
		tc.end();
	}

	@Benchmark
	@Threads(20)
	public void queryBooksByBestRating(EngineHolder eh, Blackhole bh) {
		SearchIntegrator searchIntegrator = eh.getSearchIntegrator();
		Query luceneQuery = searchIntegrator.buildQueryBuilder()
				.forEntity( BookEntity.class )
				.get()
				.all()
				.createQuery();

		int maxResults = eh.getQueryMaxResults();

		HSQuery hsQuery = searchIntegrator.createHSQuery( luceneQuery, BookEntity.class );
		hsQuery.sort( new Sort( new SortField( "rating", SortField.Type.FLOAT, true ) ) );
		hsQuery.maxResults( maxResults );
		int queryResultSize = hsQuery.queryResultSize();
		List<EntityInfo> queryEntityInfos = hsQuery.queryEntityInfos();
		if ( maxResults != queryEntityInfos.size() ) {
			throw new RuntimeException( "Unexpected resultset size" );
		}
		bh.consume( queryResultSize );
		bh.consume( queryEntityInfos );
	}

	@Benchmark
	@GroupThreads(5)
	@Group("concurrentReadWriteTest")
	public void readWriteTestWriter(EngineHolder eh, RandomHolder rh) {
		changeset( eh, rh );
	}

	@Benchmark
	@GroupThreads(5)
	@Group("concurrentReadWriteTest")
	public void readWriteTestReader(EngineHolder eh, Blackhole bh) {
		SearchIntegrator searchIntegrator = eh.getSearchIntegrator();
		Query luceneQuery = searchIntegrator.buildQueryBuilder()
				.forEntity( BookEntity.class )
				.get()
				.all()
				.createQuery();

		int maxResults = eh.getQueryMaxResults();

		HSQuery hsQuery = searchIntegrator.createHSQuery( luceneQuery, BookEntity.class );
		hsQuery.maxResults( maxResults );
		int queryResultSize = hsQuery.queryResultSize();
		List<EntityInfo> queryEntityInfos = hsQuery.queryEntityInfos();
		bh.consume( queryEntityInfos );
		bh.consume( queryResultSize );
	}

}
