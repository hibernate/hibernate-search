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
	public boolean distanceSortDesc() {
		// we don't test the descending order here as it's currently not supported by Lucene
		// see LuceneSearchSortIT
		return false;
	}

	@Override
	public boolean aggregationsOnMultiValuedFields(Class<?> fieldType) {
		// TODO HSEARCH-1929 + HSEARCH-1927 Aggregations on multi-valued numeric fields are not supported at the moment
		//  See in particular https://hibernate.atlassian.net/browse/HSEARCH-1927?focusedCommentId=88210&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-88210
		return String.class.equals( fieldType );
	}

}
