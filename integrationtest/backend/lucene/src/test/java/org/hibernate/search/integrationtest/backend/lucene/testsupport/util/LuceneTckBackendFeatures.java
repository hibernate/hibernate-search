/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

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
	public boolean reliesOnNestedDocumentsForObjectProjection() {
		return true;
	}
}
