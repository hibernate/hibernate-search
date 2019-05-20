/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.search;

import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ElasticsearchMatchSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String TEST_TERM = "ThisWillBeLowercasedByTheNormalizer";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void match_skipAnalysis_normalizedStringField() {
		SubTest.expectException( () -> indexManager.createSearchScope().query()
				.predicate( f -> f.match().onField( "normalizedStringField" ).matching( TEST_TERM ).skipAnalysis() )
				.toQuery()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "HSEARCH400560" )
				.hasMessageContaining( "Elasticsearch backend does not support skip analysis on not analyzed field" )
				.hasMessageContaining( "normalizedStringField" );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), document -> document.addValue( indexMapping.normalizedStringField, TEST_TERM ) );
		workPlan.execute().join();
	}

	private static class IndexMapping {
		final IndexFieldReference<String> normalizedStringField;

		IndexMapping(IndexSchemaElement root) {
			normalizedStringField = root.field(
					"normalizedStringField",
					c -> c.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name )
			)
					.toReference();
		}
	}
}
