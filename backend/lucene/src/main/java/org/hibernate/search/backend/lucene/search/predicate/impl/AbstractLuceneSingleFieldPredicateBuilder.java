/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;

public abstract class AbstractLuceneSingleFieldPredicateBuilder extends AbstractLuceneNestablePredicateBuilder {

	protected final String absoluteFieldPath;
	private final List<String> nestedPathHierarchy;

	protected AbstractLuceneSingleFieldPredicateBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<?> field) {
		this( searchContext, field.absolutePath(), field.nestedPathHierarchy() );
	}

	protected AbstractLuceneSingleFieldPredicateBuilder(LuceneSearchContext searchContext,
			String absoluteFieldPath, List<String> nestedPathHierarchy) {
		super( searchContext );
		this.absoluteFieldPath = absoluteFieldPath;
		this.nestedPathHierarchy = nestedPathHierarchy;
	}

	@Override
	protected List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	@Override
	protected List<String> getFieldPathsForErrorMessage() {
		return Collections.singletonList( absoluteFieldPath );
	}
}
