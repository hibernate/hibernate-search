/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
// tag::include[]
package org.hibernate.search.documentation.reporting.failurehandler;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.IndexFailureContext;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

public class MyFailureHandler implements FailureHandler {
	// end::include[]
	static final StaticCounters.Key INSTANCES = StaticCounters.createKey();

	public MyFailureHandler() {
		StaticCounters.get().increment( INSTANCES );
	}
	// tag::include[]

	@Override
	public void handle(FailureContext context) { // <1>
		try {
			String failingOperationDescription = context.getFailingOperation().toString(); // <2>
			Throwable throwable = context.getThrowable(); // <3>

			// ... report the failure ... // <4>
		}
		catch (Throwable t) {
			// ... handle the failure and do *not* propagate the throwable ... // <5>
		}
	}

	@Override
	public void handle(IndexFailureContext context) { // <6>
		try {
			String failingOperationDescription = context.getFailingOperation().toString();
			Throwable throwable = context.getThrowable();
			List<String> uncommittedOperationsDescriptions = new ArrayList<>();
			for ( Object uncommittedOperation : context.getUncommittedOperations() ) { // <7>
				uncommittedOperationsDescriptions.add( uncommittedOperation.toString() );
			}

			// ... report the failure ... // <8>
		}
		catch (Throwable t) {
			// ... handle the failure and do *not* propagate the throwable ... // <9>
		}
	}
}
// end::include[]
