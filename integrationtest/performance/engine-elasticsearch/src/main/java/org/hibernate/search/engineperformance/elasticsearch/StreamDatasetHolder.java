/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch;

import java.io.IOException;
import java.net.URISyntaxException;

import org.hibernate.search.engineperformance.elasticsearch.setuputilities.DatasetCreation;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * @author Yoann Rodiere
 */
@State(Scope.Benchmark)
public class StreamDatasetHolder extends BaseDataSetup {

	@Param( { "100000" } )
	private int indexSize;

	@Param( { DatasetCreation.HIBERNATE_DEV_ML_2016_01 } )
	private String dataset;

	@Setup
	public void initializeState() throws IOException, URISyntaxException {
		initializeState( dataset );
	}

	@Setup(Level.Iteration)
	public void initializeIndexes(StreamWriteEngineHolder eh) {
		initializeIndexes( eh.getSearchIntegrator(), indexSize );
	}

	public int getInitialIndexSize() {
		return indexSize;
	}

}
