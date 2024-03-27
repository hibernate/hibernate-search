/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class AbstractPredicateTypeCheckingAndConversionIT<V extends AbstractPredicateTestValues<?>, P> {

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void defaultDslConverter_valueConvertDefault_validType(SimpleMappedIndex<IndexBinding> index,
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
	void defaultDslConverter_valueConvertDefault_invalidType(SimpleMappedIndex<IndexBinding> index,
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
	void customDslConverter_valueConvertDefault_validType(SimpleMappedIndex<IndexBinding> index,
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
	void customDslConverter_valueConvertDefault_invalidType(SimpleMappedIndex<IndexBinding> index,
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
						unwrappedMatchingParam( 0, dataSet ), ValueConvert.YES ) )
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
				ValueConvert.YES ) )
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
						wrappedMatchingParam( 0, dataSet ), ValueConvert.YES ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

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
				ValueConvert.YES ) )
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
	void defaultDslConverter_valueConvertNo_validType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, defaultDslConverterField0Path( index, dataSet ),
						unwrappedMatchingParam( 0, dataSet ), ValueConvert.NO ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

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
				ValueConvert.NO ) )
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
	void customDslConverter_valueConvertNo_validType(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ),
						unwrappedMatchingParam( 0, dataSet ), ValueConvert.NO ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

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
				ValueConvert.NO ) )
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
	void multiFields_customDslConverter_valueConvertYes(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ), customDslConverterField1Path(
						index, dataSet ),
						wrappedMatchingParam( 0, dataSet ), ValueConvert.YES ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ), customDslConverterField1Path(
						index, dataSet ),
						wrappedMatchingParam( 1, dataSet ), ValueConvert.YES ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
	}

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
						unwrappedMatchingParam( 0, dataSet ), ValueConvert.NO ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( index.query()
				.where( f -> predicate( f, customDslConverterField0Path( index, dataSet ), customDslConverterField1Path(
						index, dataSet ),
						unwrappedMatchingParam( 1, dataSet ), ValueConvert.NO ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void multiIndex_withCompatibleIndex_valueConvertYes(SimpleMappedIndex<IndexBinding> index,
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
						ValueConvert.YES ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
					b.doc( compatibleIndex.typeName(), dataSet.docId( 0 ) );
				} );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void multiIndex_withRawFieldCompatibleIndex_valueConvertYes(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		StubMappingScope scope = index.createScope( rawFieldCompatibleIndex );

		String fieldPath = defaultDslConverterField0Path( index, dataSet );

		assertThatThrownBy( () -> predicate( scope.predicate(), fieldPath,
				unwrappedMatchingParam( 0, dataSet ), ValueConvert.YES ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Attribute 'dslConverter' differs:", " vs. "
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( index.name(), rawFieldCompatibleIndex.name() )
				) );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void multiIndex_withRawFieldCompatibleIndex_valueConvertNo(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		StubMappingScope scope = index.createScope( rawFieldCompatibleIndex );

		assertThatQuery( scope.query()
				.where( f -> predicate( f, defaultDslConverterField0Path( index, dataSet ),
						unwrappedMatchingParam( 0, dataSet ), ValueConvert.NO ) )
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
	void multiIndex_withMissingFieldIndex_valueConvertYes(SimpleMappedIndex<IndexBinding> index,
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
						ValueConvert.YES ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
				} );

		// ... but it should not prevent the query from executing either:
		// if the predicate is optional, it should be ignored for missingFieldIndex.
		assertThatQuery( scope.query()
				.where( f -> f.or(
						predicate( f, defaultDslConverterField0Path( index, dataSet ), unwrappedMatchingParam( 0, dataSet ),
								ValueConvert.YES ),
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
	void multiIndex_withMissingFieldIndex_valueConvertNo(SimpleMappedIndex<IndexBinding> index,
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
						ValueConvert.NO ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
				} );

		// ... but it should not prevent the query from executing either:
		// if the predicate is optional, it should be ignored for missingFieldIndex.
		assertThatQuery( scope.query()
				.where( f -> f.or(
						predicate( f, defaultDslConverterField0Path( index, dataSet ), unwrappedMatchingParam( 0, dataSet ),
								ValueConvert.NO ),
						f.id().matching( dataSet.docId( DataSet.MISSING_FIELD_INDEX_DOC_ORDINAL ) ) ) ) )
				.hasDocRefHitsAnyOrder( c -> c
						.doc( index.typeName(), dataSet.docId( 0 ) )
						.doc( missingFieldIndex.typeName(), dataSet.docId( DataSet.MISSING_FIELD_INDEX_DOC_ORDINAL ) ) )
				.hasTotalHitCount( 2 );
	}

	@ParameterizedTest(name = "{5}")
	@MethodSource("params")
	void multiIndex_withIncompatibleIndex_valueConvertYes(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		StubMappingScope scope = index.createScope( incompatibleIndex );

		String fieldPath = defaultDslConverterField0Path( index, dataSet );

		assertThatThrownBy( () -> predicate( scope.predicate(), fieldPath,
				unwrappedMatchingParam( 0, dataSet ), ValueConvert.YES ) )
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
	void multiIndex_withIncompatibleIndex_valueConvertNo(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex,
			SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet<?, V> dataSet) {
		StubMappingScope scope = index.createScope( incompatibleIndex );

		String fieldPath = defaultDslConverterField0Path( index, dataSet );

		assertThatThrownBy( () -> predicate( scope.predicate(), fieldPath,
				unwrappedMatchingParam( 0, dataSet ), ValueConvert.NO ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for '" + predicateTrait() + "'"
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

	protected abstract P unwrappedMatchingParam(int matchingDocOrdinal, DataSet<?, V> dataSet);

	protected abstract P wrappedMatchingParam(int matchingDocOrdinal, DataSet<?, V> dataSet);

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
