/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class LuceneDocumentModelDslIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private StubMappedIndex index;

	@Test
	void unknownAnalyzer() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					ctx.createTypeFactory().asString()
							.analyzer( "someNameThatIsClearlyNotAssignedToADefinition" );
				} ),
				"Referencing an unknown analyzer"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.failure( "Unknown analyzer" ) );
	}

	@Test
	void unknownNormalizer() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					ctx.createTypeFactory().asString()
							.normalizer( "someNameThatIsClearlyNotAssignedToADefinition" );
				} ),
				"Referencing an unknown analyzer"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.failure( "Unknown normalizer" ) );
	}

	@Test
	void unknownSearchAnalyzer() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					ctx.createTypeFactory().asString()
							.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
							.searchAnalyzer( "someNameThatIsClearlyNotAssignedToADefinition" );
				} ),
				"Referencing an unknown search analyzer"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.failure( "Unknown analyzer" ) );
	}

	private void setup(Consumer<IndexBindingContext> mappingContributor) {
		index = StubMappedIndex.ofAdvancedNonRetrievable( mappingContributor );
		setupHelper.start().withIndex( index ).setup();
	}
}
