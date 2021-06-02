/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.scope.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneDifferentNestedObjectCompatibilityChecker {

	public static LuceneDifferentNestedObjectCompatibilityChecker empty(LuceneSearchContext searchContext) {
		return new LuceneDifferentNestedObjectCompatibilityChecker( searchContext, null, Collections.emptyList() );
	}

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchContext searchContext;
	private final String fieldPath;
	private final List<String> nestedPathHierarchy;

	private LuceneDifferentNestedObjectCompatibilityChecker(LuceneSearchContext searchContext, String fieldPath,
			List<String> nestedPathHierarchy) {
		this.searchContext = searchContext;
		this.fieldPath = fieldPath;
		this.nestedPathHierarchy = nestedPathHierarchy;
	}

	public LuceneDifferentNestedObjectCompatibilityChecker combineAndCheck(String incomingFieldPath) {
		List<String> incomingNestedPathHierarchy = searchContext.field( incomingFieldPath ).nestedPathHierarchy();
		if ( fieldPath == null ) {
			return new LuceneDifferentNestedObjectCompatibilityChecker( searchContext, incomingFieldPath, incomingNestedPathHierarchy );
		}

		if ( !nestedPathHierarchy.equals( incomingNestedPathHierarchy ) ) {
			throw log.simpleQueryStringSpanningMultipleNestedPaths( fieldPath, getLastPath( nestedPathHierarchy ), incomingFieldPath, getLastPath( incomingNestedPathHierarchy ) );
		}
		return this;
	}

	public List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	private static String getLastPath(List<String> hierarchy) {
		if ( hierarchy.isEmpty() ) {
			return "<<root>>";
		}
		return hierarchy.get( hierarchy.size() - 1 );
	}
}
