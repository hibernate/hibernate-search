/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.rule.log4j;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.hamcrest.Description;

public class LogChecker {

	private final LogExpectation expectation;
	private volatile int count = 0;
	private List<LogEvent> matchingEvents;
	private List<LogEvent> extraEvents;

	LogChecker(LogExpectation expectation) {
		this.expectation = expectation;
	}

	public void appendFailure(Description description, String newline) {
		description.appendText( newline );
		if ( count < expectation.getMinExpectedCount() ) {
			description.appendText( "Expected at least " + expectation.getMinExpectedCount() + " time(s) " );
			expectation.getMatcher().describeTo( description );
			description.appendText( " but only got " + count + " such event(s)." );
			description.appendText( newline );
			description.appendText( "Matching events: " );
			appendEvents( description, newline, matchingEvents );
		}
		if ( expectation.getMaxExpectedCount() != null && expectation.getMaxExpectedCount() < count ) {
			description.appendText( "Expected at most " + expectation.getMaxExpectedCount() + " time(s) " );
			expectation.getMatcher().describeTo( description );
			description.appendText( " but got " + count + " such event(s)." );
			description.appendText( newline );
			description.appendText( "Extra events: " );
			appendEvents( description, newline, extraEvents );
			description.appendText( newline );
			description.appendText( "Matching events: " );
			appendEvents( description, newline, matchingEvents );
		}
	}

	void process(LogEvent event) {
		if ( expectation.getMaxExpectedCount() == null && expectation.getMinExpectedCount() <= count ) {
			// We don't care about events anymore, expectations are met and it won't change
			return;
		}
		if ( expectation.getMatcher().matches( event ) ) {
			processMatching( event );
		}
	}

	// This must be synchronized to avoid problems when multiple threads issue log events concurrently
	private synchronized void processMatching(LogEvent event) {
		++count;
		if ( expectation.getMaxExpectedCount() != null && count > expectation.getMaxExpectedCount() ) {
			if ( extraEvents == null ) {
				extraEvents = new ArrayList<>();
			}
			extraEvents.add( event.toImmutable() );
		}
		else {
			if ( matchingEvents == null ) {
				matchingEvents = new ArrayList<>();
			}
			matchingEvents.add( event.toImmutable() );
		}
	}

	boolean areExpectationsMet() {
		return expectation.getMinExpectedCount() <= count
				&& ( expectation.getMaxExpectedCount() == null || count <= expectation.getMaxExpectedCount() );
	}

	private static void appendEvents(Description description, String newline, List<LogEvent> events) {
		if ( events == null || events.isEmpty() ) {
			description.appendText( "<none>" );
			return;
		}
		for ( LogEvent event : events ) {
			description.appendText( newline );
			description.appendText( "\t - " );
			description.appendText( event.getMessage().getFormattedMessage() );
		}
	}
}
