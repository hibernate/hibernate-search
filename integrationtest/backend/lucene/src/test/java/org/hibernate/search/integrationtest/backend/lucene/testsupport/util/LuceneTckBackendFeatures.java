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
	public boolean aggregationsOnMultiValuedFields(Class<?> fieldType) {
		// TODO HSEARCH-1929 + HSEARCH-1927 Aggregations on multi-valued numeric fields are not supported at the moment
		//  See in particular https://hibernate.atlassian.net/browse/HSEARCH-1927?focusedCommentId=88210&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-88210
		return String.class.equals( fieldType );
	}

	@Override
	public boolean nonDefaultOrderInTermsAggregations() {
		// TODO HSEARCH-3666 Lucene terms aggregations (discrete facets) may return wrong results for any sort other than the default one
		return false;
	}

	@Override
	public boolean flushWillFailIfAppliedToDeletedIndex() {
		/*
		 * Lucene has optimizations in place to not apply flushes when there are no pending change in the writer.
		 * Thus, even if we ruthlessly delete the index from the filesystem,
		 * executing a flush will work most of the time,
		 * because most of the time changes are already committed when the flush executes.
		 */
		return false;
	}

	@Override
	public boolean optimizeWillFailIfAppliedToDeletedIndex() {
		/*
		 * Lucene has optimizations in place to not apply optimize() when there is only one segment.
		 * Thus, even if we ruthlessly delete the index from the filesystem,
		 * executing optimize() will work most of the time,
		 * because most of the time we will only have one segment.
		 */
		return false;
	}
}
