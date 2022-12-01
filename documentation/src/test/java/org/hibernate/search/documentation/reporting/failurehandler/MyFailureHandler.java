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

import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

public class MyFailureHandler implements FailureHandler {
	// end::include[]
	static final StaticCounters.Key INSTANCES = StaticCounters.createKey();

	public MyFailureHandler() {
		StaticCounters.get().increment( INSTANCES );
	}
	// tag::include[]

	@Override
	public void handle(FailureContext context) { // <1>
		String failingOperationDescription = context.failingOperation().toString(); // <2>
		Throwable throwable = context.throwable(); // <3>

		// ... report the failure ... // <4>
	}

	@Override
	public void handle(EntityIndexingFailureContext context) { // <5>
		String failingOperationDescription = context.failingOperation().toString();
		Throwable throwable = context.throwable();
		List<String> entityReferencesAsStrings = new ArrayList<>();
		for ( Object entityReference : context.entityReferences() ) { // <6>
			entityReferencesAsStrings.add( entityReference.toString() );
		}

		// ... report the failure ... // <7>
	}

}
// end::include[]
