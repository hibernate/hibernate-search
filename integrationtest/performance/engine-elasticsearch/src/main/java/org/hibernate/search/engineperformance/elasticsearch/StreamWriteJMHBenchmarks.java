/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.impl.StreamingOperationDispatcher;
import org.hibernate.search.backend.spi.OperationDispatcher;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.impl.SimpleInitializer;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engineperformance.elasticsearch.model.AbstractBookEntity;
import org.hibernate.search.engineperformance.elasticsearch.setuputilities.SearchIntegratorHelper;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.SearchIntegrator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for stream work execution,
 * which is primarily used for add works in the mass indexer.
 *
 * @author Yoann Rodiere
 */
@Fork(1)
public class StreamWriteJMHBenchmarks {

	@Benchmark
	@GroupThreads(3 * AbstractBookEntity.TYPE_COUNT)
	public void write(StreamWriteEngineHolder eh, StreamAddEntityGenerator generator, StreamWriteCounters counters, Blackhole blackhole) {
		SearchIntegrator si = eh.getSearchIntegrator();
		OperationDispatcher streamingDispatcher = new StreamingOperationDispatcher( si, true /* forceAsync */ );
		IndexedTypeIdentifier typeId = generator.getTypeId();
		DocumentBuilderIndexedEntity docBuilder = si.getIndexBinding( typeId ).getDocumentBuilder();

		InstanceInitializer initializer = SimpleInitializer.INSTANCE;
		ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
		IndexingMonitor monitor = blackhole::consume;

		generator.stream().forEach( book -> {
			Long id = book.getId();
			AddLuceneWork addWork = docBuilder.createAddWork(
					null,
					docBuilder.getTypeIdentifier(),
					book,
					id,
					docBuilder.getIdBridge().objectToString( id ),
					initializer,
					conversionContext
			);
			streamingDispatcher.dispatch( addWork, monitor );
			++counters.add;
		} );

		// Ensure that we'll block until all works have been performed
		SearchIntegratorHelper.flush( si, typeId );
	}

}
