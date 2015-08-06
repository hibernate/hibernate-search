/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend.lucene;

import org.hibernate.search.exception.AssertionFailure;

/**
 * @author gustavonalle
 */
public class Conditions {

	private Conditions() { }

	public static void assertConditionMet(final Condition condition) throws InterruptedException {
		final int maxLoops = 2500;
		final int sleep = 20;
		int loop = 0;
		while ( ! condition.evaluate() ) {
			if ( ++ loop > maxLoops ) {
				throw new AssertionFailure( "Condition not met because of a timeout" );
			}
			Thread.sleep( sleep );
		}
	}

}

