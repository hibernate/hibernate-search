/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.fieldtype;


import static org.hibernate.search.integrationtest.backend.lucene.testsupport.util.DocumentAssert.containsDocument;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.document.Document;
import org.assertj.core.api.Assertions;

public class LuceneFieldContentIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String TEXT_1 = "This is a text containing things. Red house with a blue carpet on the road...";
	private static final String TEXT_2 = "This is a text containing other things. Such as move the line on the right margin...";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexAccessors indexAccessors;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration( "myLuceneBackend" )
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1640")
	public void verifyProjectionsOnDifferentTypes() {
		IndexSearchQuery<Document> query = indexManager.createSearchScope().query()
				.asProjection(
						f -> f.extension( LuceneExtension.get() ).document()
				)
				.predicate( f -> f.matchAll() )
				.toQuery();

		List<Document> result = query.execute().getHits();
		Assertions.assertThat( result )
				.hasSize( 2 )
				.satisfies( containsDocument(
						"ID:1",
						doc -> doc.hasField( "string", "keyword" )
								.hasField( "text", TEXT_1 )
								.hasField( "integer", 739 )
								.hasField( "longNumber", 739L )
								.hasField( "bool", 1 )
								.andOnlyInternalFields()
				) )
				.satisfies( containsDocument(
						"ID:2",
						doc -> doc.hasField( "string", "anotherKeyword" )
								.hasField( "text", TEXT_2 )
								.hasField( "integer", 123 )
								.hasField( "longNumber", 123L )
								.hasField( "bool", 0 )
								.andOnlyInternalFields()
				) );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();

		workPlan.add( referenceProvider( "ID:1" ), document -> {
			indexAccessors.string.write( document, "keyword" );
			indexAccessors.text.write( document, TEXT_1 );
			indexAccessors.integer.write( document, 739 );
			indexAccessors.longNumber.write( document, 739L );
			indexAccessors.bool.write( document, true );
		} );
		workPlan.add( referenceProvider( "ID:2" ), document -> {
			indexAccessors.string.write( document, "anotherKeyword" );
			indexAccessors.text.write( document, TEXT_2 );
			indexAccessors.integer.write( document, 123 );
			indexAccessors.longNumber.write( document, 123L );
			indexAccessors.bool.write( document, false );
		} );

		workPlan.execute().join();
	}

	private static class IndexAccessors {

		final IndexFieldAccessor<String> string;
		final IndexFieldAccessor<String> text;
		final IndexFieldAccessor<Integer> integer;
		final IndexFieldAccessor<Long> longNumber;
		final IndexFieldAccessor<Boolean> bool;

		IndexAccessors(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) ).createAccessor();
			text = root.field( "text", f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name ).projectable( Projectable.YES ) ).createAccessor();
			integer = root.field( "integer", f -> f.asInteger().projectable( Projectable.YES ) ).createAccessor();
			longNumber = root.field( "longNumber", f -> f.asLong().projectable( Projectable.YES ) ).createAccessor();

			// the external form is the Boolean,
			// BUT we treat it as **Integer** for comparison operation (range, sort)
			// and we store it as **Integer** as well.
			bool = root.field( "bool", f -> f.asBoolean().projectable( Projectable.YES ) ).createAccessor();
		}
	}
}
