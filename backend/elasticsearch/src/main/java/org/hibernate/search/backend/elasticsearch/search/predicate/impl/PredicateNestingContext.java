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
		this.nestedPath = nestedPath;
		this.acceptsKnnClause = acceptsKnnClause;
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

	public PredicateNestingContext rejectKnn() {
		if ( !acceptsKnnClause ) {
			return this;
		}
		if ( nestedPath == null ) {
			return DOES_NOT_ACCEPT_KNN;
		}
		return new PredicateNestingContext( nestedPath, false );
	}
}
