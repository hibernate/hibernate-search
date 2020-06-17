/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

public class PredicateRequestContext {

	private static final PredicateRequestContext ROOT = new PredicateRequestContext( null );

	private final String nestedPath;

	public PredicateRequestContext(String nestedPath) {
		this.nestedPath = nestedPath;
	}

	public String getNestedPath() {
		return nestedPath;
	}

	public static PredicateRequestContext root() {
		return ROOT;
	}
}
