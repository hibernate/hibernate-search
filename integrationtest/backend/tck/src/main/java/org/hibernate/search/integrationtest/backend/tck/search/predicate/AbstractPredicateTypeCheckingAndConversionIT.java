/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.IntegerFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.InvalidType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class AbstractPredicateTypeCheckingAndConversionIT<V extends AbstractPredicateTestValues<?>, P> {

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void defaultDslConverter_valueModelDefault_validType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, defaultDslConverterField0Path( index, dataSet ),
						unwrappedMatchingParam( 0, dataSet ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void defaultDslConverter_valueModelDefault_invalidType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, defaultDslConverterField0Path( index, dataSet ), invalidTypeParam() ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( defaultDslConverterField0Path( index, dataSet ) )
				) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void customDslConverter_valueModelDefault_validType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ), wrappedMatchingParam( 0,
						dataSet
				) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void customDslConverter_valueModelDefault_invalidType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, customDslConverterField0Path( index, dataSet ), invalidTypeParam() ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( customDslConverterField0Path( index, dataSet ) )
				) );
	}

	@Deprecated(since = "test")
	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void defaultDslConverter_valueConvertYes_validType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, defaultDslConverterField0Path( index, dataSet ),
						unwrappedMatchingParam( 0, dataSet ), org.hibernate.search.engine.search.common.ValueConvert.YES
				) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void defaultDslConverter_valueModelMapping_validType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, defaultDslConverterField0Path( index, dataSet ),
						unwrappedMatchingParam( 0, dataSet ), ValueModel.MAPPING ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void defaultDslConverter_valueConvertYes_invalidType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, defaultDslConverterField0Path( index, dataSet ), invalidTypeParam(),
				ValueModel.MAPPING ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( defaultDslConverterField0Path( index, dataSet ) )
				) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void customDslConverter_valueConvertYes_validType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ),
						wrappedMatchingParam( 0, dataSet ), ValueModel.MAPPING ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void customDslConverter_valueModelMapping_invalidType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, customDslConverterField0Path( index, dataSet ), invalidTypeParam(),
				ValueModel.MAPPING ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( customDslConverterField0Path( index, dataSet ) )
				) );
	}

	@Deprecated(since = "test")
	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void customDslConverter_valueConvertYes_invalidType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, customDslConverterField0Path( index, dataSet ), invalidTypeParam(),
				org.hibernate.search.engine.search.common.ValueConvert.YES ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( customDslConverterField0Path( index, dataSet ) )
				) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void defaultDslConverter_valueModelIndex_validType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, defaultDslConverterField0Path( index, dataSet ),
						unwrappedMatchingParam( 0, dataSet ), ValueModel.INDEX ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@Deprecated(since = "test")
	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void defaultDslConverter_valueConvertNo_validType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, defaultDslConverterField0Path( index, dataSet ),
						unwrappedMatchingParam( 0, dataSet ), org.hibernate.search.engine.search.common.ValueConvert.NO ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void defaultDslConverter_valueModelIndex_invalidType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, defaultDslConverterField0Path( index, dataSet ), invalidTypeParam(),
				ValueModel.INDEX ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( defaultDslConverterField0Path( index, dataSet ) )
				) );
	}

	@Deprecated(since = "test")
	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void defaultDslConverter_valueConvertNo_invalidType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, defaultDslConverterField0Path( index, dataSet ), invalidTypeParam(),
				org.hibernate.search.engine.search.common.ValueConvert.NO ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( defaultDslConverterField0Path( index, dataSet ) )
				) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void customDslConverter_valueModelIndex_validType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ),
						unwrappedMatchingParam( 0, dataSet ), ValueModel.INDEX ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@Deprecated(since = "test")
	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void customDslConverter_valueConvertNo_validType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ),
						unwrappedMatchingParam( 0, dataSet ), org.hibernate.search.engine.search.common.ValueConvert.NO ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void customDslConverter_valueModelIndex_invalidType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, customDslConverterField0Path( index, dataSet ), invalidTypeParam(),
				ValueModel.INDEX ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( customDslConverterField0Path( index, dataSet ) )
				) );
	}

	@Deprecated(since = "test")
	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void customDslConverter_valueConvertNo_invalidType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, customDslConverterField0Path( index, dataSet ), invalidTypeParam(),
				org.hibernate.search.engine.search.common.ValueConvert.NO ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( customDslConverterField0Path( index, dataSet ) )
				) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void multiFields_customDslConverter_valueModelMapping(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ), customDslConverterField1Path(
						index, dataSet ),
						wrappedMatchingParam( 0, dataSet ), ValueModel.MAPPING ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ), customDslConverterField1Path(
						index, dataSet ),
						wrappedMatchingParam( 1, dataSet ), ValueModel.MAPPING ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
	}

	@Deprecated(since = "test")
	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void multiFields_customDslConverter_valueConvertYes(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ), customDslConverterField1Path(
						index, dataSet ),
						wrappedMatchingParam( 0, dataSet ), org.hibernate.search.engine.search.common.ValueConvert.YES ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ), customDslConverterField1Path(
						index, dataSet ),
						wrappedMatchingParam( 1, dataSet ), org.hibernate.search.engine.search.common.ValueConvert.YES ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void multiFields_customDslConverter_valueModelIndex(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ), customDslConverterField1Path(
						index, dataSet ),
						unwrappedMatchingParam( 0, dataSet ), ValueModel.INDEX ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ), customDslConverterField1Path(
						index, dataSet ),
						unwrappedMatchingParam( 1, dataSet ), ValueModel.INDEX ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
	}

	@Deprecated(since = "test")
	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void multiFields_customDslConverter_valueConvertNo(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ), customDslConverterField1Path(
						index, dataSet ),
						unwrappedMatchingParam( 0, dataSet ), org.hibernate.search.engine.search.common.ValueConvert.NO ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ), customDslConverterField1Path(
						index, dataSet ),
						unwrappedMatchingParam( 1, dataSet ), org.hibernate.search.engine.search.common.ValueConvert.NO ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void multiIndex_withCompatibleIndex_valueModelMapping(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		StubMappingScope scope = index.createScope( compatibleIndex );

		assertThatQuery( scope.query()
				.where( f -> predicate( f, defaultDslConverterField0Path( index, dataSet ), unwrappedMatchingParam( 0,
						dataSet
				),
						ValueModel.MAPPING ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
					b.doc( compatibleIndex.typeName(), dataSet.docId( 0 ) );
				} );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void multiIndex_withRawFieldCompatibleIndex_valueModelMapping(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		StubMappingScope scope = index.createScope( rawFieldCompatibleIndex );

		String fieldPath = defaultDslConverterField0Path( index, dataSet );

		assertThatThrownBy( () -> predicate( scope.predicate(), fieldPath,
				unwrappedMatchingParam( 0, dataSet ), ValueModel.MAPPING ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Attribute 'mappingDslConverter' differs:", " vs. "
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( index.name(), rawFieldCompatibleIndex.name() )
				) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void multiIndex_withRawFieldCompatibleIndex_valueModelIndex(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		StubMappingScope scope = index.createScope( rawFieldCompatibleIndex );

		assertThatQuery( scope.query()
				.where( f -> predicate( f, defaultDslConverterField0Path( index, dataSet ),
						unwrappedMatchingParam( 0, dataSet ), ValueModel.INDEX ) )
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
	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4173")
	void multiIndex_withMissingFieldIndex_valueModelMapping(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		StubMappingScope scope = index.createScope( missingFieldIndex );

		// The predicate should not match anything in missingFieldIndex
		assertThatQuery( scope.query()
				.where( f -> predicate( f, defaultDslConverterField0Path( index, dataSet ), unwrappedMatchingParam( 0,
						dataSet
				),
						ValueModel.MAPPING ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
				} );

		// ... but it should not prevent the query from executing either:
		// if the predicate is optional, it should be ignored for missingFieldIndex.
		assertThatQuery( scope.query()
				.where( f -> f.or(
						predicate( f, defaultDslConverterField0Path( index, dataSet ), unwrappedMatchingParam( 0, dataSet ),
								ValueModel.MAPPING ),
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
	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4173")
	void multiIndex_withMissingFieldIndex_valueModelIndex(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		StubMappingScope scope = index.createScope( missingFieldIndex );

		// The predicate should not match anything in missingFieldIndex
		assertThatQuery( scope.query()
				.where( f -> predicate( f, defaultDslConverterField0Path( index, dataSet ), unwrappedMatchingParam( 0,
						dataSet
				),
						ValueModel.INDEX ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
				} );

		// ... but it should not prevent the query from executing either:
		// if the predicate is optional, it should be ignored for missingFieldIndex.
		assertThatQuery( scope.query()
				.where( f -> f.or(
						predicate( f, defaultDslConverterField0Path( index, dataSet ), unwrappedMatchingParam( 0, dataSet ),
								ValueModel.INDEX ),
						f.id().matching( dataSet.docId( DataSet.MISSING_FIELD_INDEX_DOC_ORDINAL ) ) ) ) )
				.hasDocRefHitsAnyOrder( c -> c
						.doc( index.typeName(), dataSet.docId( 0 ) )
						.doc( missingFieldIndex.typeName(), dataSet.docId( DataSet.MISSING_FIELD_INDEX_DOC_ORDINAL ) ) )
				.hasTotalHitCount( 2 );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void multiIndex_withIncompatibleIndex_valueModelMapping(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		StubMappingScope scope = index.createScope( incompatibleIndex );

		String fieldPath = defaultDslConverterField0Path( index, dataSet );

		assertThatThrownBy( () -> predicate( scope.predicate(), fieldPath,
				unwrappedMatchingParam( 0, dataSet ), ValueModel.MAPPING ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for '" + predicateTrait() + "'", " vs. "
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( index.name(), incompatibleIndex.name() )
				) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void multiIndex_withIncompatibleIndex_valueModelIndex(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		StubMappingScope scope = index.createScope( incompatibleIndex );

		String fieldPath = defaultDslConverterField0Path( index, dataSet );

		assertThatThrownBy( () -> predicate( scope.predicate(), fieldPath,
				unwrappedMatchingParam( 0, dataSet ), ValueModel.INDEX ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for '" + predicateTrait() + "'"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( index.name(), incompatibleIndex.name() )
				) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void defaultDslConverter_valueModelString_validType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ),
						stringMatchingParam( 0, dataSet ), ValueModel.STRING ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void defaultDslConverter_valueModelString_invalidType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, defaultDslConverterField0Path( index, dataSet ), invalidTypeParam(),
				ValueModel.STRING ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( defaultDslConverterField0Path( index, dataSet ) )
				) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void customDslConverter_valueModelString_validType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ),
						stringMatchingParam( 0, dataSet ), ValueModel.STRING ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void customDslConverter_valueModelString_invalidType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, customDslConverterField0Path( index, dataSet ), invalidTypeParam(),
				ValueModel.STRING ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( customDslConverterField0Path( index, dataSet ) )
				) );
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("integerIndexParams")
	void customParser_valueModelString_validType(SimpleMappedIndex<IndexBinding> integerIndex, DataSet<?, V> dataSet) {
		for ( Map.Entry<String, Integer> entry : IndexIntegerBinding.Converter.NUMBERS.entrySet() ) {
			assertThatQuery( integerIndex.query()
					.where( f -> predicate( f, "integer", stringMatchingParamCustomParser( entry.getValue(), dataSet ),
							ValueModel.STRING ) )
			).hasDocRefHitsAnyOrder( integerIndex.typeName(), dataSet.docId( entry.getValue() ) );
		}
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void defaultDslConverter_valueModelRaw_validType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ),
						rawMatchingParam( 0, dataSet ), ValueModel.RAW ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void defaultDslConverter_valueModelRaw_invalidType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> predicate( f, defaultDslConverterField0Path( index, dataSet ), invalidTypeParam(),
				ValueModel.RAW ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to convert DSL argument: " )
				.hasMessageContaining( InvalidType.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( defaultDslConverterField0Path( index, dataSet ) )
				) );
	}

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, P matchingParam);

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, P matchingParam,
			ValueModel valueModel);

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String field0Path, String field1Path,
			P matchingParam, ValueModel valueModel);

	@Deprecated(since = "test")
	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, P matchingParam,
			org.hibernate.search.engine.search.common.ValueConvert valueConvert);

	@Deprecated(since = "test")
	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String field0Path, String field1Path,
			P matchingParam, org.hibernate.search.engine.search.common.ValueConvert valueConvert);

	protected abstract P invalidTypeParam();

	protected abstract P unwrappedMatchingParam(int matchingDocOrdinal, DataSet<?, V> dataSet);

	protected abstract P wrappedMatchingParam(int matchingDocOrdinal, DataSet<?, V> dataSet);

	protected abstract P stringMatchingParam(int matchingDocOrdinal, DataSet<?, V> dataSet);

	protected abstract P stringMatchingParamCustomParser(int matchingDocOrdinal, DataSet<?, V> dataSet);

	protected abstract P rawMatchingParam(int matchingDocOrdinal, DataSet<?, V> dataSet);

	protected abstract String predicateTrait();

	private String defaultDslConverterField0Path(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		return index.binding().defaultDslConverterField0.get( dataSet.fieldType ).relativeFieldName;
	}

	private String customDslConverterField0Path(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		return index.binding().customDslConverterField0.get( dataSet.fieldType ).relativeFieldName;
	}

	private String customDslConverterField1Path(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		return index.binding().customDslConverterField1.get( dataSet.fieldType ).relativeFieldName;
	}

	public static final class IndexBinding {
		private final SimpleFieldModelsByType defaultDslConverterField0;
		private final SimpleFieldModelsByType customDslConverterField0;
		private final SimpleFieldModelsByType customDslConverterField1;

		public IndexBinding(IndexSchemaElement root, Collection<
				? extends FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
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

		public CompatibleIndexBinding(IndexSchemaElement root, Collection<
				? extends FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
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
		protected void addIrrelevantOptions(FieldTypeDescriptor<?, ?> fieldType,
				SearchableProjectableIndexFieldTypeOptionsStep<?, ?> c) {
			c.projectable( Projectable.YES );
			if ( fieldType.isFieldSortSupported() && fieldType.isFieldAggregationSupported() ) {
				( (StandardIndexFieldTypeOptionsStep<?, ?>) c ).sortable( Sortable.YES );
				( (StandardIndexFieldTypeOptionsStep<?, ?>) c ).aggregable( Aggregable.YES );
			}
		}
	}

	public static final class RawFieldCompatibleIndexBinding {
		private final SimpleFieldModelsByType defaultDslConverterField0;
		private final SimpleFieldModelsByType customDslConverterField0;
		private final SimpleFieldModelsByType customDslConverterField1;

		public RawFieldCompatibleIndexBinding(IndexSchemaElement root,
				Collection<? extends FieldTypeDescriptor<?,
						? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
			defaultDslConverterField0 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "defaultDslConverterField0_",
					c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() ) );
			customDslConverterField0 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "customDslConverterField0_" );
			customDslConverterField1 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "customDslConverterField1_" );
		}
	}

	public static final class IncompatibleIndexBinding {
		public IncompatibleIndexBinding(IndexSchemaElement root, Collection<
				? extends FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
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
		public MissingFieldIndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?, ?>> fieldTypes) {
		}
	}

	public static final class IndexIntegerBinding {
		private final IndexFieldReference<Integer> integer;

		public IndexIntegerBinding(IndexSchemaElement root) {
			integer = root.field( "integer", f -> f.asInteger().parser( Converter.INSTANCE ) ).toReference();
		}

		public static BulkIndexer contribute(SimpleMappedIndex<IndexIntegerBinding> index) {
			BulkIndexer indexer = index.bulkIndexer();
			for ( int i = 0; i < 10; i++ ) {
				int value = i;
				indexer.add( docId( i ), d -> d.addValue( index.binding().integer, value ) );
			}
			return indexer;
		}

		private static String docId(int docOrdinal) {
			return IntegerFieldTypeDescriptor.INSTANCE.getUniqueName() + "_doc_" + docOrdinal;
		}

		public static class Converter implements ToDocumentValueConverter<String, Integer> {

			public static final Converter INSTANCE = new Converter();

			public static final Map<String, Integer> NUMBERS = Map.of(
					"zero", 0,
					"one", 1,
					"two", 2,
					"three", 3,
					"four", 4,
					"five", 5,
					"six", 6,
					"seven", 7,
					"eight", 8,
					"nine", 9
			);

			public static String string(Object value) {
				for ( Map.Entry<String, Integer> entry : NUMBERS.entrySet() ) {
					if ( entry.getValue().equals( value ) ) {
						return entry.getKey();
					}
				}
				throw new AssertionFailure(
						"Unexpected string-integer " + value + ". Only 0-9 values are supported by this converter." );
			}

			@Override
			public Integer toDocumentValue(String value, ToDocumentValueConvertContext context) {
				if ( value == null ) {
					return null;
				}
				if ( !NUMBERS.containsKey( value.toLowerCase( Locale.ROOT ) ) ) {
					throw new AssertionFailure(
							"Unexpected string-integer " + value + ". Only 0-9 values are supported by this converter." );
				}
				return NUMBERS.get( value.toLowerCase( Locale.ROOT ) );
			}

			@Override
			public boolean isCompatibleWith(ToDocumentValueConverter<?, ?> other) {
				return other == this;
			}
		}
	}

	public static final class DataSet<F, V extends AbstractPredicateTestValues<F>>
			extends AbstractPerFieldTypePredicateDataSet<F, V> {
		public static final int MISSING_FIELD_INDEX_DOC_ORDINAL = 100;

		public DataSet(V values) {
			super( values );
		}

		@SuppressWarnings("unused") // For EJC and lambda arg
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
