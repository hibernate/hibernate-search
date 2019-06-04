/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;

public class LuceneTckBackendFeatures extends TckBackendFeatures {

	@Override
	public boolean distanceSortDesc() {
		// we don't test the descending order here as it's currently not supported by Lucene
		// see LuceneSearchSortIT
		return false;
	}
}
