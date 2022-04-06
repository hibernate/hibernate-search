/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;

class ElasticsearchTckBackendFeatures extends TckBackendFeatures {

	private final ElasticsearchTestDialect dialect;

	ElasticsearchTckBackendFeatures(ElasticsearchTestDialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public boolean geoPointIndexNullAs() {
		return dialect.supportsGeoPointIndexNullAs();
	}

	@Override
	public boolean worksFineWithStrictAboveRangedQueriesOnDecimalScaledField() {
		return dialect.supportsStrictGreaterThanRangedQueriesOnScaledFloatField();
	}

	@Override
	public boolean normalizesStringMissingValues() {
		// TODO HSEARCH-3387 Elasticsearch does not apply the normalizer defined on the field
		//   to the String provided as replacement for missing values on sorting
		return false;
	}

	@Override
	public boolean normalizesStringArgumentToWildcardPredicateForAnalyzedStringField() {
		return dialect.normalizesStringArgumentToWildcardPredicateForAnalyzedStringField();
	}

	@Override
	public boolean normalizesStringArgumentToRangePredicateForAnalyzedStringField() {
		// TODO HSEARCH-3959 Elasticsearch does not normalizes arguments passed to the range predicate
		//   for text fields.
		return false;
	}

	@Override
	public boolean zonedDateTimeDocValueHasUTCZoneId() {
		return dialect.zonedDateTimeDocValueHasUTCZoneId();
	}

	@Override
	public boolean nonCanonicalRangeInAggregations() {
		// Elasticsearch only supports [a, b), (-Infinity, b), [a, +Infinity), but not [a, b] for example.
		return false;
	}

	@Override
	public boolean lenientOnMultiIndexesCompatibilityChecks() {
		return true;
	}

	@Override
	public boolean sortByFieldValue(TestedFieldStructure fieldStructure, Class<?> fieldType, SortMode sortMode) {
		if (
				fieldStructure.isInNested()
				&& sortMode == SortMode.MAX
				&& ( Float.class.equals( fieldType )
						|| Double.class.equals( fieldType )
						|| BigInteger.class.equals( fieldType )
						|| BigDecimal.class.equals( fieldType ) )
		) {
			// For some reason, ES 5.6 seems to evaluate the max to 0 when a nested document
			// has a field with only negative floating-point values.
			// This causes problems in our tests relative to field sorts
			// because it brings a max from -42 to 0, which changes the relative order of documents.
			// This is most likely a bug, though I couldn't find the bug report or fix,
			// and it is fixed in ES 6.x.
			// Since 5.6 is really old and EOL'd anyway, it's unlikely to ever get a fix.
			// We'll just ignore tests that fail because of this.
			return ! dialect.hasBugForSortMaxOnNegativeFloats();
		}
		else {
			return true;
		}
	}

	@Override
	public boolean supportsValuesForDynamicField(Class<?> javaType) {
		if ( BigInteger.class.equals( javaType ) ) {
			// For some reason, ES 5.6 to 7.2 fail to index BigInteger values
			// with "No matching token for number_type [BIG_INTEGER]".
			// It's fixed in Elasticsearch 7.3, though.
			return ! dialect.hasBugForBigIntegerValuesForDynamicField();
		}
		else if ( BigDecimal.class.equals( javaType ) ) {
			// For some reason, ES 5.6 and 6.x sometimes fails to index BigDecimal values
			// in dynamic fields.
			// See https://hibernate.atlassian.net/browse/HSEARCH-4310,
			// https://hibernate.atlassian.net/browse/HSEARCH-4310
			return ! dialect.hasBugForBigDecimalValuesForDynamicField();
		}
		else {
			return true;
		}
	}

	@Override
	public boolean supportsTotalHitsThresholdForSearch() {
		return dialect.supportsSkipOrLimitingTotalHitCount();
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
			// See https://github.com/elastic/elasticsearch/issues/65306
			return !dialect.hasBugForExistsOnNullGeoPointFieldWithoutDocValues();
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
	public boolean supportMoreThan1024TermsOnMatchingAny() {
		return dialect.supportMoreThan1024Terms();
	}

	@Override
	public boolean supportsDistanceSortWhenFieldMissingInSomeTargetIndexes() {
		// Not supported in older versions of Elasticsearch
		return dialect.supportsIgnoreUnmappedForGeoPointField();
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
		if ( BigInteger.class.equals( fieldType ) || BigDecimal.class.equals( fieldType ) ) {
			// We cannot use unmapped_type for scaled floats:
			// Elasticsearch complains it needs a scaling factor, but we don't have any way to provide it.
			// See https://hibernate.atlassian.net/browse/HSEARCH-4176
			return false;
		}
		else {
			return true;
		}
	}

	@Override
	public boolean supportsFieldSortWhenNestedFieldMissingInSomeTargetIndexes() {
		// Not supported in older versions of Elasticsearch
		return dialect.ignoresFieldSortWhenNestedFieldMissing();
	}

	@Override
	public boolean reliesOnNestedDocumentsForObjectProjection() {
		return false;
	}
}
