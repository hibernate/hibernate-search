/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.engine;

import org.hibernate.search.exception.SearchException;

/**
 * Represent a timeout during a Fulltext search in the HSQuery.
 * The object source integration should catch this and throw a
 * relevant exception for the object source. For example in Hibernate Core, an
 * {@code org.hibernate.QueryTimeoutException}.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 * @author Emmanuel Bernard
 */
public class QueryTimeoutException extends SearchException {

	private QueryTimeoutException(String message, String queryDescription) {
		super( message + " \"" + queryDescription + '\"' );
	}

}
