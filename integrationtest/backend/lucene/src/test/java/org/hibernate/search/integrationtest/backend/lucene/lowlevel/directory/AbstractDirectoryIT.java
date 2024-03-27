/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.directory;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.function.Function;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class AbstractDirectoryIT {

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	protected static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	protected StubMapping mapping;

	protected final void checkIndexingAndQuerying() {
		IndexIndexingPlan plan = index.createIndexingPlan();
		plan.add( referenceProvider( DOCUMENT_1 ), document -> {
			document.addValue( index.binding().string, "text 1" );
		} );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> {
			document.addValue( index.binding().string, "text 2" );
		} );
		plan.add( referenceProvider( DOCUMENT_3 ), document -> {
			document.addValue( index.binding().string, "text 3" );
		} );
		plan.execute( OperationSubmitter.blocking() ).join();

		// Check that all documents are searchable
		StubMappingScope scope = index.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThatQuery( query ).hasDocRefHitsAnyOrder(
				index.typeName(),
				DOCUMENT_1, DOCUMENT_2, DOCUMENT_3
		);
	}

	protected final void setup(Object directoryType,
			Function<SearchSetupHelper.SetupContext, SearchSetupHelper.SetupContext> additionalConfiguration) {
		mapping = additionalConfiguration.apply(
				setupHelper.start()
						.withIndex( index )
						.withBackendProperty(
								LuceneIndexSettings.DIRECTORY_TYPE, directoryType
						)
		)
				.setup();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field(
					"string",
					f -> f.asString()
			)
					.toReference();
		}
	}
}
