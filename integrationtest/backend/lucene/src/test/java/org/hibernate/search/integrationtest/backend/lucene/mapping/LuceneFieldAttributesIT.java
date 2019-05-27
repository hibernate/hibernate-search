/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.mapping;

import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.document.Document;
import org.assertj.core.api.Assertions;

public class LuceneFieldAttributesIT {

	private static final String INDEX_NAME = "my-index";
	private static final String TEXT = "This is a text containing things. Red house with a blue carpet on the road...";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration( "myLuceneBackend" )
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void verifyIndexFieldTypes() {
		SearchQuery<Document> query = indexManager.createSearchScope().query()
				.asProjection(
						f -> f.extension( LuceneExtension.get() ).document()
				)
				.predicate( f -> f.matchAll() )
				.toQuery();

		List<Document> result = query.fetch().getHits();

		Assertions.assertThat( result ).hasSize( 1 );
		Document document = result.get( 0 );

		// norms false => omit-norms true
		Assertions.assertThat( document.getField( "keyword" ).fieldType().omitNorms() ).isTrue();

		// norms true => omit-norms false
		Assertions.assertThat( document.getField( "text" ).fieldType().omitNorms() ).isFalse();
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();

		workPlan.add( referenceProvider( "ID:1" ), document -> {
			document.addValue( indexMapping.string, "keyword" );
			document.addValue( indexMapping.text, TEXT );
		} );

		workPlan.execute().join();
	}

	private static class IndexMapping {

		final IndexFieldReference<String> string;
		final IndexFieldReference<String> text;

		IndexMapping(IndexSchemaElement root) {
			string = root.field( "keyword", f -> f.asString().projectable( Projectable.YES ) ).toReference();
			text = root.field( "text", f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ).projectable( Projectable.YES ) ).toReference();
		}
	}
}
