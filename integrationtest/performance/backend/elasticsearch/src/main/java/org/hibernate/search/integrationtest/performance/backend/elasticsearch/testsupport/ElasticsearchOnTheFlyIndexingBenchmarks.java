/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.elasticsearch.testsupport;

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
public class ElasticsearchOnTheFlyIndexingBenchmarks extends AbstractOnTheFlyIndexingBenchmarks {

	@Param({ "FORCE" }) // No need to test "NONE": this parameter is ignored by the Elasticsearch backend anyway.
	private DocumentCommitStrategy commitStrategy;

	@Setup(Level.Trial)
	public void setupTrial(ElasticsearchBackendHolder backendHolder, IndexInitializer indexInitializer,
			ThreadParams threadParams) {
		doSetupTrial( backendHolder, indexInitializer, threadParams );
	}

	@Override
	protected DocumentCommitStrategy getCommitStrategyParam() {
		return commitStrategy;
	}
}
