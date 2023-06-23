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
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.InvalidType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

public abstract class AbstractPredicateTypeCheckingAndConversionIT<V extends AbstractPredicateTestValues<?>, P> {

	private final SimpleMappedIndex<IndexBinding> index;
	private final SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex;
	private final SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex;
	private final SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex;
	private final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex;
	protected final DataSet<?, V> dataSet;

	protected AbstractPredicateTypeCheckingAndConversionIT(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		this.index = index;
		this.compatibleIndex = compatibleIndex;
		this.rawFieldCompatibleIndex = rawFieldCompatibleIndex;
		this.missingFieldIndex = missingFieldIndex;
		this.incompatibleIndex = incompatibleIndex;
		this.dataSet = dataSet;
	}

	@Test
	public void defaultDslConverter_valueConvertDefault_validType() {
		assertThatQuery( index.query()
				.where( f -> predicate( f, defaultDslConverterField0Path(),
						unwrappedMatchingParam( 0 ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@Test
	public void defaultDslConverter_valueConvertDefault_invalidType() {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, defaultDslConverterField0Path(), invalidTypeParam() ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( defaultDslConverterField0Path() )
				) );
	}

	@Test
	public void customDslConverter_valueConvertDefault_validType() {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path(), wrappedMatchingParam( 0 ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@Test
	public void customDslConverter_valueConvertDefault_invalidType() {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, customDslConverterField0Path(), invalidTypeParam() ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( customDslConverterField0Path() )
				) );
	}

	@Test
	public void defaultDslConverter_valueConvertYes_validType() {
		assertThatQuery( index.query()
				.where( f -> predicate( f, defaultDslConverterField0Path(),
						unwrappedMatchingParam( 0 ), ValueConvert.YES ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@Test
	public void defaultDslConverter_valueConvertYes_invalidType() {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, defaultDslConverterField0Path(), invalidTypeParam(),
				ValueConvert.YES ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( defaultDslConverterField0Path() )
				) );
	}

	@Test
	public void customDslConverter_valueConvertYes_validType() {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path(),
						wrappedMatchingParam( 0 ), ValueConvert.YES ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@Test
	public void customDslConverter_valueConvertYes_invalidType() {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, customDslConverterField0Path(), invalidTypeParam(),
				ValueConvert.YES ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( customDslConverterField0Path() )
				) );
	}

	@Test
	public void defaultDslConverter_valueConvertNo_validType() {
		assertThatQuery( index.query()
				.where( f -> predicate( f, defaultDslConverterField0Path(),
						unwrappedMatchingParam( 0 ), ValueConvert.NO ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@Test
	public void defaultDslConverter_valueConvertNo_invalidType() {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, defaultDslConverterField0Path(), invalidTypeParam(),
				ValueConvert.NO ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( defaultDslConverterField0Path() )
				) );
	}

	@Test
	public void customDslConverter_valueConvertNo_validType() {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path(),
						unwrappedMatchingParam( 0 ), ValueConvert.NO ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@Test
	public void customDslConverter_valueConvertNo_invalidType() {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, customDslConverterField0Path(), invalidTypeParam(),
				ValueConvert.NO ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( customDslConverterField0Path() )
				) );
	}

	@Test
	public void multiFields_customDslConverter_valueConvertYes() {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path(), customDslConverterField1Path(),
						wrappedMatchingParam( 0 ), ValueConvert.YES ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path(), customDslConverterField1Path(),
						wrappedMatchingParam( 1 ), ValueConvert.YES ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
	}

	@Test
	public void multiFields_customDslConverter_valueConvertNo() {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path(), customDslConverterField1Path(),
						unwrappedMatchingParam( 0 ), ValueConvert.NO ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path(), customDslConverterField1Path(),
						unwrappedMatchingParam( 1 ), ValueConvert.NO ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
	}

	@Test
	public void multiIndex_withCompatibleIndex_valueConvertYes() {
		StubMappingScope scope = index.createScope( compatibleIndex );

		assertThatQuery( scope.query()
				.where( f -> predicate( f, defaultDslConverterField0Path(), unwrappedMatchingParam( 0 ),
						ValueConvert.YES ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
					b.doc( compatibleIndex.typeName(), dataSet.docId( 0 ) );
				} );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex_valueConvertYes() {
		StubMappingScope scope = index.createScope( rawFieldCompatibleIndex );

		String fieldPath = defaultDslConverterField0Path();

		assertThatThrownBy( () -> predicate( scope.predicate(), fieldPath,
				unwrappedMatchingParam( 0 ), ValueConvert.YES ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Attribute 'dslConverter' differs:", " vs. "
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( index.name(), rawFieldCompatibleIndex.name() )
				) );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex_valueConvertNo() {
		StubMappingScope scope = index.createScope( rawFieldCompatibleIndex );

		assertThatQuery( scope.query()
				.where( f -> predicate( f, defaultDslConverterField0Path(),
						unwrappedMatchingParam( 0 ), ValueConvert.NO ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
					b.doc( rawFieldCompatibleIndex.typeName(), dataSet.docId( 0 ) );
				} );
	}

	/**
	 * Test that no failure occurs when a predicate targets a field
	 * that only exists in one of the targeted indexes.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4173")
	public void multiIndex_withMissingFieldIndex_valueConvertYes() {
		StubMappingScope scope = index.createScope( missingFieldIndex );

		// The predicate should not match anything in missingFieldIndex
		assertThatQuery( scope.query()
				.where( f -> predicate( f, defaultDslConverterField0Path(), unwrappedMatchingParam( 0 ),
						ValueConvert.YES ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
				} );

		// ... but it should not prevent the query from executing either:
		// if the predicate is optional, it should be ignored for missingFieldIndex.
		assertThatQuery( scope.query()
				.where( f -> f.or(
						predicate( f, defaultDslConverterField0Path(), unwrappedMatchingParam( 0 ), ValueConvert.YES ),
						f.id().matching( dataSet.docId( DataSet.MISSING_FIELD_INDEX_DOC_ORDINAL ) ) ) ) )
				.hasDocRefHitsAnyOrder( c -> c
						.doc( index.typeName(), dataSet.docId( 0 ) )
						.doc( missingFieldIndex.typeName(), dataSet.docId( DataSet.MISSING_FIELD_INDEX_DOC_ORDINAL ) ) )
				.hasTotalHitCount( 2 );
	}

	/**
	 * Test that no failure occurs when a predicate targets a field
	 * that only exists in one of the targeted indexes.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4173")
	public void multiIndex_withMissingFieldIndex_valueConvertNo() {
		StubMappingScope scope = index.createScope( missingFieldIndex );

		// The predicate should not match anything in missingFieldIndex
		assertThatQuery( scope.query()
				.where( f -> predicate( f, defaultDslConverterField0Path(), unwrappedMatchingParam( 0 ),
						ValueConvert.NO ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
				} );

		// ... but it should not prevent the query from executing either:
		// if the predicate is optional, it should be ignored for missingFieldIndex.
		assertThatQuery( scope.query()
				.where( f -> f.or(
						predicate( f, defaultDslConverterField0Path(), unwrappedMatchingParam( 0 ), ValueConvert.NO ),
						f.id().matching( dataSet.docId( DataSet.MISSING_FIELD_INDEX_DOC_ORDINAL ) ) ) ) )
				.hasDocRefHitsAnyOrder( c -> c
						.doc( index.typeName(), dataSet.docId( 0 ) )
						.doc( missingFieldIndex.typeName(), dataSet.docId( DataSet.MISSING_FIELD_INDEX_DOC_ORDINAL ) ) )
				.hasTotalHitCount( 2 );
	}

	@Test
	public void multiIndex_withIncompatibleIndex_valueConvertYes() {
		StubMappingScope scope = index.createScope( incompatibleIndex );

		String fieldPath = defaultDslConverterField0Path();

		assertThatThrownBy( () -> predicate( scope.predicate(), fieldPath,
				unwrappedMatchingParam( 0 ), ValueConvert.YES ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for '" + predicateNameInErrorMessage() + "'", " vs. "
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( index.name(), incompatibleIndex.name() )
				) );
	}

	@Test
	public void multiIndex_withIncompatibleIndex_valueConvertNo() {
		StubMappingScope scope = index.createScope( incompatibleIndex );

		String fieldPath = defaultDslConverterField0Path();

		assertThatThrownBy( () -> predicate( scope.predicate(), fieldPath,
				unwrappedMatchingParam( 0 ), ValueConvert.NO ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for '" + predicateNameInErrorMessage() + "'"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( index.name(), incompatibleIndex.name() )
				) );
	}

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, P matchingParam);

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, P matchingParam,
			ValueConvert valueConvert);

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String field0Path, String field1Path,
			P matchingParam, ValueConvert valueConvert);

	protected abstract P invalidTypeParam();

	protected abstract P unwrappedMatchingParam(int matchingDocOrdinal);

	protected abstract P wrappedMatchingParam(int matchingDocOrdinal);

	protected abstract String predicateNameInErrorMessage();

	private String defaultDslConverterField0Path() {
		return index.binding().defaultDslConverterField0.get( dataSet.fieldType ).relativeFieldName;
	}

	private String customDslConverterField0Path() {
		return index.binding().customDslConverterField0.get( dataSet.fieldType ).relativeFieldName;
	}

	private String customDslConverterField1Path() {
		return index.binding().customDslConverterField1.get( dataSet.fieldType ).relativeFieldName;
	}

	public static final class IndexBinding {
		private final SimpleFieldModelsByType defaultDslConverterField0;
		private final SimpleFieldModelsByType customDslConverterField0;
		private final SimpleFieldModelsByType customDslConverterField1;

		public IndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			defaultDslConverterField0 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "defaultDslConverterField0_" );
			customDslConverterField0 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "customDslConverterField0_",
					c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() ) );
			customDslConverterField1 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "customDslConverterField1_",
					c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() ) );
		}
	}

	public static final class CompatibleIndexBinding {
		private final SimpleFieldModelsByType defaultDslConverterField0;
		private final SimpleFieldModelsByType customDslConverterField0;
		private final SimpleFieldModelsByType customDslConverterField1;

		public CompatibleIndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			defaultDslConverterField0 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "defaultDslConverterField0_",
					this::addIrrelevantOptions );
			customDslConverterField0 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "customDslConverterField0_",
					(fieldType, c) -> {
						c.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() );
						addIrrelevantOptions( fieldType, c );
					} );
			customDslConverterField1 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "customDslConverterField1_",
					(fieldType, c) -> {
						c.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() );
						addIrrelevantOptions( fieldType, c );
					} );
		}

		// See HSEARCH-3307: this checks that irrelevant options are ignored when checking cross-index field compatibility
		protected void addIrrelevantOptions(FieldTypeDescriptor<?> fieldType, StandardIndexFieldTypeOptionsStep<?, ?> c) {
			c.projectable( Projectable.YES );
			if ( fieldType.isFieldSortSupported() ) {
				c.sortable( Sortable.YES );
				c.aggregable( Aggregable.YES );
			}
		}
	}

	public static final class RawFieldCompatibleIndexBinding {
		private final SimpleFieldModelsByType defaultDslConverterField0;
		private final SimpleFieldModelsByType customDslConverterField0;
		private final SimpleFieldModelsByType customDslConverterField1;

		public RawFieldCompatibleIndexBinding(IndexSchemaElement root,
				Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			defaultDslConverterField0 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "defaultDslConverterField0_",
					c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() ) );
			customDslConverterField0 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "customDslConverterField0_" );
			customDslConverterField1 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "customDslConverterField1_" );
		}
	}

	public static final class IncompatibleIndexBinding {
		public IncompatibleIndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			fieldTypes.forEach( fieldType -> SimpleFieldModel.mapper( FieldTypeDescriptor.getIncompatible( fieldType ) )
					.map( root, "defaultDslConverterField0_" + fieldType.getUniqueName() )
			);
			fieldTypes.forEach( fieldType -> SimpleFieldModel.mapper( FieldTypeDescriptor.getIncompatible( fieldType ) )
					.map( root, "customDslConverterField0_" + fieldType.getUniqueName() )
			);
			fieldTypes.forEach( fieldType -> SimpleFieldModel.mapper( FieldTypeDescriptor.getIncompatible( fieldType ) )
					.map( root, "customDslConverterField1_" + fieldType.getUniqueName() )
			);
		}
	}

	public static class MissingFieldIndexBinding {
		public MissingFieldIndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
		}
	}

	public static final class DataSet<F, V extends AbstractPredicateTestValues<F>>
			extends AbstractPerFieldTypePredicateDataSet<F, V> {
		public static final int MISSING_FIELD_INDEX_DOC_ORDINAL = 100;

		public DataSet(V values) {
			super( values );
		}

		public void contribute(SimpleMappedIndex<IndexBinding> mainIndex, BulkIndexer mainIndexer,
				SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex, BulkIndexer compatibleIndexer,
				SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
				BulkIndexer rawFieldCompatibleIndexer,
				SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex, BulkIndexer missingFieldIndexer) {
			mainIndexer.add( docId( 0 ), routingKey,
					document -> initDocument( mainIndex, document, values.fieldValue( 0 ) ) );
			mainIndexer.add( docId( 1 ), routingKey,
					document -> initDocument( mainIndex, document, values.fieldValue( 1 ) ) );
			compatibleIndexer.add( docId( 0 ), routingKey,
					document -> initCompatibleDocument( compatibleIndex, document, values.fieldValue( 0 ) ) );
			compatibleIndexer.add( docId( 1 ), routingKey,
					document -> initCompatibleDocument( compatibleIndex, document, values.fieldValue( 1 ) ) );
			rawFieldCompatibleIndexer.add( docId( 0 ), routingKey,
					document -> initRawFieldCompatibleDocument( rawFieldCompatibleIndex, document, values.fieldValue( 0 ) ) );
			rawFieldCompatibleIndexer.add( docId( 1 ), routingKey,
					document -> initRawFieldCompatibleDocument( rawFieldCompatibleIndex, document, values.fieldValue( 1 ) ) );
			missingFieldIndexer.add( docId( MISSING_FIELD_INDEX_DOC_ORDINAL ), routingKey, document -> {} );
		}

		private void initDocument(SimpleMappedIndex<IndexBinding> index, DocumentElement document,
				F fieldValue) {
			IndexBinding binding = index.binding();
			document.addValue( binding.defaultDslConverterField0.get( fieldType ).reference, fieldValue );
			document.addValue( binding.customDslConverterField0.get( fieldType ).reference, fieldValue );
			document.addValue( binding.customDslConverterField1.get( fieldType ).reference, fieldValue );
		}

		private void initCompatibleDocument(SimpleMappedIndex<CompatibleIndexBinding> index, DocumentElement document,
				F fieldValue) {
			CompatibleIndexBinding binding = index.binding();
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
