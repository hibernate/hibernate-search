/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.performance.backend.lucene.testsupport;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.integrationtest.performance.backend.base.AbstractOnTheFlyIndexingBenchmarks;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.IndexInitializer;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.ThreadParams;

@State(Scope.Thread)
public class LuceneOnTheFlyIndexingBenchmarks extends AbstractOnTheFlyIndexingBenchmarks {

	@Param({ "NONE", "FORCE" })
	private DocumentCommitStrategy commitStrategy;

	@Setup(Level.Trial)
	public void setupTrial(LuceneBackendHolder backendHolder, IndexInitializer indexInitializer,
			ThreadParams threadParams) {
		doSetupTrial( backendHolder, indexInitializer, threadParams );
	}

	@Override
	protected DocumentCommitStrategy getCommitStrategyParam() {
		return commitStrategy;
	}
}
