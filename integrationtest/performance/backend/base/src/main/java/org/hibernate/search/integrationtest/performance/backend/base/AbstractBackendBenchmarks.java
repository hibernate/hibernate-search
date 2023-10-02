/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.performance.backend.base;

import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.AbstractBackendHolder;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.IndexInitializer;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.PerThreadIndexPartition;

import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.ThreadParams;

@State(Scope.Thread)
// Use a longer iteration time than the default of 10s:
// backends have background operations that execute every second,
// which could introduce significant errors in 10-second iterations.
@Measurement(time = 30)
public abstract class AbstractBackendBenchmarks {

	private IndexInitializer indexInitializer;
	private PerThreadIndexPartition indexPartition;

	protected void doSetupTrial(AbstractBackendHolder backendHolder, IndexInitializer indexInitializer,
			ThreadParams threadParams) {
		this.indexInitializer = indexInitializer;
		this.indexPartition = new PerThreadIndexPartition( backendHolder, indexInitializer, threadParams );
	}

	@CompilerControl(CompilerControl.Mode.INLINE)
	protected final IndexInitializer getIndexInitializer() {
		return indexInitializer;
	}

	@CompilerControl(CompilerControl.Mode.INLINE)
	protected final PerThreadIndexPartition getIndexPartition() {
		return indexPartition;
	}
}
