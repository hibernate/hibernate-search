/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.schema.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.util.LuceneIndexContentUtils;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LuceneIndexSchemaManagerCreationOrPreservationIT {

	public static List<? extends Arguments> params() {
		return LuceneIndexSchemaManagerOperation.creatingOrPreserving().stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3759")
	void doesNotExist(LuceneIndexSchemaManagerOperation operation) throws IOException {
		assertThat( indexExists() ).isFalse();

		setup();
		createOrPreserve( operation );

		assertThat( indexExists() ).isTrue();
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3759")
	void alreadyExists(LuceneIndexSchemaManagerOperation operation) throws IOException {
		assertThat( indexExists() ).isFalse();

		setup();
		createOrPreserve( operation );

		assertThat( indexExists() ).isTrue();

		IndexIndexingPlan plan = index.createIndexingPlan();
		plan.add( referenceProvider( "1" ), document -> {} );
		plan.execute( OperationSubmitter.blocking() ).join();

		assertThat( countDocsOnDisk() ).isEqualTo( 1 );

		createOrPreserve( operation );

		assertThat( indexExists() ).isTrue();
		assertThat( countDocsOnDisk() ).isEqualTo( 1 );
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

	private void createOrPreserve(LuceneIndexSchemaManagerOperation operation) {
		Futures.unwrappedExceptionJoin( operation.apply( index.schemaManager(), OperationSubmitter.blocking() ) );
	}

	private void setup() {
		setupHelper.start()
				.withIndex( index )
				.setup();
	}
}
