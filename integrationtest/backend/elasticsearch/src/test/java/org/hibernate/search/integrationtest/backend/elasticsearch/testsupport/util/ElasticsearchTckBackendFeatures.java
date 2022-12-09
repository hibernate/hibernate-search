/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchVersionUtils.isBetween;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchVersionUtils.isMatching;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchVersionUtils.isLessThan;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchVersionUtils.isAtMost;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchVersionUtils.isOpenSearch;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchTestHostConnectionConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;

class ElasticsearchTckBackendFeatures extends TckBackendFeatures {

	private final ElasticsearchVersion actualVersion;

	ElasticsearchTckBackendFeatures() {
		this.actualVersion = ElasticsearchTestDialect.getActualVersion();
	}

	@Override
	public boolean geoPointIndexNullAs() {
		return !isLessThan( actualVersion, "elastic:6.3.0" );
	}

	@Override
	public boolean worksFineWithStrictAboveRangedQueriesOnDecimalScaledField() {
		return !isLessThan( actualVersion, "elastic:6.0.0" );
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
		return isBetween( actualVersion, "elastic:7.7", "elastic:7.12.1" ) ||
				isOpenSearch( actualVersion );
	}

	@Override
	public boolean normalizesStringArgumentToRangePredicateForAnalyzedStringField() {
		// TODO HSEARCH-3959 Elasticsearch does not normalizes arguments passed to the range predicate
		//   for text fields.
		return false;
	}

	@Override
	public boolean zonedDateTimeDocValueHasUTCZoneId() {
		return isAtMost( actualVersion, "elastic:6.8" );
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
			return !isLessThan( actualVersion, "elastic:6.0.0" );
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
			return !isLessThan( actualVersion, "elastic:7.3.0" );
		}
		else if ( BigDecimal.class.equals( javaType ) ) {
			// For some reason, ES 5.6 and 6.x sometimes fails to index BigDecimal values
			// in dynamic fields.
			// See https://hibernate.atlassian.net/browse/HSEARCH-4310
			return !isAtMost( actualVersion, "elastic:6.8" );
		}
		else {
			return true;
		}
	}

	@Override
	public boolean fieldsProjectableByDefault() {
		return true;
	}

	@Override
	public boolean supportsTotalHitsThresholdForSearch() {
		return !isAtMost( actualVersion, "elastic:6.8" );
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
			return isAtMost( actualVersion, "elastic:7.9" );
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
		return !isLessThan( actualVersion, "elastic:6.0.0" );
	}

	@Override
	public boolean supportsDistanceSortWhenFieldMissingInSomeTargetIndexes() {
		// Not supported in older versions of Elasticsearch
		//
		// Support for ignore_unmapped in geo_distance sorts added in 6.4:
		// https://github.com/elastic/elasticsearch/pull/31153
		// In 6.3 and below, we just can't ignore unmapped fields,
		// which means sorts will fail when the geo_point field is not present in all indexes.
		return !isLessThan( actualVersion, "elastic:6.4.0" );
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
	public boolean supportsFieldSortWhenNestedFieldMissingInSomeTargetIndexes() {
		// Not supported in older versions of Elasticsearch
		//
		// Support for ignoring field sorts when a nested field is missing was added in 6.8.1/7.1.2:
		// https://github.com/elastic/elasticsearch/pull/42451
		// In 6.8.0 and below, we just can't ignore unmapped nested fields in field sorts,
		// which means sorts will fail when the nested field is not present in all indexes.
		// -----------------------------------------------------------------------------------------
		// AWS apparently didn't apply this patch, which solves the problem in 6.8.1/7.1.2,
		// to their 6.8 branch:
		// https://github.com/elastic/elasticsearch/pull/42451

		return !isAtMost( actualVersion, "elastic:6.7" ) &&
				( !ElasticsearchTestHostConnectionConfiguration.get().isAws() ||
						!isMatching( actualVersion, "elastic:6.8" ) );
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
		return !isBetween( actualVersion, "elastic:8.4.2", "elastic:8.5.0" );
	}

	@Override
	public boolean supportsMatchOnScaledNumericLossOfPrecision() {
		// https://github.com/elastic/elasticsearch/issues/91246
		// Hopefully this will get fixed in 8.5.4.
		return !isBetween( actualVersion, "elastic:8.5.0", "elastic:8.5.3" );
	}
}
