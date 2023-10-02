/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.schema.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.util.LuceneIndexContentUtils;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class LuceneIndexSchemaManagerDropIfExistingIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
			root -> root.field( "field", f -> f.asString() )
					.toReference()
	);

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3759")
	void doesNotExist() throws IOException {
		assertThat( indexExists() ).isFalse();

		setup();

		// The setup currently creates the index: work around that.
		Futures.unwrappedExceptionJoin(
				LuceneIndexSchemaManagerOperation.DROP_IF_EXISTING.apply( index.schemaManager(),
						OperationSubmitter.blocking()
				)
		);

		assertThat( indexExists() ).isFalse();

		Futures.unwrappedExceptionJoin(
				LuceneIndexSchemaManagerOperation.DROP_IF_EXISTING.apply( index.schemaManager(),
						OperationSubmitter.blocking()
				)
		);

		// No exception was thrown and the index still doesn't exist.
		assertThat( indexExists() ).isFalse();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3759")
	void alreadyExists() throws IOException {
		assertThat( indexExists() ).isFalse();

		setup();
		Futures.unwrappedExceptionJoin(
				LuceneIndexSchemaManagerOperation.CREATE_IF_MISSING.apply( index.schemaManager(),
						OperationSubmitter.blocking()
				)
		);

		assertThat( indexExists() ).isTrue();

		IndexIndexingPlan plan = index.createIndexingPlan();
		plan.add( referenceProvider( "1" ), document -> {} );
		plan.execute( OperationSubmitter.blocking() ).join();

		assertThat( countDocsOnDisk() ).isEqualTo( 1 );

		Futures.unwrappedExceptionJoin(
				LuceneIndexSchemaManagerOperation.DROP_IF_EXISTING.apply( index.schemaManager(),
						OperationSubmitter.blocking()
				)
		);

		assertThat( indexExists() ).isFalse();
	}

	private boolean indexExists() throws IOException {
		return LuceneIndexContentUtils.indexExists( setupHelper, index.name() );
	}

	private int countDocsOnDisk() throws IOException {
		return LuceneIndexContentUtils.readIndex(
				setupHelper, index.name(),
				reader -> reader.getDocCount( MetadataFields.idFieldName() )
		);
	}

	private void setup() {
		setupHelper.start()
				.withIndex( index )
				.setup();
	}
}
