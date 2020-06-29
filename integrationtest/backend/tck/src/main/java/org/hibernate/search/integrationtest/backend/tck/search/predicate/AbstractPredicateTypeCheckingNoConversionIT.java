/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Test;

public abstract class AbstractPredicateTypeCheckingNoConversionIT<V extends AbstractPredicateTestValues<?>> {

	private final SimpleMappedIndex<IndexBinding> index;
	private final SimpleMappedIndex<IndexBinding> compatibleIndex;
	private final SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex;
	private final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex;
	protected final DataSet<?, V> dataSet;

	protected AbstractPredicateTypeCheckingNoConversionIT(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<IndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		this.index = index;
		this.compatibleIndex = compatibleIndex;
		this.rawFieldCompatibleIndex = rawFieldCompatibleIndex;
		this.incompatibleIndex = incompatibleIndex;
		this.dataSet = dataSet;
	}

	// DSL converters should be ignored
	@Test
	public void customDslConverter() {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path(),
						0 ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	// DSL converters, and their incompatibility, should be ignored
	@Test
	public void multiFields_customDslConverter() {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path(), customDslConverterField1Path(),
						0 ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path(), customDslConverterField1Path(),
						1 ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
	}

	@Test
	public void multiIndex_withCompatibleIndex() {
		StubMappingScope scope = index.createScope( compatibleIndex );

		assertThatQuery( scope.query()
				.where( f -> predicate( f, defaultDslConverterField0Path(), 0 ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
					b.doc( compatibleIndex.typeName(), dataSet.docId( 0 ) );
				} );
	}

	// DSL converters, and their incompatibility, should be ignored
	@Test
	public void multiIndex_withRawFieldCompatibleIndex() {
		StubMappingScope scope = index.createScope( rawFieldCompatibleIndex );

		assertThatQuery( scope.query()
				.where( f -> predicate( f, defaultDslConverterField0Path(), 0 ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
					b.doc( rawFieldCompatibleIndex.typeName(), dataSet.docId( 0 ) );
				} );
	}

	// Fields with a different type *are* a problem, though.
	@Test
	public void multiIndex_withIncompatibleIndex() {
		StubMappingScope scope = index.createScope( incompatibleIndex );

		assertThatThrownBy( () -> predicate( scope.predicate(), defaultDslConverterField0Path(),
				0 ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types" )
				.hasMessageContaining( "'" + defaultDslConverterField0Path() + "'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( index.name(), incompatibleIndex.name() )
				) );
	}

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, int matchingDocOrdinal);

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String field0Path, String field1Path,
			int matchingDocOrdinal);

	private String defaultDslConverterField0Path() {
		return index.binding().defaultDslConverterField0.get( dataSet.fieldType ).relativeFieldName;
	}

	private String customDslConverterField0Path() {
		return index.binding().customDslConverterField0.get( dataSet.fieldType ).relativeFieldName;
	}

	private String customDslConverterField1Path() {
		return index.binding().customDslConverterField1.get( dataSet.fieldType ).relativeFieldName;
	}

	// These DSL converters should not be used, since no conversion takes place.
	private static <T> ToDocumentFieldValueConverter<ValueWrapper, T> unusedDslConverter() {
		return new ToDocumentFieldValueConverter<ValueWrapper, T>() {
			@Override
			public T convert(ValueWrapper value, ToDocumentFieldValueConvertContext context) {
				throw new UnsupportedOperationException( "Should not be called" );
			}

			@Override
			public boolean isCompatibleWith(ToDocumentFieldValueConverter<?, ?> other) {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		};
	}

	public static final class IndexBinding {
		private final SimpleFieldModelsByType defaultDslConverterField0;
		private final SimpleFieldModelsByType customDslConverterField0;
		private final SimpleFieldModelsByType customDslConverterField1;

		public IndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			defaultDslConverterField0 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "defaultDslConverterField0_" );
			customDslConverterField0 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "customDslConverterField0_",
					c -> c.dslConverter( ValueWrapper.class, unusedDslConverter() ) );
			customDslConverterField1 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "customDslConverterField1_",
					c -> c.dslConverter( ValueWrapper.class, unusedDslConverter() ) );
		}
	}

	public static final class RawFieldCompatibleIndexBinding {
		private final SimpleFieldModelsByType defaultDslConverterField0;
		private final SimpleFieldModelsByType customDslConverterField0;
		private final SimpleFieldModelsByType customDslConverterField1;

		public RawFieldCompatibleIndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			defaultDslConverterField0 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "defaultDslConverterField0_",
					c -> c.dslConverter( ValueWrapper.class, unusedDslConverter() ) );
			customDslConverterField0 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "customDslConverterField0_" );
			customDslConverterField1 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "customDslConverterField1_" );
		}
	}

	public static final class IncompatibleIndexBinding {
		public IncompatibleIndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			fieldTypes.forEach( fieldType ->
					SimpleFieldModel.mapper( FieldTypeDescriptor.getIncompatible( fieldType ) )
							.map( root, "defaultDslConverterField0_" + fieldType.getUniqueName() )
			);
			fieldTypes.forEach( fieldType ->
					SimpleFieldModel.mapper( FieldTypeDescriptor.getIncompatible( fieldType ) )
							.map( root, "customDslConverterField0_" + fieldType.getUniqueName() )
			);
			fieldTypes.forEach( fieldType ->
					SimpleFieldModel.mapper( FieldTypeDescriptor.getIncompatible( fieldType ) )
							.map( root, "customDslConverterField1_" + fieldType.getUniqueName() )
			);
		}
	}

	public static final class DataSet<F, V extends AbstractPredicateTestValues<F>>
			extends AbstractPerFieldTypePredicateDataSet<F, V> {
		public DataSet(V values) {
			super( values );
		}

		public void contribute(SimpleMappedIndex<IndexBinding> mainIndex, BulkIndexer mainIndexer,
				SimpleMappedIndex<IndexBinding> compatibleIndex, BulkIndexer compatibleIndexer,
				SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex, BulkIndexer rawFieldCompatibleIndexer) {
			mainIndexer.add( docId( 0 ), routingKey,
					document -> initCompatibleDocument( mainIndex, document, values.fieldValue( 0 ) ) );
			compatibleIndexer.add( docId( 0 ), routingKey,
					document -> initCompatibleDocument( compatibleIndex, document, values.fieldValue( 0 ) ) );
			rawFieldCompatibleIndexer.add( docId( 0 ), routingKey,
					document -> initRawFieldCompatibleDocument( rawFieldCompatibleIndex, document, values.fieldValue( 0 ) ) );

			if ( values.size() > 1 ) {
				mainIndexer.add( docId( 1 ), routingKey,
						document -> initCompatibleDocument( mainIndex, document, values.fieldValue( 1 ) ) );
				compatibleIndexer.add( docId( 1 ), routingKey,
						document -> initCompatibleDocument( compatibleIndex, document, values.fieldValue( 1 ) ) );
				rawFieldCompatibleIndexer.add( docId( 1 ), routingKey,
						document -> initRawFieldCompatibleDocument( rawFieldCompatibleIndex, document, values.fieldValue( 1 ) ) );
			}
		}

		private void initCompatibleDocument(SimpleMappedIndex<IndexBinding> index, DocumentElement document,
				F fieldValue) {
			IndexBinding binding = index.binding();
			document.addValue( binding.defaultDslConverterField0.get( fieldType ).reference, fieldValue );
			document.addValue( binding.customDslConverterField0.get( fieldType ).reference, fieldValue );
			document.addValue( binding.customDslConverterField1.get( fieldType ).reference, fieldValue );
		}

		private void initRawFieldCompatibleDocument(SimpleMappedIndex<RawFieldCompatibleIndexBinding> index,
				DocumentElement document, F fieldValue) {
			RawFieldCompatibleIndexBinding binding = index.binding();
			document.addValue( binding.defaultDslConverterField0.get( fieldType ).reference, fieldValue );
			document.addValue( binding.customDslConverterField0.get( fieldType ).reference, fieldValue );
			document.addValue( binding.customDslConverterField1.get( fieldType ).reference, fieldValue );
		}
	}
}
