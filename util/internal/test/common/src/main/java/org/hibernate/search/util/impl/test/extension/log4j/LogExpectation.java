/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.extension.log4j;

import org.hamcrest.Matcher;

public class LogExpectation {

	private final Matcher<?> matcher;
	private Integer expectedMinCount;
	private Integer expectedMaxCount;

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
		if ( this.expectedMinCount != null || this.expectedMaxCount != null ) {
			throw new IllegalStateException( "Can only set log expectations once" );
		}
		this.expectedMinCount = expectedCount;
		this.expectedMaxCount = expectedCount;
	}

	public void atLeast(int expectedCount) {
		this.expectedMaxCount = Integer.MAX_VALUE;
		this.expectedMinCount = expectedCount;
	}

	public LogChecker createChecker() {
		return new LogChecker( this );
	}

	Matcher<?> getMatcher() {
		return matcher;
	}

	int getMinExpectedCount() {
		return expectedMinCount == null ? 1 : expectedMinCount;
	}

	Integer getMaxExpectedCount() {
		return expectedMaxCount;
	}
}
