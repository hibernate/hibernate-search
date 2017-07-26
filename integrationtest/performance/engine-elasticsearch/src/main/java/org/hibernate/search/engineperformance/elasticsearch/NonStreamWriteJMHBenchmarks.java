/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch;

import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.engineperformance.elasticsearch.model.AbstractBookEntity;
import org.hibernate.search.engineperformance.elasticsearch.setuputilities.SearchIntegratorHelper;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for non-stream work execution,
 * which is primarily used when doing CRUD operations on the database
 * in the ORM integration.
 *
 * @author Yoann Rodiere
 */
/*
 * Write methods already perform multiple operations,
 * so we could simply run those once,
 * but we don't have individual control over how many times
 * each method is run, and in the concurrent test we want the
 * read methods to be run as long as the writes continue.
 * Thus we don't set "batchSize" here.
 *
 * Note that the single-shot mode won't work,
 * since it doesn't handle auxiliary counters
 * (which are the only meaningful counters).
 */
@Fork(1)
public class NonStreamWriteJMHBenchmarks {

	@Benchmark
	@Threads(3 * AbstractBookEntity.TYPE_COUNT)
	public void write(NonStreamWriteEngineHolder eh, ChangesetGenerator changesetGenerator, NonStreamWriteCounters counters) {
		SearchIntegrator si = eh.getSearchIntegrator();
		Worker worker = si.getWorker();
		IndexedTypeIdentifier typeId = changesetGenerator.getTypeId();

		changesetGenerator.stream().forEach( changeset -> {
			TransactionContextForTest tc = new TransactionContextForTest();
			changeset.toAdd().forEach( book -> {
						Work work = new Work( book, book.getId(), WorkType.ADD );
						worker.performWork( work, tc );
					} );
			changeset.toUpdate().forEach( book -> {
						Work work = new Work( book, book.getId(), WorkType.UPDATE );
						worker.performWork( work, tc );
					});
			changeset.toDelete().forEach( id -> {
						Work work = new Work( typeId, id, WorkType.DELETE );
						worker.performWork( work, tc );
					});
			tc.end();
			++counters.changeset;
		} );

		// Ensure that we'll block until all works have been performed
		SearchIntegratorHelper.flush( si, typeId );
	}

	@Benchmark
	@Threads(6 * AbstractBookEntity.TYPE_COUNT)
	public void queryBooksByBestRating(NonStreamWriteEngineHolder eh, NonStreamQueryParams qp, Blackhole bh) {
		SearchIntegrator searchIntegrator = eh.getSearchIntegrator();
		Class<?> entityType = qp.getEntityType();
		Query luceneQuery = searchIntegrator.buildQueryBuilder()
				.forEntity( entityType )
				.get()
				.all()
				.createQuery();

		int maxResults = qp.getQueryMaxResults();

		HSQuery hsQuery = searchIntegrator.createHSQuery( luceneQuery, entityType );
		hsQuery.sort( new Sort( new SortField( "rating", SortField.Type.FLOAT, true ) ) );
		hsQuery.maxResults( maxResults );
		int queryResultSize = hsQuery.queryResultSize();
		List<EntityInfo> queryEntityInfos = hsQuery.queryEntityInfos();
		bh.consume( queryResultSize );
		bh.consume( queryEntityInfos );
	}

	@Benchmark
	@GroupThreads(2 * AbstractBookEntity.TYPE_COUNT)
	@Group("concurrentReadWriteTest")
	public void readWriteTestWriter(NonStreamWriteEngineHolder eh, ChangesetGenerator changesetGenerator, NonStreamWriteCounters counters) {
		write( eh, changesetGenerator, counters );
	}

	@Benchmark
	@GroupThreads(2 * AbstractBookEntity.TYPE_COUNT)
	@Group("concurrentReadWriteTest")
	public void readWriteTestReader(NonStreamWriteEngineHolder eh, NonStreamQueryParams qp, Blackhole bh) {
		SearchIntegrator searchIntegrator = eh.getSearchIntegrator();
		Class<?> entityType = qp.getEntityType();
		Query luceneQuery = searchIntegrator.buildQueryBuilder()
				.forEntity( entityType )
				.get()
				.all()
				.createQuery();

		int maxResults = qp.getQueryMaxResults();

		HSQuery hsQuery = searchIntegrator.createHSQuery( luceneQuery, entityType );
		hsQuery.maxResults( maxResults );
		int queryResultSize = hsQuery.queryResultSize();
		List<EntityInfo> queryEntityInfos = hsQuery.queryEntityInfos();
		bh.consume( queryEntityInfos );
		bh.consume( queryResultSize );
	}

}
