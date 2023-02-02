/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.schema.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.util.LuceneIndexContentUtils;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

public class LuceneIndexSchemaManagerValidationIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3759")
	public void doesNotExist() throws IOException {
		assertThat( indexExists() ).isFalse();

		setup();

		// The setup currently creates the index: work around that.
		Futures.unwrappedExceptionJoin(
				LuceneIndexSchemaManagerOperation.DROP_IF_EXISTING.apply( index.schemaManager(),
						OperationSubmitter.blocking()
				)
		);

		assertThat( indexExists() ).isFalse();

		assertThatThrownBy( this::validate )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Index does not exist for directory",
						index.name()
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3759")
	public void alreadyExists() throws IOException {
		assertThat( indexExists() ).isFalse();

		setup();

		assertThat( indexExists() ).isTrue();

		validate();

		// No exception was thrown
	}

	private boolean indexExists() throws IOException {
		return LuceneIndexContentUtils.indexExists( setupHelper, index.name() );
	}

	private void validate() {
		Futures.unwrappedExceptionJoin(
				LuceneIndexSchemaManagerOperation.VALIDATE.apply( index.schemaManager(), OperationSubmitter.blocking() )
		);
	}

	private void setup() {
		setupHelper.start()
				.withIndex( index )
				.setup();
	}
}
