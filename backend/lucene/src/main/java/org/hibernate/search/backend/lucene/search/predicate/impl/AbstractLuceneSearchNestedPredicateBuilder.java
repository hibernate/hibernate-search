/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.LinkedList;
import java.util.List;

import org.hibernate.search.util.common.AssertionFailure;

import org.apache.lucene.search.Query;

public abstract class AbstractLuceneSearchNestedPredicateBuilder extends AbstractLuceneSearchPredicateBuilder {

	private final List<String> nestedPathHierarchy;

	public AbstractLuceneSearchNestedPredicateBuilder(List<String> nestedPathHierarchy) {
		this.nestedPathHierarchy = nestedPathHierarchy;
	}

	@Override
	public final Query build(LuceneSearchPredicateContext context) {
		List<String> nestedSteps = implicitNestedSteps( context, nestedPathHierarchy );
		return applyImplicitNestedSteps( nestedSteps, context, super::build );
	}

	public static List<String> implicitNestedSteps(LuceneSearchPredicateContext context, List<String> nestedPathHierarchy) {
		String contextNestedPath = context.getNestedPath();

		if ( contextNestedPath == null ) {
			// we need to handle all the nestedPathHierarchy belong to the target
			return new LinkedList<>( nestedPathHierarchy );
		}

		int contextNestedPathIndex = nestedPathHierarchy.indexOf( contextNestedPath );
		if ( contextNestedPathIndex < 0 ) {
			throw new AssertionFailure( "The nested path must belong to the nested path hierarchy: there's an Hibernate Search bug." );
		}

		// we need to handle just the last part of the nestedPathHierarchy belong to the target,
		// the one that hasn't been handled by the context
		return new LinkedList<>( nestedPathHierarchy.subList( contextNestedPathIndex + 1, nestedPathHierarchy.size() ) );
	}
}
