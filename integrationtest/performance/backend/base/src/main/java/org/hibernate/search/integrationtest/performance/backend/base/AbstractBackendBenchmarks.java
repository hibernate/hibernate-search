/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.base;

import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.AbstractBackendHolder;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.IndexInitializer;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.PerThreadIndexPartition;

import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.ThreadParams;

@State(Scope.Thread)
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
