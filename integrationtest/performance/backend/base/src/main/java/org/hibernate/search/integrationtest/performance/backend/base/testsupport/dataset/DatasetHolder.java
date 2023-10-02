/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
