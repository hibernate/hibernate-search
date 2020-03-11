/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.apache.lucene.search.Query;

public abstract class AbstractLuceneSearchNestedPredicateBuilder extends AbstractLuceneSearchPredicateBuilder {

	private final String nestedDocumentPath;

	public AbstractLuceneSearchNestedPredicateBuilder(String nestedDocumentPath) {
		this.nestedDocumentPath = nestedDocumentPath;
	}

	@Override
	public final Query build(LuceneSearchPredicateContext context) {
		if ( nestedDocumentPath == null || nestedDocumentPath.equals( context.getNestedPath() ) ) {
			return super.build( context );
		}

		LuceneSearchPredicateContext childContext = new LuceneSearchPredicateContext( nestedDocumentPath );
		return LuceneNestedPredicateBuilder.doBuild( context, nestedDocumentPath, super.build( childContext ) );
	}
}
