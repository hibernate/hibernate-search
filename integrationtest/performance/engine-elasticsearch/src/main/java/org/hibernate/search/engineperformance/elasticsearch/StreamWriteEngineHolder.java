/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.IntStream;

import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.engineperformance.elasticsearch.datasets.Dataset;
import org.hibernate.search.engineperformance.elasticsearch.setuputilities.DatasetCreation;
import org.hibernate.search.engineperformance.elasticsearch.setuputilities.SearchIntegratorCreation;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.SearchIntegrator;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Benchmark)
public class StreamWriteEngineHolder extends BaseIndexSetup {

	@Param( { "default", "blackhole-5.4.0" } )
	private String client;

	@Param( { "true", "false" } )
	private boolean refreshAfterWrite;

	@Param( { "100000" } )
	private int indexSize;

	@Param( { DatasetCreation.HIBERNATE_DEV_ML_2016_01 } )
	private String dataset;

	@Param( { "10000" } )
	private int streamedAddsPerFlush;

	private SearchIntegrator si;

	private Dataset data;

	/*
	 * We spawn one engine per iteration,
	 * to ensure we won't end up with huge indexes on iteration 10.
	 */
	@Setup(Level.Iteration)
	public void initializeState() throws IOException, URISyntaxException {
		si = SearchIntegratorCreation.createIntegrator( client, getConnectionInfo(), refreshAfterWrite, null /* irrelevant */ );
		data = DatasetCreation.createDataset( dataset, pickCacheDirectory() );
		SearchIntegratorCreation.preindexEntities( si, data, IntStream.range( 0, indexSize ) );
	}

	@TearDown(Level.Iteration)
	public void shutdownIndexingEngine() throws IOException {
		if ( si != null ) {
			si.close();
		}
	}

	public SearchIntegrator getSearchIntegrator() {
		return si;
	}

	public Dataset getDataset() {
		return data;
	}

	public int getInitialIndexSize() {
		return indexSize;
	}

	public int getStreamedAddsPerFlush() {
		return streamedAddsPerFlush;
	}

	public void flush(IndexedTypeIdentifier typeId) {
		for ( IndexManager indexManager : si.getIndexBinding( typeId ).getIndexManagerSelector().all() ) {
			indexManager.performStreamOperation( new FlushLuceneWork( null, typeId ), null, false );
		}
	}

}
