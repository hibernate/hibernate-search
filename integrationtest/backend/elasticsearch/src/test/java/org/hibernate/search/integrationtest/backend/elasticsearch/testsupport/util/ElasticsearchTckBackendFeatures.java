/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect.isActualVersion;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.types.format.impl.ElasticsearchDefaultFieldFormatProvider;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.cfg.spi.FormatUtils;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.BigDecimalFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FloatFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.InstantFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.LocalDateFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.LocalDateTimeFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.LocalTimeFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.MonthDayFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.OffsetDateTimeFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.OffsetTimeFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.YearFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.YearMonthFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.ZonedDateTimeFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ElasticsearchTckBackendFeatures extends TckBackendFeatures {

	private Gson gson = new GsonBuilder().setPrettyPrinting().create();

	ElasticsearchTckBackendFeatures() {
	}

	@Override
	public boolean normalizesStringMissingValues() {
		// TODO HSEARCH-3387 Elasticsearch does not apply the normalizer defined on the field
		//   to the String provided as replacement for missing values on sorting
		return false;
	}

	@Override
	public boolean normalizesStringArgumentToWildcardPredicateForAnalyzedStringField() {
		// In ES 7.7 through 7.11, wildcard predicates on analyzed fields get their pattern normalized,
		// but that was deemed a bug and fixed in 7.12.2+: https://github.com/elastic/elasticsearch/pull/53127
		// Apparently the "fix" was also introduced in OpenSearch 2.5.0
		return isActualVersion(
				esVersion -> esVersion.isBetween( "7.7", "7.12.1" ),
				osVersion -> osVersion.isLessThan( "2.5.0" )
		);
	}

	@Override
	public boolean normalizesStringArgumentToRangePredicateForAnalyzedStringField() {
		// TODO HSEARCH-3959 Elasticsearch does not normalizes arguments passed to the range predicate
		//   for text fields.
		return false;
	}

	@Override
	public boolean nonCanonicalRangeInAggregations() {
		// Elasticsearch only supports [a, b), (-Infinity, b), [a, +Infinity), but not [a, b] for example.
		return false;
	}

	@Override
	public boolean fieldsProjectableByDefault() {
		return true;
	}

	@Override
	public boolean supportsTotalHitsThresholdForScroll() {
		// If we try to customize track_total_hits for a scroll, we get an error:
		// "disabling [track_total_hits] is not allowed in a scroll context"
		return false;
	}

	@Override
	public boolean supportsTruncateAfterForScroll() {
		// See https://hibernate.atlassian.net/browse/HSEARCH-4029
		return false;
	}

	@Override
	public boolean supportsExistsForFieldWithoutDocValues(Class<?> fieldType) {
		if ( GeoPoint.class.equals( fieldType ) ) {
			return false;
		}
		return true;
	}

	@Override
	public boolean geoDistanceSortingSupportsConfigurableMissingValues() {
		// See https://www.elastic.co/guide/en/elasticsearch/reference/7.10/sort-search-results.html
		// In particular:
		// geo distance sorting does not support configurable missing values:
		// the distance will always be considered equal to Infinity when a document does not have values for the field
		// that is used for distance computation.
		return false;
	}

	@Override
	public boolean regexpExpressionIsNormalized() {
		// Surprisingly it is!
		// See *.tck.search.predicate.RegexpPredicateSpecificsIT#normalizedField
		return true;
	}

	@Override
	public boolean termsArgumentsAreNormalized() {
		// Surprisingly it is!
		// See *.tck.search.predicate.TermsPredicateSpecificsIT#normalizedField_termIsNotNormalized
		return true;
	}

	@Override
	public boolean supportsDistanceSortWhenNestedFieldMissingInSomeTargetIndexes() {
		// Even with ignore_unmapped: true,
		// the distance sort will fail if the nested field doesn't exist in one index.
		// Elasticsearch complains it cannot find the nested field
		// ("[nested] failed to find nested object under path [nested]"),
		// but we don't have any way to tell it to ignore this.
		// See https://hibernate.atlassian.net/browse/HSEARCH-4179
		return false;
	}

	@Override
	public boolean supportsFieldSortWhenFieldMissingInSomeTargetIndexes(Class<?> fieldType) {
		// We cannot use unmapped_type for scaled floats:
		// Elasticsearch complains it needs a scaling factor, but we don't have any way to provide it.
		// See https://hibernate.atlassian.net/browse/HSEARCH-4176
		return !BigInteger.class.equals( fieldType ) && !BigDecimal.class.equals( fieldType );
	}

	@Override
	public boolean reliesOnNestedDocumentsForMultiValuedObjectProjection() {
		return false;
	}

	@Override
	public boolean supportsYearType() {
		// https://github.com/elastic/elasticsearch/issues/90187
		// Seems like this was fixed in 8.5.1 and they won't backport to 8.4:
		// https://github.com/elastic/elasticsearch/pull/90458
		return isActualVersion(
				esVersion -> !esVersion.isBetween( "8.4.2", "8.5.0" ),
				osVersion -> true
		);
	}

	@Override
	public boolean supportsExtremeScaledNumericValues() {
		// https://github.com/elastic/elasticsearch/issues/91246
		// Hopefully this will get fixed in a future version.
		return isActualVersion(
				esVersion -> !esVersion.isBetween( "7.17.7", "7.17" ) && esVersion.isLessThan( "8.5.0" ),
				// https://github.com/opensearch-project/OpenSearch/issues/12433
				osVersion -> osVersion.isLessThan( "2.12.0" )
		);
	}

	@Override
	public boolean supportsExtremeLongValues() {
		// https://github.com/elastic/elasticsearch/issues/84601
		// There doesn't seem to be any hope for this to get fixed in older versions (7.17/8.0),
		// but it's been fixed in 8.1.
		return isActualVersion(
				esVersion -> !esVersion.isBetween( "7.17.7", "7.17" ) && !esVersion.isBetween( "8.0.0", "8.0" ),
				osVersion -> true
		);
	}

	@Override
	public boolean supportsHighlighterEncoderAtFieldLevel() {
		// https://github.com/elastic/elasticsearch/issues/94028
		return false;
	}

	@Override
	public boolean supportsHighlighterFastVectorNoMatchSizeOnMultivaluedFields() {
		// https://github.com/elastic/elasticsearch/issues/94550
		return false;
	}

	@Override
	public boolean supportsHighlighterPlainOrderByScoreMultivaluedField() {
		// A plain highlighter implementation in ES had a bug
		// https://github.com/elastic/elasticsearch/issues/87210
		// that is now fixed with https://github.com/elastic/elasticsearch/pull/87414
		return isActualVersion(
				esVersion -> !esVersion.isBetween( "7.15", "8.3" ),
				osVersion -> true
		);
	}

	@Override
	public boolean supportsHighlighterUnifiedPhraseMatching() {
		return isActualVersion(
				esVersion -> !esVersion.isAtMost( "8.9" ),
				osVersion -> false
		);
	}

	public static boolean supportsIndexClosingAndOpening() {
		return isActualVersion(
				// See https://docs.aws.amazon.com/opensearch-service/latest/developerguide/supported-operations.html#version_7_10
				es -> true,
				// See https://docs.aws.amazon.com/opensearch-service/latest/developerguide/supported-operations.html#version_opensearch_1.0
				os -> true,
				aoss -> false
		);
	}

	public static boolean supportsVersionCheck() {
		return isActualVersion(
				es -> true,
				os -> true,
				aoss -> false
		);
	}

	public static boolean supportsIndexStatusCheck() {
		return isActualVersion(
				es -> true,
				os -> true,
				aoss -> false
		);
	}

	@Override
	public boolean supportsExplicitPurge() {
		return ElasticsearchTestDialect.get().supportsExplicitPurge();
	}

	@Override
	public boolean supportsExplicitMergeSegments() {
		return isActualVersion(
				es -> true,
				os -> true,
				aoss -> false
		);
	}

	@Override
	public boolean supportsExplicitFlush() {
		return isActualVersion(
				es -> true,
				os -> true,
				aoss -> false
		);
	}

	@Override
	public boolean supportsExplicitRefresh() {
		return isActualVersion(
				es -> true,
				os -> true,
				aoss -> false
		);
	}

	@Override
	public boolean supportsVectorSearch() {
		return isActualVersion(
				es -> !es.isLessThan( "8.12.0" ),
				os -> !os.isLessThan( "2.9.0" ),
				aoss -> true
		);
	}

	@Override
	public boolean supportsVectorSearchRequiredMinimumSimilarity() {
		return isActualVersion(
				es -> true,
				os -> !os.isLessThan( "2.14.0" ),
				aoss -> true
		);
	}

	@Override
	public boolean supportsSimilarity(VectorSimilarity vectorSimilarity) {
		switch ( vectorSimilarity ) {
			case DOT_PRODUCT:
			case MAX_INNER_PRODUCT:
				return isActualVersion(
						es -> true,
						os -> false,
						aoss -> false
				);
			default:
				return true;
		}
	}

	@Override
	public boolean hasBuiltInAnalyzerDescriptorsAvailable() {
		return false;
	}

	@Override
	public boolean canPerformTermsQuery(FieldTypeDescriptor<?, ?> fieldType) {
		return isActualVersion(
				es -> true,
				// https://github.com/opensearch-project/OpenSearch/issues/12432
				os -> !os.isMatching( "2.12.0" ) || !FloatFieldTypeDescriptor.INSTANCE.equals( fieldType ),
				aoss -> true
		);
	}

	@Override
	public boolean knnWorksInsideNestedPredicateWithImplicitFilters() {
		return false;
	}

	@Override
	public <F> String formatForQueryStringPredicate(FieldTypeDescriptor<F, ?> descriptor, F value) {
		ElasticsearchDefaultFieldFormatProvider formatProvider = ElasticsearchTestDialect.get().getDefaultFieldFormatProvider();
		if ( TemporalAccessor.class.isAssignableFrom( descriptor.getJavaType() ) ) {
			@SuppressWarnings("unchecked")
			var formatter = formatProvider
					.getDefaultDateTimeFormatter( ( (Class<? extends TemporalAccessor>) descriptor.getJavaType() ) )
					.withLocale( Locale.ROOT );
			if ( InstantFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
				return formatter
						.withZone( ZoneOffset.UTC )
						.format( (Instant) value );
			}
			if ( MonthDayFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
				return formatter
						.format( LocalDate.of( 0, ( (MonthDay) value ).getMonth(), ( (MonthDay) value ).getDayOfMonth() ) );
			}
			return formatter.format( (TemporalAccessor) value );
		}

		return super.formatForQueryStringPredicate( descriptor, value );
	}

	@Override
	public boolean queryStringFailOnPatternQueries() {
		return isActualVersion(
				es -> true,
				os -> false,
				aoss -> false
		);
	}

	@Override
	public boolean vectorSearchRequiredMinimumSimilarityAsLucene() {
		return isActualVersion(
				es -> true,
				os -> false,
				aoss -> false
		);
	}

	@Override
	public <F> Object toRawValue(FieldTypeDescriptor<F, ?> descriptor, F value) {
		if ( TemporalAccessor.class.isAssignableFrom( descriptor.getJavaType() ) ) {
			return gson.toJson( formatForQueryStringPredicate( descriptor, value ) );
		}

		if ( GeoPointFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			// we use a linked map to preserve the order of keys so that we get a predictable string.
			var point = new LinkedHashMap<>();
			point.put( "lat", ( (GeoPoint) value ).latitude() );
			point.put( "lon", ( (GeoPoint) value ).longitude() );
			return gson.toJson( point );
		}
		return gson.toJson( value );
	}

	@Override
	public <F> Object toSortRawValue(FieldTypeDescriptor<F, ?> descriptor, F value) {
		// see corresponding Elasticsearch codecs #nullUnsafeScalar(..)
		if ( InstantFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return Objects.toString( ( (Instant) value ).toEpochMilli() );
		}
		if ( YearFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return Objects.toString( ( (Year) value ).atDay( 1 ).atStartOfDay().toInstant( ZoneOffset.UTC ).toEpochMilli() );
		}
		if ( ZonedDateTimeFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return Objects.toString( ( (ZonedDateTime) value ).toInstant().toEpochMilli() );
		}
		if ( LocalDateTimeFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return Objects.toString( ( (LocalDateTime) value ).toInstant( ZoneOffset.UTC ).toEpochMilli() );
		}
		if ( LocalTimeFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return Objects.toString( ( (LocalTime) value ).atDate( LocalDate.of( 1970, Month.JANUARY, 1 ) )
					.toInstant( ZoneOffset.UTC ).toEpochMilli() );
		}
		if ( LocalDateFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return Objects.toString( ( (LocalDate) value ).atStartOfDay( ZoneOffset.UTC ).toInstant().toEpochMilli() );
		}
		if ( MonthDayFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return Objects
					.toString( ( (MonthDay) value ).atYear( 0 ).atStartOfDay().toInstant( ZoneOffset.UTC ).toEpochMilli() );
		}
		if ( YearMonthFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return Objects
					.toString( ( (YearMonth) value ).atDay( 1 ).atStartOfDay().toInstant( ZoneOffset.UTC ).toEpochMilli() );
		}
		if ( OffsetDateTimeFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return Objects.toString( ( (OffsetDateTime) value ).toInstant().toEpochMilli() );
		}
		if ( OffsetTimeFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			return Objects.toString(
					( (OffsetTime) value ).atDate( LocalDate.of( 1970, Month.JANUARY, 1 ) ).toInstant().toEpochMilli() );
		}
		return super.toSortRawValue( descriptor, value );
	}

	@Override
	public <F> Class<?> rawType(FieldTypeDescriptor<F, ?> descriptor) {
		return String.class;
	}

	@Override
	public <F> String toStringValue(FieldTypeDescriptor<F, ?> descriptor, F value) {
		if ( BigDecimalFieldTypeDescriptor.INSTANCE.equals( descriptor ) ) {
			if ( ( (BigDecimal) value ).scale() <= 0 ) {
				return FormatUtils.format( ( (BigDecimal) value ).setScale( 1, RoundingMode.UNNECESSARY ) );
			}
			return FormatUtils.format( ( (BigDecimal) value ) );
		}
		return super.toStringValue( descriptor, value );
	}

	@Override
	public Optional<Comparator<? super Object>> rawValueEqualsComparator(FieldTypeDescriptor<?, ?> fieldType) {
		if ( GeoPointFieldTypeDescriptor.INSTANCE.equals( fieldType ) ) {
			return Optional.of( (Comparator<Object>) (o1, o2) -> {
				// we have exact string match or both are null
				if ( Objects.equals( o1, o2 ) ) {
					return 0;
				}
				// if one is null and the other is not - not equal
				if ( o1 == null || o2 == null ) {
					return -1;
				}
				// compare parsed JSONs to address possible attribute order change:
				return gson.fromJson( (String) o1, JsonElement.class )
						.equals( gson.fromJson( (String) o2, JsonElement.class ) ) ? 0 : -1;
			} );
		}
		return super.rawValueEqualsComparator( fieldType );
	}

	@Override
	public boolean normalizesStringArgumentToPrefixPredicateForAnalyzedStringField() {
		return false;
	}

	@Override
	public boolean rangeAggregationsDoNotIgnoreQuery() {
		// See https://github.com/opensearch-project/OpenSearch/issues/15169
		//  There is a problem with OpenSearch 2.16.0 where query is ignored for a range aggregation,
		//  leading to routes, being ignored and incorrect counts produced in the results:
		return isActualVersion(
				es -> true,
				os -> !os.isMatching( "2.16.0" ),
				aoss -> false
		);
	}

	@Override
	public boolean negativeDecimalScaleIsAppliedToAvgAggregationFunction() {
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <F, T> T fromRawAggregation(FieldTypeDescriptor<F, ?> typeDescriptor, T value) {
		JsonObject jsonObject = gson.fromJson( value.toString(), JsonObject.class );
		return (T) ( gson.toJson( jsonObject.has( "value_as_string" )
				? jsonObject.get( "value_as_string" )
				: jsonObject.get( "value" ) ) );
	}

	@Override
	public <F> boolean rawAggregationProduceSensibleDoubleValue(FieldTypeDescriptor<F, ?> fFieldTypeDescriptor) {
		if ( YearFieldTypeDescriptor.INSTANCE.equals( fFieldTypeDescriptor )
				|| YearMonthFieldTypeDescriptor.INSTANCE.equals( fFieldTypeDescriptor )
				|| LocalDateFieldTypeDescriptor.INSTANCE.equals( fFieldTypeDescriptor ) ) {
			return false;
		}
		return super.rawAggregationProduceSensibleDoubleValue( fFieldTypeDescriptor );
	}

	@Override
	public <U, R> R accumulatedNullValue(ProjectionCollector.Provider<U, R> collector) {
		return accumulatedNullValue( collector.get() );
	}

	private <U, R, A> R accumulatedNullValue(ProjectionCollector<Object, U, A, R> collector) {
		ArrayList<Object> values = new ArrayList<>();
		values.add( null );
		return collector.finish( collector.accumulateAll( collector.createInitial(), values ) );
	}
}
