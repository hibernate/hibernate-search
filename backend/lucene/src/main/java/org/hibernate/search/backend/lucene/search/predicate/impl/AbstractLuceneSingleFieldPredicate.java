/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;

public abstract class AbstractLuceneSingleFieldPredicate extends AbstractLuceneNestablePredicate {

	protected final String absoluteFieldPath;
	private final List<String> nestedPathHierarchy;

	protected AbstractLuceneSingleFieldPredicate(AbstractBuilder builder) {
		super( builder );
		this.absoluteFieldPath = builder.absoluteFieldPath;
		this.nestedPathHierarchy = builder.nestedPathHierarchy;
	}

	@Override
	protected List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	@Override
	protected List<String> getFieldPathsForErrorMessage() {
		return Collections.singletonList( absoluteFieldPath );
	}

	public abstract static class AbstractBuilder extends AbstractLuceneSearchPredicate.AbstractBuilder {
		protected final String absoluteFieldPath;
		private final List<String> nestedPathHierarchy;

		protected AbstractBuilder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexNodeContext node) {
			this( scope, node.absolutePath(), node.nestedPathHierarchy() );
		}

		protected AbstractBuilder(LuceneSearchIndexScope<?> scope, String absoluteFieldPath,
				List<String> nestedPathHierarchy) {
			super( scope );
			this.absoluteFieldPath = absoluteFieldPath;
			this.nestedPathHierarchy = nestedPathHierarchy;
		}
	}
}
