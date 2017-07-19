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

import org.hibernate.search.engineperformance.elasticsearch.datasets.Dataset;
import org.hibernate.search.engineperformance.elasticsearch.setuputilities.DatasetCreation;
import org.hibernate.search.engineperformance.elasticsearch.setuputilities.SearchIntegratorHelper;
import org.hibernate.search.spi.SearchIntegrator;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Benchmark)
public class NonStreamWriteEngineHolder extends BaseIndexSetup {

	@Param( { "default", "blackhole-5.4.0" } )
	private String client;

	@Param( { "true", "false" } )
	private boolean refreshAfterWrite;

	@Param( { "sync", "async" } )
	private String workerExecution;

	@Param( { "100000" } )
	private int indexSize;

	@Param( { "100" } )
	private int maxResults;

	@Param( { DatasetCreation.HIBERNATE_DEV_ML_2016_01 } )
	private String dataset;

	@Param( { "1000" } )
	private int changesetsPerFlush;

	/**
	 * Format: (number of add/remove) + "," + (number of updates)
	 * <p>
	 * The two values are squeezed into one parameter so as to
	 * give more control over which combinations will be executed.
	 * For instance you may want to test 5;5 then 500;500,
	 * but 5;500 and 500;5 may not be of interest.
	 */
	@Param( { "10;20" } )
	private String worksPerChangeset;

	private SearchIntegrator si;

	private Dataset data;

	private int addsDeletesPerChangeset;

	private int updatesPerChangeset;

	@Setup
	public void initializeState() throws IOException, URISyntaxException {
		si = SearchIntegratorHelper.createIntegrator( client, getConnectionInfo(), refreshAfterWrite, workerExecution );
		data = DatasetCreation.createDataset( dataset, pickCacheDirectory() );
		SearchIntegratorHelper.preindexEntities( si, data, IntStream.range( 0, indexSize ) );

		String[] worksPerChangesetSplit = worksPerChangeset.split( ";" );
		addsDeletesPerChangeset = Integer.parseInt( worksPerChangesetSplit[0] );
		updatesPerChangeset = Integer.parseInt( worksPerChangesetSplit[1] );
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

	public int getChangesetsPerFlush() {
		return changesetsPerFlush;
	}

	public int getAddsDeletesPerChangeset() {
		return addsDeletesPerChangeset;
	}

	public int getUpdatesPerChangeset() {
		return updatesPerChangeset;
	}

	public int getQueryMaxResults() {
		return maxResults;
	}

	@TearDown
	public void shutdownIndexingEngine() throws IOException {
		if ( si != null ) {
			si.close();
		}
	}

}
