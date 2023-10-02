/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.schema.management;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.util.LuceneIndexContentUtils;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LuceneIndexSchemaManagerCreationIT {

	public static List<? extends Arguments> params() {
		return LuceneIndexSchemaManagerOperation.creating().stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
			root -> root.field( "field", f -> f.asString() )
					.toReference()
	);

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3759")
	void simple(LuceneIndexSchemaManagerOperation operation) throws IOException {
		assertThat( indexExists() ).isFalse();

		setup();
		create( operation );

		assertThat( indexExists() ).isTrue();
	}

	private boolean indexExists() throws IOException {
		return LuceneIndexContentUtils.indexExists( setupHelper, index.name() );
	}

	private void create(LuceneIndexSchemaManagerOperation operation) {
		Futures.unwrappedExceptionJoin( operation.apply( index.schemaManager(), OperationSubmitter.blocking() ) );
	}

	private void setup() {
		setupHelper.start()
				.withIndex( index )
				.setup();
	}
}
