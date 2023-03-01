/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;

class LuceneTckBackendFeatures extends TckBackendFeatures {

	@Override
	public boolean nonDefaultOrderInTermsAggregations() {
		// TODO HSEARCH-3666 Lucene terms aggregations (discrete facets) may return wrong results for any sort other than the default one
		return false;
	}

	@Override
	public boolean projectionPreservesNulls() {
		return false;
	}

	@Override
	public boolean fieldsProjectableByDefault() {
		return false;
	}

	@Override
	public boolean projectionPreservesEmptySingleValuedObject(ObjectStructure structure) {
		// For single-valued, flattened object fields,
		// we cannot distinguish between an empty object (non-null object, but no subfield carries a value)
		// and an empty object.
		return ObjectStructure.NESTED.equals( structure );
	}

	@Override
	public boolean reliesOnNestedDocumentsForMultiValuedObjectProjection() {
		return true;
	}

	@Override
	public boolean supportsHighlighterUnifiedTypeNoMatchSize() {
		// Lucene default unified highlighter does not support no-match-size setting.
		// While in ES a custom highlighter is used that allows for such option.
		return false;
	}

	@Override
	public boolean supportsHighlighterUnifiedTypeMultipleFragmentsAsSeparateItems() {
		// Lucene default unified highlighter will combine all fragments into one string separating them with `ellipses` parameter.
		// While in ES a custom highlighter is used that adds each fragment as a separate item to the result list.
		// See https://hibernate.atlassian.net/browse/HSEARCH-4828
		return false;
	}

	@Override
	public boolean supportsHighlighterUnifiedTypeFragmentSize() {
		// Break iterators from `java.text.BreakIterator` do not allow for such config.
		// While in ES a custom iterator is available that wraps sentence and word break iterators and is using the max size option.
		return false;
	}

	@Override
	public boolean supportsHighlighterUnifiedTypeMaxAnalyzedOffsetOnFieldsWithTermVector() {
		// In default unified highlighter if term vectors are set the analyzer is not used when highlighting.
		// This leads to wrapped analyzer with this setting being not used.
		// While in ES a custom field highlighter is used that supports this setting.
		return false;
	}
}
