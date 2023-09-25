/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.impl.LogFailureHandler;

public class TestFailureHandler implements FailureHandler {
	public final List<FailureContext> genericFailures = Collections.synchronizedList( new ArrayList<>() );
	public final Map<Integer, List<EntityIndexingFailureContext>> entityFailures = new ConcurrentHashMap<>();
	private final LogFailureHandler delegate = new LogFailureHandler();

	@Override
	public void handle(FailureContext context) {
		genericFailures.add( context );
		// For easier debugging
		delegate.handle( context );
	}

	@Override
	public void handle(EntityIndexingFailureContext context) {
		for ( EntityReference entityReference : context.failingEntityReferences() ) {
			Integer id = (Integer) entityReference.id();

			entityFailures.computeIfAbsent( id, key -> Collections.synchronizedList( new ArrayList<>() ) );
			entityFailures.get( id ).add( context );
		}
		// For easier debugging
		delegate.handle( context );
	}
}
