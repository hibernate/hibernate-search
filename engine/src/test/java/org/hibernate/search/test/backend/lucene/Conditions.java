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

	public static void assertConditionMet(Condition condition) throws InterruptedException {
		int maxLoops = 500;
		int loop = 0;
		int sleep = 100;
		while ( ! condition.evaluate() ) {
			Thread.sleep( sleep );
			if ( ++ loop > maxLoops ) {
				throw new AssertionFailure( "Condition not met because of a timeout" );
			}
		}
	}

}

