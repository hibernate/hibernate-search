/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.document;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test the behavior of implementations of {@link IndexSchemaElement} when defining field types.
 * <p>
 * This does not check the effects of the definitions on the actual index schema,
 * since this would require backend-specific code to inspect that schema.
 * However, in search and projection tests, we check that defined fields behave correctly at runtime.
 */
public class IndexSchemaElementTypeIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private StubMappedIndex index;

	@Test
	public void analyzerOnSortableField() {
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
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.failure(
								"Cannot apply an analyzer on a sortable field",
								"Use a normalizer instead",
								"'" + DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name + "'"
						)
						.build() );
	}

	@Test
	public void analyzerOnAggregableField() {
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
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.failure(
								"Cannot apply an analyzer on an aggregable field",
								"Use a normalizer instead",
								"'" + DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name + "'"
						)
						.build() );
	}

	@Test
	public void analyzerAndNormalizer() {
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
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.failure(
								"Cannot apply both an analyzer and a normalizer",
								"'" + DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name + "'",
								"'" + DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name + "'"
						)
						.build() );
	}

	@Test
	public void searchAnalyzerWithoutAnalyzer() {
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
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( index.typeName() )
						.indexContext( index.name() )
						.failure(
								"Cannot apply a search analyzer if an analyzer has not been defined on the same field",
								"'" + DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name + "'"
						)
						.build() );
	}

	private void setup(Consumer<IndexBindingContext> mappingContributor) {
		index = StubMappedIndex.ofAdvancedNonRetrievable( mappingContributor );
		setupHelper.start().withIndex( index ).setup();
	}

}
