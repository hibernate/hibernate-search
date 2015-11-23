/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance;

import java.io.IOException;

import org.hibernate.search.engineperformance.setuputilities.SearchIntegratorCreation;
import org.hibernate.search.spi.SearchIntegrator;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Benchmark)
public class QueryEngineHolder extends BaseIndexSetup {

	public volatile SearchIntegrator si;

	@Param( { "ram", "fs", "fs-nrt" } )
	private String queryBackend;

	@Param( { "100", "5000000" } )
	private int indexSize;

	@Param( { "100" } )
	private int maxResults;

	@Setup
	public void initializeState() throws IOException {
		si = SearchIntegratorCreation.createIntegrator( queryBackend, pickIndexStorageDirectory() );
		SearchIntegratorCreation.preindexEntities( si, indexSize );
	}

	public int getExpectedIndexSize() {
		return indexSize;
	}

	public int getMaxResults() {
		return maxResults;
	}

	@TearDown
	public void shutdownIndexingEngine() throws IOException {
		if ( si != null ) {
			si.close();
		}
		super.cleanup();
	}

}
