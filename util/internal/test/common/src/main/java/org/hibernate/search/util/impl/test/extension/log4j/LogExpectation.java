/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.extension.log4j;

import org.hamcrest.Matcher;

public class LogExpectation {

	private final Matcher<?> matcher;
	private Integer expectedCount;

	public LogExpectation(Matcher<?> matcher) {
		this.matcher = matcher;
	}

	public void never() {
		times( 0 );
	}

	public void once() {
		times( 1 );
	}

	public void times(int expectedCount) {
		if ( this.expectedCount != null ) {
			throw new IllegalStateException( "Can only set log expectations once" );
		}
		this.expectedCount = expectedCount;
	}

	public LogChecker createChecker() {
		return new LogChecker( this );
	}

	Matcher<?> getMatcher() {
		return matcher;
	}

	int getMinExpectedCount() {
		return expectedCount == null ? 1 : expectedCount;
	}

	Integer getMaxExpectedCount() {
		return expectedCount;
	}
}
