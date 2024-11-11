/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.time.temporal.TemporalAccessor;
import java.util.Comparator;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingBackendFeatures;

public abstract class TckBackendFeatures implements StubMappingBackendFeatures {

	public boolean normalizesStringMissingValues() {
		return true;
	}

	public boolean normalizesStringArgumentToWildcardPredicateForAnalyzedStringField() {
		return true;
	}

	public boolean normalizesStringArgumentToRangePredicateForAnalyzedStringField() {
		return true;
	}

	public boolean nonCanonicalRangeInAggregations() {
		return true;
	}

	public boolean nonDefaultOrderInTermsAggregations() {
		return true;
	}

	public boolean projectionPreservesNulls() {
		return true;
	}

	public abstract boolean fieldsProjectableByDefault();

	public boolean supportsTotalHitsThresholdForScroll() {
		return true;
	}

	public boolean supportsTruncateAfterForScroll() {
		return true;
	}

	public boolean supportsExistsForFieldWithoutDocValues(Class<?> fieldType) {
		return true;
	}

	public boolean geoDistanceSortingSupportsConfigurableMissingValues() {
		return true;
	}

	public boolean regexpExpressionIsNormalized() {
		return false;
	}

	public boolean termsArgumentsAreNormalized() {
		return false;
	}

	public boolean supportsDistanceSortWhenNestedFieldMissingInSomeTargetIndexes() {
		return true;
	}

	public boolean supportsFieldSortWhenFieldMissingInSomeTargetIndexes(Class<?> fieldType) {
		return true;
	}

	public boolean projectionPreservesEmptySingleValuedObject(ObjectStructure structure) {
		return true;
	}

	public abstract boolean reliesOnNestedDocumentsForMultiValuedObjectProjection();

	public boolean supportsYearType() {
		return true;
	}

	public boolean supportsExtremeLongValues() {
		return true;
	}

	public boolean supportsExtremeScaledNumericValues() {
		return true;
	}

	public boolean supportsHighlightableWithoutProjectable() {
		return true;
	}

	public boolean supportsHighlighterEncoderAtFieldLevel() {
		return true;
	}

	public boolean supportsHighlighterUnifiedTypeNoMatchSize() {
		return true;
	}

	public boolean supportsHighlighterUnifiedTypeFragmentSize() {
		return true;
	}

	public boolean supportsHighlighterFastVectorNoMatchSizeOnMultivaluedFields() {
		return true;
	}

	public boolean supportsHighlighterPlainOrderByScoreMultivaluedField() {
		return true;
	}

	public boolean supportsHighlighterUnifiedPhraseMatching() {
		return false;
	}

	public boolean supportsExplicitMergeSegments() {
		return true;
	}

	public boolean supportsExplicitPurge() {
		return true;
	}

	public boolean supportsExplicitFlush() {
		return true;
	}

	@Override
	public boolean supportsExplicitRefresh() {
		return true;
	}

	public boolean supportsVectorSearch() {
		return true;
	}

	public boolean supportsVectorSearchRequiredMinimumSimilarity() {
		return true;
	}

	public boolean supportsSimilarity(VectorSimilarity vectorSimilarity) {
		return true;
	}

	public boolean hasBuiltInAnalyzerDescriptorsAvailable() {
		return true;
	}

	public boolean canPerformTermsQuery(FieldTypeDescriptor<?, ?> fieldType) {
		return true;
	}

	public boolean knnWorksInsideNestedPredicateWithImplicitFilters() {
		return true;
	}

	public <F> String formatForQueryStringPredicate(FieldTypeDescriptor<F, ?> descriptor, F value) {
		return descriptor.format( value );
	}

	public boolean queryStringFailOnPatternQueries() {
		return true;
	}

	public boolean vectorSearchRequiredMinimumSimilarityAsLucene() {
		return true;
	}

	public abstract <F> Object toRawValue(FieldTypeDescriptor<F, ?> descriptor, F value);

	// with some backends sorts require a different raw value to what other places like predicates will allow.
	// E.g. Elasticsearch won't accept the formatted string date-time types and expects it to be in form of a number instead.
	public <F> Object toSortRawValue(FieldTypeDescriptor<F, ?> descriptor, F value) {
		return toRawValue( descriptor, value );
	}

	public abstract <F> Class<?> rawType(FieldTypeDescriptor<F, ?> descriptor);

	public <F> String toStringValue(FieldTypeDescriptor<F, ?> descriptor, F value) {
		return descriptor.format( value );
	}

	public Optional<Comparator<? super Object>> rawValueEqualsComparator(FieldTypeDescriptor<?, ?> fieldType) {
		return Optional.empty();
	}

	public boolean normalizesStringArgumentToPrefixPredicateForAnalyzedStringField() {
		return true;
	}

	public boolean rangeAggregationsDoNotIgnoreQuery() {
		return true;
	}

	public boolean negativeDecimalScaleIsAppliedToAvgAggregationFunction() {
		return true;
	}

	public <F, T> T fromRawAggregation(FieldTypeDescriptor<F, ?> typeDescriptor, T value) {
		return value;
	}

	public <F> Double toDoubleValue(FieldTypeDescriptor<F, ?> descriptor, F fieldValue) {
		if ( Number.class.isAssignableFrom( descriptor.getJavaType() ) ) {
			return ( (Number) fieldValue ).doubleValue();
		}

		if ( TemporalAccessor.class.isAssignableFrom( descriptor.getJavaType() ) ) {
			return ( (Number) toSortRawValue( descriptor, fieldValue ) ).doubleValue();
		}

		throw new UnsupportedOperationException( "Type " + descriptor.getJavaType() + " is not supported" );
	}

	public <F> boolean rawAggregationProduceSensibleDoubleValue(FieldTypeDescriptor<F, ?> fFieldTypeDescriptor) {
		return true;
	}

	public <U, R> R accumulatedNullValue(ProjectionCollector.Provider<U, R> collector) {
		return collector.get().empty();
	}
}
