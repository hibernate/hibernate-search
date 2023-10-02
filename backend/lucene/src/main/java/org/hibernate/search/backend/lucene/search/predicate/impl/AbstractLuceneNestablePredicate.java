/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;

abstract class AbstractLuceneNestablePredicate extends AbstractLuceneSearchPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	AbstractLuceneNestablePredicate(AbstractBuilder builder) {
		super( builder );
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		List<String> nestedPathHierarchy = getNestedPathHierarchy();

		if ( expectedParentNestedPath != null && !nestedPathHierarchy.contains( expectedParentNestedPath ) ) {
			throw log.invalidNestedObjectPathForPredicate( this, expectedParentNestedPath,
					getFieldPathsForErrorMessage() );
		}
	}

	@Override
	public final Query toQuery(PredicateRequestContext context) {
		checkNestableWithin( context.getNestedPath() );

		List<String> nestedPathHierarchy = getNestedPathHierarchy();
		String expectedNestedPath = nestedPathHierarchy.isEmpty()
				? null
				: nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 );

		if ( Objects.equals( context.getNestedPath(), expectedNestedPath ) ) {
			// Implicit nesting is not necessary
			return super.toQuery( context );
		}

		// The context we expect this predicate to be built in.
		// We'll make sure to wrap it in nested predicates as appropriate in the next few lines,
		// so that the Query is actually executed in this context.
		PredicateRequestContext contextAfterImplicitNesting =
				context.withNestedPath( expectedNestedPath );

		Query result = super.toQuery( contextAfterImplicitNesting );

		// traversing the nestedPathHierarchy in reversed order
		int hierarchyLastIndex = nestedPathHierarchy.size() - 1;
		for ( int i = hierarchyLastIndex; i >= 0; i-- ) {
			String path = nestedPathHierarchy.get( i );
			if ( path.equals( context.getNestedPath() ) ) {
				// the upper levels have been handled by the explicit predicate/s
				break;
			}

			String parentNestedDocumentPath = ( i == 0 )
					? null // The parent document is the root document
					: nestedPathHierarchy.get( i - 1 ); // The parent document is a nested document one level higher
			result = LuceneNestedPredicate.createNestedQuery( parentNestedDocumentPath, path, result );
		}

		return result;
	}

	protected abstract List<String> getNestedPathHierarchy();

	protected abstract List<String> getFieldPathsForErrorMessage();
}
