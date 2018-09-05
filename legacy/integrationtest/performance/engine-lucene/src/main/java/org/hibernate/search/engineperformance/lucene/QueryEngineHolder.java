/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.lucene;

import java.io.IOException;

import org.hibernate.search.engineperformance.lucene.setuputilities.SearchIntegratorCreation;
import org.hibernate.search.spi.SearchIntegrator;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Benchmark)
public class QueryEngineHolder extends BaseIndexSetup {

	public volatile SearchIntegrator si;

	@Param( { "local-heap", "fs", "fs-nrt" } )
	private String directorytype;

	@Param( { "async", "shared" } )
	private String readerStrategy;

	@Param( { "100", "5000000" } )
	private int indexSize;

	@Param( { "100" } )
	private int maxResults;

	@Setup
	public void initializeState() throws IOException {
		si = SearchIntegratorCreation.createIntegrator( directorytype, readerStrategy, pickIndexStorageDirectory() );
		SearchIntegratorCreation.preindexEntities( si, indexSize );
	}

	public int getExpectedIndexSize() {
		return indexSize;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public boolean isQuerySync() {
		return ! "async".equals( readerStrategy );
	}

	@TearDown
	public void shutdownIndexingEngine() throws IOException {
		if ( si != null ) {
			si.close();
		}
		super.cleanup();
	}

}
