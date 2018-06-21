/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

public class LuceneSearchPredicateContext {

	private static final LuceneSearchPredicateContext ROOT = new LuceneSearchPredicateContext( null );

	private final String nestedPath;

	public LuceneSearchPredicateContext(String nestedPath) {
		this.nestedPath = nestedPath;
	}

	public String getNestedPath() {
		return nestedPath;
	}

	public static LuceneSearchPredicateContext root() {
		return ROOT;
	}
}
