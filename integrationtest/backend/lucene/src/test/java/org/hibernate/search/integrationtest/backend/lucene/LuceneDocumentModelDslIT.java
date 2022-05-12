/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.Rule;
import org.junit.Test;

public class LuceneDocumentModelDslIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private StubMappedIndex index;

	@Test
	public void unknownAnalyzer() {
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
	public void unknownNormalizer() {
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
	public void unknownSearchAnalyzer() {
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
