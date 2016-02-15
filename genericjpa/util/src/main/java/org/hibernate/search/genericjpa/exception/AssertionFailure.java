/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.exception;

/**
 * Indicates failure of an assertion: a possible bug in Hibernate Search GenericJPA.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Martin Braun
 */
public class AssertionFailure extends RuntimeException {

	private static final long serialVersionUID = 5058748804105410416L;

	public AssertionFailure(String s) {
		super( s );
	}

	public AssertionFailure(String s, Throwable t) {
		super( s, t );
	}

}