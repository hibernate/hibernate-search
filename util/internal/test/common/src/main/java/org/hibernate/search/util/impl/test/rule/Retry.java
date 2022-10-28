/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.util.impl.test.logging.Log;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

public final class Retry implements TestRule {

	/**
	 * @return A rule that will retry failing tests 3 times, without resetting any other {@link TestRule}.
	 * Use this only if you don't have any other rule in your test.
	 */
	public static Retry withoutOtherRules() {
		return new Retry( Collections.emptyList() );
	}

	/**
	 * @param otherRule1 Another rule used in the same test.
	 * @param otherRules Other rules used in the same test.
	 * @return A rule that will retry failing tests 3 times, taking care to reset the given other {@link TestRule}s
	 * between each new attempt.
	 */
	public static Retry withOtherRules(TestRule otherRule1, TestRule ... otherRules) {
		List<TestRule> otherRulesList = new ArrayList<>();
		otherRulesList.add( otherRule1 );
		Collections.addAll( otherRulesList, otherRules );
		// Start with the innermost rule
		Collections.reverse( otherRulesList );
		return new Retry( otherRulesList );
	}

	private final List<TestRule> otherRulesStartingWithInnermost;

	private Retry(List<TestRule> otherRulesStartingWithInnermost) {
		this.otherRulesStartingWithInnermost = otherRulesStartingWithInnermost;
	}

	@Override
	public Statement apply(Statement base, org.junit.runner.Description description) {
		Statement delegate = base;
		for ( TestRule otherRule : otherRulesStartingWithInnermost ) {
			delegate = otherRule.apply( delegate, description );
		}
		final Statement finalDelegate = delegate;
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				List<AssertionError> failures = new ArrayList<>();
				for ( int i = 0; i < 3; i++ ) {
					if ( i > 0 ) {
						Log.INSTANCE.info( "Retrying..." );
					}
					try {
						finalDelegate.evaluate();
						return; // Test succeeded
					}
					catch (AssumptionViolatedException e) {
						// if we caught an `AssumptionViolatedException` it means the test should be skipped and we just
						// need to rethrow an exception.
						throw e;
					}
					catch (AssertionError | Exception e) {
						String failureMessage = "Attempt #" + ( i + 1 ) + " failed: " + e.getMessage();
						Log.INSTANCE.error( failureMessage, e );

						AssertionError wrapped = new AssertionError( failureMessage, e );
						failures.add( wrapped );
					}
				}
				AssertionError failure = new AssertionError( "Test failed after 3 attempts" );
				failures.forEach( failure::addSuppressed );
				throw failure;
			}
		};
	}

}
