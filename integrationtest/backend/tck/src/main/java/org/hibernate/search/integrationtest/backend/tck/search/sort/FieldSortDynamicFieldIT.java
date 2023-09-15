/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.junit.Assume.assumeTrue;

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
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class FieldSortDynamicFieldIT<F> {

	private static List<FieldTypeDescriptor<?>> supportedFieldTypes;

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		supportedFieldTypes = new ArrayList<>();
		List<Object[]> parameters = new ArrayList<>();
		for ( FieldTypeDescriptor<?> fieldType : FieldTypeDescriptor.getAll() ) {
			if ( fieldType.isFieldSortSupported() ) {
				supportedFieldTypes.add( fieldType );
				parameters.add( new Object[] { fieldType } );
			}
		}
		return parameters.toArray( new Object[0][] );
	}

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	private static final String EMPTY = "empty";

	private static final int DOCUMENT_1_ORDINAL = 1;
	private static final int DOCUMENT_2_ORDINAL = 3;
	private static final int DOCUMENT_3_ORDINAL = 5;

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndexes( mainIndex )
				.setup();

		initData();
	}

	private final FieldTypeDescriptor<F> fieldTypeDescriptor;

	public FieldSortDynamicFieldIT(FieldTypeDescriptor<F> fieldTypeDescriptor) {
		this.fieldTypeDescriptor = fieldTypeDescriptor;
	}

	@Test
	public void simple() {
		String fieldPath = mainFieldPath();

		assertThatQuery( matchNonEmptyQuery( f -> f.field( fieldPath ).asc() ) )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		assertThatQuery( matchNonEmptyQuery( f -> f.field( fieldPath ).desc() ) )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4531")
	public void neverPopulated() {
		assumeTrue(
				"This backend doesn't support sorts on a field of type '" + fieldTypeDescriptor
						+ "' that is missing from some of the target indexes.",
				TckConfiguration.get().getBackendFeatures()
						.supportsFieldSortWhenFieldMissingInSomeTargetIndexes( fieldTypeDescriptor.getJavaType() )
		);

		String neverPopulatedFieldPath = neverPopulatedFieldPath();
		String mainFieldPath = mainFieldPath();

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

	private String mainFieldPath() {
		return mainFieldPath( fieldTypeDescriptor );
	}

	private static String mainFieldPath(FieldTypeDescriptor<?> type) {
		return IndexBinding.fieldPath( type, "main" );
	}

	private String neverPopulatedFieldPath() {
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
		for ( FieldTypeDescriptor<?> type : supportedFieldTypes ) {
			addValue( type, document, ordinal );
		}
	}

	private static void addValue(FieldTypeDescriptor<?> type, DocumentElement documentElement, Integer ordinal) {
		if ( ordinal == null ) {
			return;
		}
		documentElement.addValue(
				mainFieldPath( type ),
				type.getAscendingUniqueTermValues().getSingle().get( ordinal )
		);
	}

	private static class IndexBinding {
		public static String fieldPath(FieldTypeDescriptor<?> type, String suffix) {
			return type.getUniqueName() + "_" + suffix;
		}

		IndexBinding(IndexSchemaElement root) {
			for ( FieldTypeDescriptor<?> type : supportedFieldTypes ) {
				root.fieldTemplate( "myTemplate" + type.getUniqueName(),
						f -> type.configure( f ).sortable( Sortable.YES ) )
						.matchingPathGlob( fieldPath( type, "*" ) );
			}
		}
	}

}
