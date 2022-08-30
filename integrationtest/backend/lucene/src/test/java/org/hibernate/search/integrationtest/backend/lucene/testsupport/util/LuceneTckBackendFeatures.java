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
}
