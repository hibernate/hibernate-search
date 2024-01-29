/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

public class PredicateNestingContext {
	private static final PredicateNestingContext EMPTY = new PredicateNestingContext();
	private final String nestedPath;

	public static PredicateNestingContext simple() {
		return EMPTY;
	}

	public static PredicateNestingContext nested(String nestedPath) {
		return new PredicateNestingContext( nestedPath );
	}

	private PredicateNestingContext(String nestedPath) {
		this.nestedPath = nestedPath;
	}

	private PredicateNestingContext() {
		this( null );
	}

	public String getNestedPath() {
		return nestedPath;
	}
}
