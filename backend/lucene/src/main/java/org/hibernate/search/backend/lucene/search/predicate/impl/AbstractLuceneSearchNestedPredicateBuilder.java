/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.List;

import org.apache.lucene.search.Query;

public abstract class AbstractLuceneSearchNestedPredicateBuilder extends AbstractLuceneSearchPredicateBuilder {

	private final List<String> nestedPathHierarchy;

	public AbstractLuceneSearchNestedPredicateBuilder(List<String> nestedPathHierarchy) {
		this.nestedPathHierarchy = nestedPathHierarchy;
	}

	@Override
	public final Query build(LuceneSearchPredicateContext context) {
		return applyImplicitNestedSteps( nestedPathHierarchy, context, super.build( context ) );
	}
}
