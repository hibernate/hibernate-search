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

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneScopeModel scopeModel;

	private String absoluteFieldPath;
	private List<String> nestedObjectPath;

	public LuceneDifferentNestedObjectCompatibilityChecker(LuceneScopeModel scopeModel) {
		this.scopeModel = scopeModel;
	}

	public void combineAndCheck(String anotherAbsoluteFieldPath) {
		if ( absoluteFieldPath == null ) {
			absoluteFieldPath = anotherAbsoluteFieldPath;
			nestedObjectPath = scopeModel.getNestedPathHierarchyForField( absoluteFieldPath );
			return;
		}

		List<String> anotherNestedObjectPath = scopeModel.getNestedPathHierarchyForField( anotherAbsoluteFieldPath );
		if ( !nestedObjectPath.equals( anotherNestedObjectPath ) ) {
			throw log.simpleQueryStringSpanningMultipleNestedPaths( absoluteFieldPath, getLastPath( nestedObjectPath ), anotherAbsoluteFieldPath,
					getLastPath( anotherNestedObjectPath )
			);
		}
	}

	public List<String> getNestedObjectPath() {
		return nestedObjectPath;
	}

	public boolean isEmpty() {
		return nestedObjectPath == null || nestedObjectPath.isEmpty();
	}

	private static String getLastPath(List<String> hierarchy) {
		if ( hierarchy.isEmpty() ) {
			return "<<root>>";
		}
		return hierarchy.get( hierarchy.size() - 1 );
	}
}
