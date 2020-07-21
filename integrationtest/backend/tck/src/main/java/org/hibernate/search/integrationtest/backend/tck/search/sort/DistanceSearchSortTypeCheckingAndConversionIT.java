/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueDistanceFromCenterValues.CENTER_POINT;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueDistanceFromCenterValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Tests behavior related to type checking and type conversion of DSL arguments
 * for sorts by field value.
 */
public class DistanceSearchSortTypeCheckingAndConversionIT {

	private static final GeoPointFieldTypeDescriptor fieldType = GeoPointFieldTypeDescriptor.INSTANCE;

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	private static final String EMPTY = "empty";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 = "raw_field_compatible_1";

	// TODO HSEARCH-3863 use the other ordinals when we implement.missing().use/last/first for distance sorts
	private static final int BEFORE_DOCUMENT_1_ORDINAL = 0;
	private static final int DOCUMENT_1_ORDINAL = 1;
	private static final int BETWEEN_DOCUMENT_1_AND_2_ORDINAL = 2;
	private static final int DOCUMENT_2_ORDINAL = 3;
	private static final int BETWEEN_DOCUMENT_2_AND_3_ORDINAL = 4;
	private static final int DOCUMENT_3_ORDINAL = 5;
	private static final int AFTER_DOCUMENT_3_ORDINAL = 6;

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private static final SimpleMappedIndex<IndexBinding> compatibleIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "compatible" );
	private static final SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex =
			SimpleMappedIndex.of( RawFieldCompatibleIndexBinding::new ).name( "rawFieldCompatible" );
	private static final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
			SimpleMappedIndex.of( IncompatibleIndexBinding::new ).name( "incompatible" );

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndexes( mainIndex, compatibleIndex, rawFieldCompatibleIndex, incompatibleIndex )
				.setup();

		initData();
	}

	// TODO HSEARCH-3863 implement tests related to DSL converters when we implement.missing().use/last/first for distance sorts
	//   See test methods in FieldSearchSortTypeCheckingAndConversionIT

	@Test
	public void unsortable() {
		StubMappingScope scope = mainIndex.createScope();
		String fieldPath = getNonSortableFieldPath();

		assertThatThrownBy( () -> {
				scope.sort().distance( fieldPath, CENTER_POINT );
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'sort:distance' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable (whichever is relevant)"
				);
	}

	@Test
	public void multiIndex_withCompatibleIndex() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldPath();

		query = matchAllQuery( f -> f.distance( fieldPath, CENTER_POINT ), scope );

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), EMPTY );
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( compatibleIndex.typeName(), COMPATIBLE_INDEX_DOCUMENT_1 );
			b.doc( mainIndex.typeName(), DOCUMENT_2 );
			b.doc( mainIndex.typeName(), DOCUMENT_3 );
		} );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		SearchQuery<DocumentReference> query;
		String fieldPath = getFieldWithDslConverterPath();

		query = matchAllQuery( f -> f.distance( fieldPath, CENTER_POINT ), scope );

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), EMPTY );
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( rawFieldCompatibleIndex.typeName(), RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
			b.doc( mainIndex.typeName(), DOCUMENT_2 );
			b.doc( mainIndex.typeName(), DOCUMENT_3 );
		} );
	}

	@Test
	public void multiIndex_withIncompatibleIndex() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = getFieldPath();

		assertThatThrownBy(
				() -> {
					matchAllQuery( f -> f.distance( fieldPath, CENTER_POINT ), scope );
				}
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for 'sort:distance'"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), incompatibleIndex.name() )
				) );
	}

	private SearchQuery<DocumentReference> matchAllQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor, StubMappingScope scope) {
		return scope.query()
				.where( f -> f.matchAll() )
				.sort( sortContributor )
				.toQuery();
	}

	private String getFieldPath() {
		return mainIndex.binding().fieldModel.relativeFieldName;
	}

	private String getFieldWithDslConverterPath() {
		return mainIndex.binding().fieldWithDslConverterModel.relativeFieldName;
	}

	private String getNonSortableFieldPath() {
		return mainIndex.binding().nonSortableFieldModel.relativeFieldName;
	}

	private static void initDocument(IndexBinding indexBinding, DocumentElement document, Integer ordinal) {
		addValue( indexBinding.fieldModel, document, ordinal );
		addValue( indexBinding.fieldWithDslConverterModel, document, ordinal );
	}

	private static void addValue(SimpleFieldModel<GeoPoint> fieldModel, DocumentElement documentElement, Integer ordinal) {
		if ( ordinal == null ) {
			return;
		}
		documentElement.addValue(
				fieldModel.reference,
				AscendingUniqueDistanceFromCenterValues.INSTANCE.getSingle().get( ordinal )
		);
	}

	private static void initData() {
		BulkIndexer mainIndexer = mainIndex.bulkIndexer()
				// Important: do not index the documents in the expected order after sorts (1, 2, 3)
				.add( DOCUMENT_2, document -> initDocument( mainIndex.binding(), document, DOCUMENT_2_ORDINAL ) )
				.add( EMPTY, document -> initDocument( mainIndex.binding(), document, null ) )
				.add( DOCUMENT_1, document -> initDocument( mainIndex.binding(), document, DOCUMENT_1_ORDINAL ) )
				.add( DOCUMENT_3, document -> initDocument( mainIndex.binding(), document, DOCUMENT_3_ORDINAL ) );
		BulkIndexer compatibleIndexer = compatibleIndex.bulkIndexer()
				.add( COMPATIBLE_INDEX_DOCUMENT_1,
						document -> initDocument( compatibleIndex.binding(), document, BETWEEN_DOCUMENT_1_AND_2_ORDINAL ) );
		BulkIndexer rawFieldCompatibleIndexer = rawFieldCompatibleIndex.bulkIndexer()
				.add( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1,
						document -> initDocument( rawFieldCompatibleIndex.binding(), document, BETWEEN_DOCUMENT_1_AND_2_ORDINAL ) );
		mainIndexer.join( compatibleIndexer, rawFieldCompatibleIndexer );
	}

	private static class IndexBinding {
		final SimpleFieldModel<GeoPoint> fieldModel;
		final SimpleFieldModel<GeoPoint> fieldWithDslConverterModel;
		final SimpleFieldModel<GeoPoint> nonSortableFieldModel;

		IndexBinding(IndexSchemaElement root) {
			this( root, ignored -> { } );
		}

		IndexBinding(IndexSchemaElement root,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			fieldModel = SimpleFieldModel.mapper( fieldType )
					.map( root, "unconverted", c -> c.sortable( Sortable.YES ), additionalConfiguration );
			fieldWithDslConverterModel = SimpleFieldModel.mapper( fieldType )
					.map(
							root, "converted", c -> c.sortable( Sortable.YES ),
							additionalConfiguration.andThen(
									c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() ) )
					);
			nonSortableFieldModel = SimpleFieldModel.mapper( fieldType )
					.map(
							root, "nonSortable", c -> c.sortable( Sortable.YES ),
							additionalConfiguration.andThen( c -> c.sortable( Sortable.NO ) )
					);
		}
	}

	private static class RawFieldCompatibleIndexBinding extends IndexBinding {
		RawFieldCompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the fieldModel from IndexBinding,
			 * but with an incompatible DSL converter.
			 */
			super( root, c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() ) );
		}
	}

	private static class IncompatibleIndexBinding {
		IncompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the fieldModel from IndexBinding,
			 * but with an incompatible type.
			 */
			mapFieldsWithIncompatibleType( root );
		}

		private static void mapFieldsWithIncompatibleType(IndexSchemaElement parent) {
			SimpleFieldModel.mapper( FieldTypeDescriptor.getIncompatible( fieldType ) )
					.map( parent, "unconverted", c -> c.sortable( Sortable.YES ) );
		}
	}

}
