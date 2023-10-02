/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.util.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.hamcrest.Matcher;

public class TestAppender extends AbstractAppender {

	private final ExpectedLog4jLog owner;
	private final Set<Matcher<?>> expectationsMet = new HashSet<>();
	private final Set<LogEvent> unexpectedEvents = new HashSet<>();

	public TestAppender(String name, ExpectedLog4jLog owner) {
		super( name, null, null, true, Property.EMPTY_ARRAY );
		this.owner = owner;
	}

	@Override
	public void append(LogEvent event) {
		for ( Matcher<?> expectation : owner.expectations ) {
			if ( !expectationsMet.contains( expectation ) && expectation.matches( event ) ) {
				expectationsMet.add( expectation );
			}
		}
		for ( Matcher<?> absenceExpectation : owner.absenceExpectations ) {
			if ( absenceExpectation.matches( event ) ) {
				unexpectedEvents.add( event );
			}
		}
	}

	public Set<Matcher<?>> getExpectationsNotMet() {
		Set<Matcher<?>> expectationsNotMet = new HashSet<>();
		expectationsNotMet.addAll( owner.expectations );
		expectationsNotMet.removeAll( expectationsMet );
		return expectationsNotMet;
	}

	public Set<LogEvent> getUnexpectedEvents() {
		return unexpectedEvents;
	}

}
