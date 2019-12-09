/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.mapping;


import static org.hibernate.search.integrationtest.backend.lucene.testsupport.util.DocumentAssert.containsDocument;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.document.Document;
import org.assertj.core.api.Assertions;

public class LuceneFieldTypesIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String TEXT_1 = "This is a text containing things. Red house with a blue carpet on the road...";
	private static final String TEXT_2 = "This is a text containing other things. Such as move the line on the right margin...";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1640")
	public void verifyProjectionsOnDifferentTypes() {
		SearchQuery<Document> query = indexManager.createScope().query()
				.asProjection(
						f -> f.extension( LuceneExtension.get() ).document()
				)
				.predicate( f -> f.matchAll() )
				.toQuery();

		List<Document> result = query.fetchAll().getHits();
		Assertions.assertThat( result )
				.hasSize( 2 )
				.satisfies( containsDocument(
						doc -> doc.hasField( "string", "keyword" )
								.hasField( "text", TEXT_1 )
								.hasField( "integer", 739 )
								.hasField( "longNumber", 739L )
								.hasField( "bool", 1 )
								.andOnlyInternalFields()
				) )
				.satisfies( containsDocument(
						doc -> doc.hasField( "string", "anotherKeyword" )
								.hasField( "text", TEXT_2 )
								.hasField( "integer", 123 )
								.hasField( "longNumber", 123L )
								.hasField( "bool", 0 )
								.andOnlyInternalFields()
				) );
	}

	private void initData() {
		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();

		plan.add( referenceProvider( "ID:1" ), document -> {
			document.addValue( indexMapping.string, "keyword" );
			document.addValue( indexMapping.text, TEXT_1 );
			document.addValue( indexMapping.integer, 739 );
			document.addValue( indexMapping.longNumber, 739L );
			document.addValue( indexMapping.bool, true );
		} );
		plan.add( referenceProvider( "ID:2" ), document -> {
			document.addValue( indexMapping.string, "anotherKeyword" );
			document.addValue( indexMapping.text, TEXT_2 );
			document.addValue( indexMapping.integer, 123 );
			document.addValue( indexMapping.longNumber, 123L );
			document.addValue( indexMapping.bool, false );
		} );

		plan.execute().join();
	}

	private static class IndexMapping {

		final IndexFieldReference<String> string;
		final IndexFieldReference<String> text;
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<Long> longNumber;
		final IndexFieldReference<Boolean> bool;

		IndexMapping(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) ).toReference();
			text = root.field( "text", f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ).projectable( Projectable.YES ) ).toReference();
			integer = root.field( "integer", f -> f.asInteger().projectable( Projectable.YES ) ).toReference();
			longNumber = root.field( "longNumber", f -> f.asLong().projectable( Projectable.YES ) ).toReference();

			// the external form is the Boolean,
			// BUT we treat it as **Integer** for comparison operation (range, sort)
			// and we store it as **Integer** as well.
			bool = root.field( "bool", f -> f.asBoolean().projectable( Projectable.YES ) ).toReference();
		}
	}
}
