/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.rule.log4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

public class TestAppender extends AbstractAppender {

	private final List<LogChecker> checkers = new ArrayList<>();

	public TestAppender(String name) {
		super( name, null, null, true, Property.EMPTY_ARRAY );
	}

	@Override
	public void append(LogEvent event) {
		for ( LogChecker checker : checkers ) {
			checker.process( event );
		}
	}

	public void addChecker(LogChecker checker) {
		checkers.add( checker );
	}

	public Set<LogChecker> getFailingCheckers() {
		Set<LogChecker> failingCheckers = new HashSet<>();
		for ( LogChecker checker : checkers ) {
			if ( !checker.areExpectationsMet() ) {
				failingCheckers.add( checker );
			}
		}
		return failingCheckers;
	}
}
