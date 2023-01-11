/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.common.SortMode;

public abstract class TckBackendFeatures {

	public boolean geoPointIndexNullAs() {
		return true;
	}

	public boolean worksFineWithStrictAboveRangedQueriesOnDecimalScaledField() {
		return true;
	}

	public boolean normalizesStringMissingValues() {
		return true;
	}

	public boolean normalizesStringArgumentToWildcardPredicateForAnalyzedStringField() {
		return true;
	}

	public boolean normalizesStringArgumentToRangePredicateForAnalyzedStringField() {
		return true;
	}

	public boolean zonedDateTimeDocValueHasUTCZoneId() {
		return false;
	}

	public boolean nonCanonicalRangeInAggregations() {
		return true;
	}

	public boolean nonDefaultOrderInTermsAggregations() {
		return true;
	}

	public boolean sortByFieldValue(TestedFieldStructure fieldStructure, Class<?> fieldType, SortMode sortMode) {
		return true;
	}

	public boolean supportsValuesForDynamicField(Class<?> javaType) {
		return true;
	}

	public boolean projectionPreservesNulls() {
		return true;
	}

	public abstract boolean fieldsProjectableByDefault();

	public boolean supportsTotalHitsThresholdForSearch() {
		return true;
	}

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

	public boolean supportMoreThan1024TermsOnMatchingAny() {
		return true;
	}

	public boolean supportsDistanceSortWhenFieldMissingInSomeTargetIndexes() {
		return true;
	}

	public boolean supportsDistanceSortWhenNestedFieldMissingInSomeTargetIndexes() {
		return true;
	}

	public boolean supportsFieldSortWhenFieldMissingInSomeTargetIndexes(Class<?> fieldType) {
		return true;
	}

	public boolean supportsFieldSortWhenNestedFieldMissingInSomeTargetIndexes() {
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

}
