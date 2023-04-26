/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
// tag::include[]
package org.hibernate.search.documentation.reporting.failurehandler;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
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
		String failingOperationDescription = context.failingOperation().toString(); // <2>
		Throwable throwable = context.throwable(); // <3>

		// ... report the failure ... // <4>
	}

	@Override
	public void handle(EntityIndexingFailureContext context) { // <5>
		String failingOperationDescription = context.failingOperation().toString();
		Throwable throwable = context.throwable();
		for ( EntityReference entityReference : context.failingEntityReferences() ) { // <6>
			Class<?> entityType = entityReference.type(); // <7>
			String entityName = entityReference.name(); // <7>
			Object entityId = entityReference.id(); // <7>
			String entityReferenceAsString = entityReference.toString(); // <8>

			// ... process the entity reference ... // <9>
		}
	}

}
// end::include[]
