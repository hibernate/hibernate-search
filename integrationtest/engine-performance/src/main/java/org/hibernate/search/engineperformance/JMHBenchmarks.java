/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.engineperformance.model.BookEntity;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Threads;

@Fork(1)
public class JMHBenchmarks {

	@Benchmark
	@Threads(20)
	public void simpleIndexing(EngineHolder eh) {
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

}
