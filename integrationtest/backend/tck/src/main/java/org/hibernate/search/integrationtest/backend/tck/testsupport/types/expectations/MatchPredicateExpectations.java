/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations;

public class MatchPredicateExpectations<F> {

	private final F document1Value;
	private final F document2Value;
	private final F matchingDocument1Value;

	public MatchPredicateExpectations(F document1Value, F document2Value) {
		this( document1Value, document2Value, document1Value );
	}

	public MatchPredicateExpectations(F document1Value, F document2Value, F matchingDocument1Value) {
		this.document1Value = document1Value;
		this.document2Value = document2Value;
		this.matchingDocument1Value = matchingDocument1Value;
	}

	public boolean isMatchPredicateSupported() {
		return true;
	}

	public final F getDocument1Value() {
		return document1Value;
	}

	public final F getDocument2Value() {
		return document2Value;
	}

	public final F getMatchingDocument1Value() {
		return matchingDocument1Value;
	}

}
