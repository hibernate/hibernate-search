/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.document;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test the behavior of implementations of {@link IndexSchemaElement} when defining field types.
 * <p>
 * This does not check the effects of the definitions on the actual index schema,
 * since this would require backend-specific code to inspect that schema.
 * However, in search and projection tests, we check that defined fields behave correctly at runtime.
 */
class IndexSchemaElementTypeIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private StubMappedIndex index;

	@Test
	void analyzerOnSortableField() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.field(
							"myField",
							f -> f.asString()
									.sortable( Sortable.YES )
									.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					)
							.toReference();
				} ),
				"Setting an analyzer on sortable field"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.failure(
								"Invalid index field type",
								"both analyzer '" + DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name
										+ "' and sorts are enabled",
								"Sorts are not supported on analyzed fields",
								"If you need an analyzer simply to transform the text (lowercasing, ...)"
										+ " without splitting it into tokens, use a normalizer instead",
								"If you need an actual analyzer (with tokenization), define two separate fields:"
										+ " one with an analyzer that is not sortable,"
										+ " and one with a normalizer that is sortable"
						) );
	}

	@Test
	void analyzerOnAggregableField() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.field(
							"myField",
							f -> f.asString()
									.aggregable( Aggregable.YES )
									.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					)
							.toReference();
				} ),
				"Setting an analyzer on aggregable field"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.failure(
								"Invalid index field type",
								"both analyzer '"
										+ DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name
										+ "' and aggregations are enabled",
								"Aggregations are not supported on analyzed fields",
								"If you need an analyzer simply to transform the text (lowercasing, ...)"
										+ " without splitting it into tokens, use a normalizer instead.",
								"If you need an actual analyzer (with tokenization), define two separate fields:"
										+ " one with an analyzer that is not aggregable,"
										+ " and one with a normalizer that is aggregable."
						) );
	}

	@Test
	void analyzerAndNormalizer() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.field(
							"myField",
							f -> f.asString()
									.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
									.normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name )
					)
							.toReference();
				} ),
				"Setting an analyzer and a normalizer on the same field"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.failure(
								"Invalid index field type",
								"'" + DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name + "'",
								"'" + DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name + "'",
								"Either an analyzer or a normalizer can be assigned, but not both"
						) );
	}

	@Test
	void searchAnalyzerWithoutAnalyzer() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.schemaElement();
					root.field(
							"myField",
							f -> f.asString()
									.searchAnalyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					)
							.toReference();
				} ),
				"Setting a search analyzer, without setting an analyzer on the same field"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.failure(
								"Invalid index field type: search analyzer '"
										+ DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name + "'"
										+ " is assigned to this type, but the indexing analyzer is missing.",
								"Assign an indexing analyzer and a search analyzer, or remove the search analyzer"
						) );
	}

	private void setup(Consumer<IndexBindingContext> mappingContributor) {
		index = StubMappedIndex.ofAdvancedNonRetrievable( mappingContributor );
		setupHelper.start().withIndex( index ).setup();
	}

}
