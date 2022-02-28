/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset;

import java.io.IOException;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class DatasetHolder {

	@Param({ Datasets.GREAT_EXPECTATIONS })
	private String dataset;

	private Dataset datasetInstance;

	@Setup(Level.Trial)
	public void setup() throws IOException {
		datasetInstance = Datasets.createDataset( dataset );
	}

	public Dataset getDataset() {
		return datasetInstance;
	}

}
