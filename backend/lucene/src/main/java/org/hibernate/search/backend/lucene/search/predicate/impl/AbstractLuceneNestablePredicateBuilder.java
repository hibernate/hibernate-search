/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;

abstract class AbstractLuceneNestablePredicateBuilder extends AbstractLuceneSearchPredicateBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		List<String> nestedPathHierarchy = getNestedPathHierarchy();

		if ( expectedParentNestedPath != null && !nestedPathHierarchy.contains( expectedParentNestedPath ) ) {
			throw log.invalidNestedObjectPathForPredicate(
					expectedParentNestedPath,
					getFieldPathsForErrorMessage()
			);
		}
	}

	@Override
	public final Query build(LuceneSearchPredicateContext context) {
		checkNestableWithin( context.getNestedPath() );

		List<String> nestedPathHierarchy = getNestedPathHierarchy();
		// traversing the nestedPathHierarchy in reversed order
		int hierarchyLastIndex = nestedPathHierarchy.size() - 1;

		String expectedNestedPath = hierarchyLastIndex < 0 ? null
				: nestedPathHierarchy.get( hierarchyLastIndex );

		if ( Objects.equals( context.getNestedPath(), expectedNestedPath ) ) {
			// Implicit nesting is not necessary
			return super.build( context );
		}

		if ( this instanceof LuceneNestedPredicateBuilder ) {
			hierarchyLastIndex--;
			expectedNestedPath = hierarchyLastIndex < 0 ? null
				: nestedPathHierarchy.get( hierarchyLastIndex );
		}

		// The context we expect this predicate to be built in.
		// We'll make sure to wrap it in nested predicates as appropriate in the next few lines,
		// so that the Query is actually executed in this context.
		LuceneSearchPredicateContext contextAfterImplicitNesting =
				new LuceneSearchPredicateContext( expectedNestedPath );

		Query result = super.build( contextAfterImplicitNesting );

		for ( int i = hierarchyLastIndex; i >= 0; i-- ) {
			String path = nestedPathHierarchy.get( i );
			if ( path.equals( context.getNestedPath() ) ) {
				// the upper levels have been handled by the explicit predicate/s
				break;
			}

			String parentNestedDocumentPath = ( i == 0 ) ? null // The parent document is the root document
					: nestedPathHierarchy.get( i - 1 ); // The parent document is a nested document one level higher
			result = LuceneNestedPredicateBuilder.doBuild( parentNestedDocumentPath, path, result );
		}

		return result;
	}

	protected abstract List<String> getNestedPathHierarchy();

	protected abstract List<String> getFieldPathsForErrorMessage();
}
