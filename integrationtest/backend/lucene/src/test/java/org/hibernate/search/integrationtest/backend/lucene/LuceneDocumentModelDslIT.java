/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene;

import java.util.function.Consumer;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

public class LuceneDocumentModelDslIT {

	private static final String TYPE_NAME = "TypeName";
	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Test
	public void unknownAnalyzer() {
		SubTest.expectException(
				"Referencing an unknown analyzer",
				() -> setup( ctx -> {
					ctx.createTypeFactory().asString()
							.analyzer( "someNameThatIsClearlyNotAssignedToADefinition" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.failure( "Unknown analyzer" )
						.build() );
	}

	@Test
	public void unknownNormalizer() {
		SubTest.expectException(
				"Referencing an unknown analyzer",
				() -> setup( ctx -> {
					ctx.createTypeFactory().asString()
							.normalizer( "someNameThatIsClearlyNotAssignedToADefinition" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.failure( "Unknown normalizer" )
						.build() );
	}

	@Test
	public void unknownSearchAnalyzer() {
		SubTest.expectException(
				"Referencing an unknown search analyzer",
				() -> setup( ctx -> {
					ctx.createTypeFactory().asString()
							.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
							.searchAnalyzer( "someNameThatIsClearlyNotAssignedToADefinition" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.failure( "Unknown analyzer" )
						.build() );
	}

	private void setup(Consumer<IndexBindingContext> mappingContributor) {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						b -> b.mappedType( TYPE_NAME ),
						mappingContributor,
						ignored -> { }
				)
				.setup();
	}
}
