/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import org.hibernate.search.engine.backend.types.ObjectStructure;
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

}
