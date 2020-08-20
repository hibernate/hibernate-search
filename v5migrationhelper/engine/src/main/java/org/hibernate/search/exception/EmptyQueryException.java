/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.exception;

/**
 * This Exception is thrown when an empty TermQuery (keyword query) is created,
 * or if any string query only returns whitespace after applying Analyzers.
 *
 * Applications should validate user input before running such a Query;
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 * @see org.hibernate.search.util.AnalyzerUtils
 */
public class EmptyQueryException extends SearchException {

	public EmptyQueryException() {
		super();
	}

	public EmptyQueryException(String message) {
		super( message );
	}

	public EmptyQueryException(String message, Throwable cause) {
		super( message, cause );
	}

	public EmptyQueryException(Throwable cause) {
		super( cause );
	}

}
