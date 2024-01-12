/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

public class PredicateNestingContext {
	private static final PredicateNestingContext ACCEPTS_KNN = new PredicateNestingContext( true );
	private static final PredicateNestingContext DOES_NOT_ACCEPT_KNN = new PredicateNestingContext( false );
	private final String nestedPath;
	private final boolean acceptsKnnClause;

	private final Class<? extends ElasticsearchSearchPredicate> predicateType;

	public static PredicateNestingContext acceptsKnn() {
		return ACCEPTS_KNN;
	}

	public static PredicateNestingContext doesNotAcceptKnn() {
		return DOES_NOT_ACCEPT_KNN;
	}

	public static PredicateNestingContext nested(String nestedPath) {
		return new PredicateNestingContext( nestedPath );
	}

	private PredicateNestingContext(String nestedPath, boolean acceptsKnnClause) {
		this( nestedPath, acceptsKnnClause, null );
	}

	private PredicateNestingContext(String nestedPath, boolean acceptsKnnClause,
			Class<? extends ElasticsearchSearchPredicate> predicateType) {
		this.nestedPath = nestedPath;
		this.acceptsKnnClause = acceptsKnnClause;
		this.predicateType = predicateType;
	}

	private PredicateNestingContext(String nestedPath) {
		this( nestedPath, false );
	}

	private PredicateNestingContext(boolean acceptsKnnClause) {
		this( null, acceptsKnnClause );
	}

	public String getNestedPath() {
		return nestedPath;
	}

	public boolean acceptsKnnClause() {
		return acceptsKnnClause;
	}

	public PredicateNestingContext wrap(ElasticsearchSearchPredicate elasticsearchSearchPredicate) {
		return new PredicateNestingContext(
				nestedPath,
				acceptsKnnClause && this.predicateType == null,
				elasticsearchSearchPredicate.getClass()
		);
	}
}
