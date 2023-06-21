/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.BooleanFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.data.RangeBoundInclusion;
import org.hibernate.search.util.impl.integrationtest.common.assertion.NormalizedDocRefHit;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class RangePredicateSpecificsIT<F> {
	private static final List<FieldTypeDescriptor<?>> supportedFieldTypes = new ArrayList<>();
	private static final List<DataSet<?>> dataSets = new ArrayList<>();
	private static final List<Object[]> parameters = new ArrayList<>();
	static {
		for ( FieldTypeDescriptor<?> fieldType : FieldTypeDescriptor.getAll() ) {
			if ( !GeoPointFieldTypeDescriptor.INSTANCE.equals( fieldType )
					// Booleans don't have enough values to run this test. See BooleanSortAndRangePredicateIT.
					&& !BooleanFieldTypeDescriptor.INSTANCE.equals( fieldType ) ) {
				supportedFieldTypes.add( fieldType );
			}
		}
		for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
			DataSet<?> dataSet = new DataSet<>( new RangePredicateTestValues<>( fieldType ) );
			dataSets.add( dataSet );
			parameters.add( new Object[] { dataSet } );
		}
	}

	@Parameterized.Parameters(name = "{0}")
	public static List<Object[]> parameters() {
		return parameters;
	}

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new ).name( "main" );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();

		BulkIndexer bulkIndexer = index.bulkIndexer();
		dataSets.forEach( dataSet -> dataSet.contribute( bulkIndexer ) );
		bulkIndexer.join();
	}

	private final DataSet<F> dataSet;

	public RangePredicateSpecificsIT(DataSet<F> dataSet) {
		this.dataSet = dataSet;
	}

	@Test
	public void atLeast() {
		int docOrdinal = 1;
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath() )
						.atLeast( value( docOrdinal ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( docOrdinal, null ) );
	}

	@Test
	public void atLeast_withDslConverter_valueConvertDefault() {
		int docOrdinal = 1;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath() )
						.atLeast( wrappedValue( docOrdinal ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( docOrdinal, null ) );
	}

	@Test
	public void atLeast_withDslConverter_valueConvertNo() {
		int docOrdinal = 1;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath() )
						.atLeast( value( docOrdinal ), ValueConvert.NO ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( docOrdinal, null ) );
	}

	@Test
	public void greaterThan() {
		int docOrdinal = 1;
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath() )
						.greaterThan( value( docOrdinal ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( docOrdinal + 1, null ) );
	}

	@Test
	public void greaterThan_withDslConverter_valueConvertDefault() {
		int docOrdinal = 1;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath() )
						.greaterThan( wrappedValue( docOrdinal ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( docOrdinal + 1, null ) );
	}

	@Test
	public void greaterThan_withDslConverter_valueConvertNo() {
		int docOrdinal = 1;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath() )
						.greaterThan( value( docOrdinal ), ValueConvert.NO ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( docOrdinal + 1, null ) );
	}

	@Test
	public void atMost() {
		int docOrdinal = docCount() - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath() )
						.atMost( value( docOrdinal ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( null, docOrdinal ) );
	}

	@Test
	public void atMost_withDslConverter_valueConvertDefault() {
		int docOrdinal = docCount() - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath() )
						.atMost( wrappedValue( docOrdinal ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( null, docOrdinal ) );
	}

	@Test
	public void atMost_withDslConverter_valueConvertNo() {
		int docOrdinal = docCount() - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath() )
						.atMost( value( docOrdinal ), ValueConvert.NO ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( null, docOrdinal ) );
	}

	@Test
	public void lessThan() {
		int docOrdinal = docCount() - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath() )
						.lessThan( value( docOrdinal ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( null, docOrdinal - 1 ) );
	}

	@Test
	public void lessThan_withDslConverter_valueConvertDefault() {
		int docOrdinal = docCount() - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath() )
						.lessThan( wrappedValue( docOrdinal ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( null, docOrdinal - 1 ) );
	}

	@Test
	public void lessThan_withDslConverter_valueConvertNo() {
		int docOrdinal = docCount() - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath() )
						.lessThan( value( docOrdinal ), ValueConvert.NO ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( null, docOrdinal - 1 ) );
	}

	@Test
	public void between() {
		int lowerValueNumber = 1;
		int upperValueNumber = docCount() - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath() )
						.between( value( lowerValueNumber ), value( upperValueNumber ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber, upperValueNumber ) );
	}

	@Test
	public void between_withDslConverter_valueConvertDefault() {
		int lowerValueNumber = 1;
		int upperValueNumber = docCount() - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath() )
						.between( wrappedValue( lowerValueNumber ), wrappedValue( upperValueNumber ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber, upperValueNumber ) );
	}

	@Test
	public void between_withDslConverter_valueConvertNo() {
		int lowerValueNumber = 1;
		int upperValueNumber = docCount() - 2;
		assertThatQuery( index.query()
				.where( f -> f.range().field( customDslConverterFieldPath() )
						.between( value( lowerValueNumber ), value( upperValueNumber ),
								ValueConvert.NO ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber, upperValueNumber ) );
	}

	@Test
	public void between_boundInclusion() {
		int lowerValueNumber = 1;
		int upperValueNumber = docCount() - 2;

		// Default is including both bounds
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath() )
						.between( value( lowerValueNumber ), value( upperValueNumber ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber, upperValueNumber ) );

		// explicit exclusion for the lower bound
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath() )
						.between( value( lowerValueNumber ), RangeBoundInclusion.EXCLUDED,
								value( upperValueNumber ), RangeBoundInclusion.INCLUDED ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber + 1, upperValueNumber ) );

		// explicit exclusion for the upper bound
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath() )
						.between( value( lowerValueNumber ), RangeBoundInclusion.INCLUDED,
								value( upperValueNumber ), RangeBoundInclusion.EXCLUDED ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber, upperValueNumber - 1 ) );

		// explicit inclusion for both bounds
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath() )
						.between( value( lowerValueNumber ), RangeBoundInclusion.INCLUDED,
								value( upperValueNumber ), RangeBoundInclusion.INCLUDED ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber, upperValueNumber ) );

		// explicit exclusion for both bounds
		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath() )
						.between( value( lowerValueNumber ), RangeBoundInclusion.EXCLUDED,
								value( upperValueNumber ), RangeBoundInclusion.EXCLUDED ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerValueNumber + 1, upperValueNumber - 1 ) );
	}

	@Test
	public void between_nullBounds() {
		int lowerDocOrdinal = 1;
		int upperDocOrdinal = docCount() - 2;

		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath() )
						.between( value( lowerDocOrdinal ), null ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( lowerDocOrdinal, null ) );

		assertThatQuery( index.query()
				.where( f -> f.range().field( defaultDslConverterFieldPath() )
						.between( null, value( upperDocOrdinal ) ) ) )
				.hasDocRefHitsAnyOrder( docIdRange( null, upperDocOrdinal ) );
	}

	private String defaultDslConverterFieldPath() {
		return index.binding().defaultDslConverterField.get( dataSet.fieldType ).relativeFieldName;
	}

	private String customDslConverterFieldPath() {
		return index.binding().customDslConverterField.get( dataSet.fieldType ).relativeFieldName;
	}

	private Consumer<NormalizedDocRefHit.Builder> docIdRange(Integer firstIncludedOrNull, Integer lastIncludedOrNull) {
		int firstIncluded = firstIncludedOrNull == null ? 0 : firstIncludedOrNull;
		int lastIncluded = lastIncludedOrNull == null ? docCount() - 1 : lastIncludedOrNull;
		return b -> {
			for ( int i = firstIncluded; i <= lastIncluded; i++ ) {
				b.doc( index.typeName(), dataSet.docId( i ) );
			}
		};
	}

	private F value(int docOrdinal) {
		return dataSet.values.matchingValue( docOrdinal );
	}

	private ValueWrapper<F> wrappedValue(int docOrdinal) {
		return new ValueWrapper<>( value( docOrdinal ) );
	}

	private int docCount() {
		return dataSet.values.size();
	}

	private static class IndexBinding {
		final SimpleFieldModelsByType defaultDslConverterField;
		final SimpleFieldModelsByType customDslConverterField;

		IndexBinding(IndexSchemaElement root) {
			defaultDslConverterField = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"defaultDslConverterField_" );
			customDslConverterField = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"customDslConverterField_",
					c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() ) );
		}
	}

	private static final class DataSet<F> extends AbstractPerFieldTypePredicateDataSet<F, RangePredicateTestValues<F>> {
		public DataSet(RangePredicateTestValues<F> values) {
			super( values );
		}

		public void contribute(BulkIndexer indexer) {
			for ( int i = 0; i < values.size(); i++ ) {
				F value = values.fieldValue( i );
				indexer.add( docId( i ), routingKey, document -> initDocument( document, value ) );
			}
		}

		private void initDocument(DocumentElement document, F fieldValue) {
			IndexBinding binding = index.binding();
			document.addValue( binding.defaultDslConverterField.get( fieldType ).reference, fieldValue );
			document.addValue( binding.customDslConverterField.get( fieldType ).reference, fieldValue );
		}
	}
}
