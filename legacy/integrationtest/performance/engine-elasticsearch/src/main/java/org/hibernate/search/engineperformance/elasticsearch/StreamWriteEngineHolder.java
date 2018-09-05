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

	@Param( { "2;10" } )
	private String maxConnection;

	@Param( { "10000" } )
	private int streamedAddsPerFlush;

	private SearchIntegrator si;

	/*
	 * We spawn one engine per iteration,
	 * to ensure we won't end up with huge indexes on iteration 10.
	 */
	@Setup(Level.Iteration)
	public void initializeState() {
		si = SearchIntegratorHelper.createIntegrator( client, getConnectionInfo(),
				refreshAfterWrite, null /* irrelevant */, maxConnection );
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

	public int getStreamedAddsPerFlush() {
		return streamedAddsPerFlush;
	}

}
