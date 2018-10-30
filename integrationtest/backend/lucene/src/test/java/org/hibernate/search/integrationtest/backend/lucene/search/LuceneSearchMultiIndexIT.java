/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.Projectable;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBuilder;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.integrationtest.backend.tck.search.SearchMultiIndexIT;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * This is an extension of the backend TCK test {@link SearchMultiIndexIT}.
 */
public class LuceneSearchMultiIndexIT {

	private static final String BACKEND_1 = "backend_1";

	private static final String STRING_1 = "string_1";
	private static final String STRING_2 = "string_2";

	// Backend 1 / Index 1

	private static final String INDEX_NAME_1_1 = "IndexName_1_1";

	private static final String DOCUMENT_1_1_1 = "1_1_1";
	private static final String ADDITIONAL_FIELD_1_1_1 = "additional_field_1_1_1";

	private static final String DOCUMENT_1_1_2 = "1_1_2";
	private static final String ADDITIONAL_FIELD_1_1_2 = "additional_field_1_1_2";

	// Backend 1 / Index 2

	private static final String INDEX_NAME_1_2 = "IndexName_1_2";

	private static final String DOCUMENT_1_2_1 = "1_2_1";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	// Backend 1 / Index 1

	private IndexAccessors_1_1 indexAccessors_1_1;
	private MappedIndexManager<?> indexManager_1_1;

	// Backend 1 / Index 2

	private IndexAccessors_1_2 indexAccessors_1_2;
	private MappedIndexManager<?> indexManager_1_2;

	private SessionContext sessionContext = new StubSessionContext();

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration( BACKEND_1 )
				.withIndex(
						"MappedType_1_1", INDEX_NAME_1_1,
						ctx -> this.indexAccessors_1_1 = new IndexAccessors_1_1( ctx.getSchemaElement() ),
						indexManager -> this.indexManager_1_1 = indexManager
				)
				.withIndex(
						"MappedType_1_2", INDEX_NAME_1_2,
						ctx -> this.indexAccessors_1_2 = new IndexAccessors_1_2( ctx.getSchemaElement() ),
						indexManager -> this.indexManager_1_2 = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void field_in_one_index_only_is_supported_for_sorting() {
		IndexSearchTargetBuilder searchTargetBuilder = indexManager_1_1.createSearchTarget();
		indexManager_1_2.addToSearchTarget( searchTargetBuilder );
		IndexSearchTarget searchTarget = searchTargetBuilder.build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.sort( c -> c.byField( "additionalField" ).asc().onMissingValue().sortLast() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query ).hasReferencesHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_1, DOCUMENT_1_1_2 );
			c.doc( INDEX_NAME_1_2, DOCUMENT_1_2_1 );
		} );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.sort( c -> c.byField( "additionalField" ).desc().onMissingValue().sortLast() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query ).hasReferencesHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2, DOCUMENT_1_1_1 );
			c.doc( INDEX_NAME_1_2, DOCUMENT_1_2_1 );
		} );
	}

	private void initData() {
		// Backend 1 / Index 1

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager_1_1.createWorkPlan( sessionContext );

		workPlan.add( referenceProvider( DOCUMENT_1_1_1 ), document -> {
			indexAccessors_1_1.string.write( document, STRING_1 );
			indexAccessors_1_1.additionalField.write( document, ADDITIONAL_FIELD_1_1_1 );
		} );
		workPlan.add( referenceProvider( DOCUMENT_1_1_2 ), document -> {
			indexAccessors_1_1.string.write( document, STRING_2 );
			indexAccessors_1_1.additionalField.write( document, ADDITIONAL_FIELD_1_1_2 );
		} );

		workPlan.execute().join();

		IndexSearchTarget searchTarget = indexManager_1_1.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasReferencesHitsAnyOrder( INDEX_NAME_1_1, DOCUMENT_1_1_1, DOCUMENT_1_1_2 );

		// Backend 1 / Index 2

		workPlan = indexManager_1_2.createWorkPlan( sessionContext );

		workPlan.add( referenceProvider( DOCUMENT_1_2_1 ), document -> {
			indexAccessors_1_2.string.write( document, STRING_1 );
		} );

		workPlan.execute().join();

		searchTarget = indexManager_1_2.createSearchTarget().build();
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasReferencesHitsAnyOrder( INDEX_NAME_1_2, DOCUMENT_1_2_1 );
	}

	private static class IndexAccessors_1_1 {
		final IndexFieldAccessor<String> string;
		final IndexFieldAccessor<String> additionalField;

		IndexAccessors_1_1(IndexSchemaElement root) {
			string = root.field( "string" ).asString().createAccessor();
			additionalField = root.field( "additionalField" ).asString().sortable( Sortable.YES ).projectable( Projectable.YES ).createAccessor();
		}
	}

	private static class IndexAccessors_1_2 {
		final IndexFieldAccessor<String> string;

		IndexAccessors_1_2(IndexSchemaElement root) {
			string = root.field( "string" ).asString().createAccessor();
		}
	}
}
