/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public abstract class AbstractElasticsearchNestablePredicate extends AbstractElasticsearchPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	AbstractElasticsearchNestablePredicate(AbstractElasticsearchPredicate.AbstractBuilder builder) {
		super( builder );
	}


	@Override
	public void checkNestableWithin(PredicateNestingContext context) {
		List<String> nestedPathHierarchy = getNestedPathHierarchy();
		String expectedParentNestedPath = context.getNestedPath();

		if ( expectedParentNestedPath != null && !nestedPathHierarchy.contains( expectedParentNestedPath ) ) {
			throw log.invalidNestedObjectPathForPredicate( this, expectedParentNestedPath,
					getFieldPathsForErrorMessage() );
		}
	}

	@Override
	public JsonObject toJsonQuery(PredicateRequestContext context) {
		List<String> nestedPathHierarchy = getNestedPathHierarchy();
		String expectedNestedPath = nestedPathHierarchy.isEmpty()
				? null
				: nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 );

		if ( Objects.equals( context.getNestedPath(), expectedNestedPath ) ) {
			// Implicit nesting is not necessary
			return super.toJsonQuery( context );
		}

		// The context we expect this predicate to be built in.
		// We'll make sure to wrap it in nested predicates as appropriate in the next few lines,
		// so that the predicate is actually executed in this context.
		PredicateRequestContext contextAfterImplicitNesting =
				context.withNestedPath( expectedNestedPath );

		JsonObject result = super.toJsonQuery( contextAfterImplicitNesting );

		// traversing the nestedPathHierarchy in the inverted order
		int hierarchyLastIndex = nestedPathHierarchy.size() - 1;
		for ( int i = hierarchyLastIndex; i >= 0; i-- ) {
			String path = nestedPathHierarchy.get( i );
			if ( path.equals( context.getNestedPath() ) ) {
				// skip all from this point
				break;
			}

			JsonObject outerObject = new JsonObject();
			JsonObject innerObject = new JsonObject();
			ElasticsearchNestedPredicate.wrap( indexNames(), path, outerObject, innerObject, result );
			result = outerObject;
		}

		return result;
	}

	protected abstract List<String> getNestedPathHierarchy();

	protected abstract List<String> getFieldPathsForErrorMessage();
}
