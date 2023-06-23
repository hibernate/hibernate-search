/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common;


public class AssertionFailure extends RuntimeException {

	public AssertionFailure(String message, Throwable cause) {
		super( appendIssueReportRequest( message ), cause );
	}

	public AssertionFailure(String message) {
		super( appendIssueReportRequest( message ) );
	}

	private static String appendIssueReportRequest(String message) {
		if ( message == null || message.isEmpty() ) {
			// Shouldn't happen, but let's be safe.
			message = "Unknown failure";
		}
		message += " -- this may indicate a bug or a missing test in Hibernate Search."
				+ " Please report it: https://hibernate.org/community/";
		return message;
	}

}
