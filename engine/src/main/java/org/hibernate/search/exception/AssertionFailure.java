/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.exception;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Indicates failure of an assertion: a possible bug in Hibernate Search.
 *
 * @author Gavin King
 * @auhor Emmanuel Bernard
 */
public class AssertionFailure extends RuntimeException {

	private static final Log log = LoggerFactory.make();

	public AssertionFailure(String s) {
		super( s );
		log.assertionFailure( this );
	}

	public AssertionFailure(String s, Throwable t) {
		super( s, t );
		log.assertionFailure( this );
	}

}
