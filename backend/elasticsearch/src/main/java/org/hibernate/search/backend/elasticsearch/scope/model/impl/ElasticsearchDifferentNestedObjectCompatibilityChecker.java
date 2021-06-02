/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.scope.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchDifferentNestedObjectCompatibilityChecker {

	public static ElasticsearchDifferentNestedObjectCompatibilityChecker empty(
			ElasticsearchSearchIndexScope scope) {
		return new ElasticsearchDifferentNestedObjectCompatibilityChecker( scope, null, Collections.emptyList() );
	}

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchSearchIndexScope scope;
	private final String fieldPath;
	private final List<String> nestedPathHierarchy;

	private ElasticsearchDifferentNestedObjectCompatibilityChecker(ElasticsearchSearchIndexScope scope,
			String fieldPath, List<String> nestedPathHierarchy) {
		this.scope = scope;
		this.fieldPath = fieldPath;
		this.nestedPathHierarchy = nestedPathHierarchy;
	}

	public ElasticsearchDifferentNestedObjectCompatibilityChecker combineAndCheck(String incomingFieldPath) {
		List<String> incomingNestedPathHierarchy = scope.field( incomingFieldPath ).nestedPathHierarchy();
		if ( fieldPath == null ) {
			return new ElasticsearchDifferentNestedObjectCompatibilityChecker( scope, incomingFieldPath,
					incomingNestedPathHierarchy );
		}

		if ( !nestedPathHierarchy.equals( incomingNestedPathHierarchy ) ) {
			throw log.simpleQueryStringSpanningMultipleNestedPaths( fieldPath, getLastPath( nestedPathHierarchy ),
					incomingFieldPath, getLastPath( incomingNestedPathHierarchy ) );
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
