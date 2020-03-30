/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.scope.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneDifferentNestedObjectCompatibilityChecker {

	public static LuceneDifferentNestedObjectCompatibilityChecker empty(LuceneScopeModel scopeModel) {
		return new LuceneDifferentNestedObjectCompatibilityChecker( scopeModel, null, null );
	}

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneScopeModel scopeModel;
	private final String fieldPath;
	private final List<String> nestedPathHierarchy;

	private LuceneDifferentNestedObjectCompatibilityChecker(LuceneScopeModel scopeModel, String fieldPath, List<String> nestedPathHierarchy) {
		this.scopeModel = scopeModel;
		this.fieldPath = fieldPath;
		this.nestedPathHierarchy = nestedPathHierarchy;
	}

	public LuceneDifferentNestedObjectCompatibilityChecker combineAndCheck(String incomingFieldPath) {
		List<String> incomingNestedPathHierarchy = scopeModel.getNestedPathHierarchyForField( incomingFieldPath );
		if ( fieldPath == null ) {
			return new LuceneDifferentNestedObjectCompatibilityChecker( scopeModel, incomingFieldPath, incomingNestedPathHierarchy );
		}

		if ( !nestedPathHierarchy.equals( incomingNestedPathHierarchy ) ) {
			throw log.simpleQueryStringSpanningMultipleNestedPaths( fieldPath, getLastPath( nestedPathHierarchy ), incomingFieldPath, getLastPath( incomingNestedPathHierarchy ) );
		}
		return this;
	}

	public List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	public boolean isEmpty() {
		return nestedPathHierarchy == null || nestedPathHierarchy.isEmpty();
	}

	private static String getLastPath(List<String> hierarchy) {
		if ( hierarchy.isEmpty() ) {
			return "<<root>>";
		}
		return hierarchy.get( hierarchy.size() - 1 );
	}
}
