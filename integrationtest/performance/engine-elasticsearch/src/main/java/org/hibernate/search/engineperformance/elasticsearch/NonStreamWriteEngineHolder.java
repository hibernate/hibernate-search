/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch;

import java.io.IOException;

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

	@Param( { "2;10" } )
	private String maxConnection;

	@Param( { "1000" } )
	private int indexSize;

	private SearchIntegrator si;

	@Setup
	public void initializeState() {
		si = SearchIntegratorHelper.createIntegrator( client, getConnectionInfo(),
				refreshAfterWrite, workerExecution, maxConnection );
	}

	public SearchIntegrator getSearchIntegrator() {
		return si;
	}

	public int getInitialIndexSize() {
		return indexSize;
	}

	@TearDown
	public void shutdownIndexingEngine() throws IOException {
		if ( si != null ) {
			si.close();
		}
	}

}
