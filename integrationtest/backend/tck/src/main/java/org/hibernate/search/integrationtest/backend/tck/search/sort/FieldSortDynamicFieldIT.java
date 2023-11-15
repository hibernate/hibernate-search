/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FieldSortDynamicFieldIT<F> {

	private static final List<StandardFieldTypeDescriptor<?>> supportedFieldTypes = new ArrayList<>();
	private static final List<Arguments> parameters = new ArrayList<>();

	static {
		for ( StandardFieldTypeDescriptor<?> fieldType : FieldTypeDescriptor.getAllStandard() ) {
			if ( fieldType.isFieldSortSupported() ) {
				supportedFieldTypes.add( fieldType );
				parameters.add( Arguments.of( fieldType ) );
			}
		}
	}

	public static List<? extends Arguments> params() {
		return parameters;
	}

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	private static final String EMPTY = "empty";

	private static final int DOCUMENT_1_ORDINAL = 1;
	private static final int DOCUMENT_2_ORDINAL = 3;
	private static final int DOCUMENT_3_ORDINAL = 5;

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes( mainIndex )
				.setup();

		initData();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void simple(FieldTypeDescriptor<F, ?> fieldTypeDescriptor) {
		String fieldPath = mainFieldPath( fieldTypeDescriptor );

		assertThatQuery( matchNonEmptyQuery( f -> f.field( fieldPath ).asc() ) )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		assertThatQuery( matchNonEmptyQuery( f -> f.field( fieldPath ).desc() ) )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4531")
	void neverPopulated(FieldTypeDescriptor<F, ?> fieldTypeDescriptor) {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures()
						.supportsFieldSortWhenFieldMissingInSomeTargetIndexes( fieldTypeDescriptor.getJavaType() ),
				"This backend doesn't support sorts on a field of type '" + fieldTypeDescriptor
						+ "' that is missing from some of the target indexes."
		);

		String neverPopulatedFieldPath = neverPopulatedFieldPath( fieldTypeDescriptor );
		String mainFieldPath = mainFieldPath( fieldTypeDescriptor );

		// The field that wasn't populated shouldn't have any effect on the sort,
		// but it shouldn't trigger an exception, either (see HSEARCH-4531).
		assertThatQuery( matchNonEmptyQuery( f -> f.composite()
				.add( f.field( neverPopulatedFieldPath ).asc() )
				.add( f.field( mainFieldPath ).asc() ) ) )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		assertThatQuery( matchNonEmptyQuery( f -> f.composite()
				.add( f.field( neverPopulatedFieldPath ).desc() )
				.add( f.field( mainFieldPath ).desc() ) ) )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
	}

	private SearchQuery<DocumentReference> matchNonEmptyQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		return matchNonEmptyQuery( sortContributor, mainIndex.createScope() );
	}

	private SearchQuery<DocumentReference> matchNonEmptyQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor, StubMappingScope scope) {
		return scope.query()
				.where( f -> f.matchAll().except( f.id().matching( EMPTY ) ) )
				.sort( sortContributor )
				.toQuery();
	}

	private static String mainFieldPath(FieldTypeDescriptor<?, ?> type) {
		return IndexBinding.fieldPath( type, "main" );
	}

	private String neverPopulatedFieldPath(FieldTypeDescriptor<?, ?> fieldTypeDescriptor) {
		return IndexBinding.fieldPath( fieldTypeDescriptor, "neverPopulated" );
	}

	private static void initData() {
		BulkIndexer mainIndexer = mainIndex.bulkIndexer()
				// Important: do not index the documents in the expected order after sorts (1, 2, 3)
				.add( DOCUMENT_2, document -> initDocument( document, DOCUMENT_2_ORDINAL ) )
				.add( EMPTY, document -> initDocument( document, null ) )
				.add( DOCUMENT_1, document -> initDocument( document, DOCUMENT_1_ORDINAL ) )
				.add( DOCUMENT_3, document -> initDocument( document, DOCUMENT_3_ORDINAL ) );
		mainIndexer.join();
	}

	private static void initDocument(DocumentElement document, Integer ordinal) {
		for ( StandardFieldTypeDescriptor<?> type : supportedFieldTypes ) {
			addValue( type, document, ordinal );
		}
	}

	private static void addValue(StandardFieldTypeDescriptor<?> type, DocumentElement documentElement, Integer ordinal) {
		if ( ordinal == null ) {
			return;
		}
		documentElement.addValue(
				mainFieldPath( type ),
				type.getAscendingUniqueTermValues().getSingle().get( ordinal )
		);
	}

	private static class IndexBinding {
		public static String fieldPath(FieldTypeDescriptor<?, ?> type, String suffix) {
			return type.getUniqueName() + "_" + suffix;
		}

		IndexBinding(IndexSchemaElement root) {
			for ( StandardFieldTypeDescriptor<?> type : supportedFieldTypes ) {
				root.fieldTemplate( "myTemplate" + type.getUniqueName(),
						f -> type.configure( f ).sortable( Sortable.YES ) )
						.matchingPathGlob( fieldPath( type, "*" ) );
			}
		}
	}

}
