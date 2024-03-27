/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.performance.backend.elasticsearch.testsupport;

import org.hibernate.search.integrationtest.performance.backend.base.AbstractMassIndexingBenchmarks;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.IndexInitializer;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.ThreadParams;

@State(Scope.Thread)
public class ElasticsearchMassIndexingBenchmarks extends AbstractMassIndexingBenchmarks {

	@Setup(Level.Trial)
	public void setupTrial(ElasticsearchBackendHolder backendHolder, IndexInitializer indexInitializer,
			ThreadParams threadParams) {
		doSetupTrial( backendHolder, indexInitializer, threadParams );
	}

}
