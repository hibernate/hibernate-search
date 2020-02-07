/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset;

import java.io.IOException;

import org.hibernate.search.integrationtest.performance.backend.base.testsupport.filesystem.TemporaryFileHolder;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class DatasetHolder {

	@Param({ Datasets.HIBERNATE_DEV_ML_2016_01 })
	private String dataset;

	private Dataset datasetInstance;

	@Setup(Level.Trial)
	public void setup(TemporaryFileHolder temporaryFileHolder) throws IOException {
		datasetInstance = Datasets.createDataset( dataset, temporaryFileHolder.getCacheDirectory() );
	}

	public Dataset getDataset() {
		return datasetInstance;
	}

}
