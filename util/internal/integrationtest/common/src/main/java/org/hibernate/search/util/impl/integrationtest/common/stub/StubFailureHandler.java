/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub;

import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.IndexFailureContext;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

public class StubFailureHandler implements FailureHandler {

	public static StaticCounters.Key CREATE = StaticCounters.createKey();
	public static StaticCounters.Key HANDLE_GENERIC_CONTEXT = StaticCounters.createKey();
	public static StaticCounters.Key HANDLE_ENTITY_INDEXING_CONTEXT = StaticCounters.createKey();
	public static StaticCounters.Key HANDLE_INDEX_CONTEXT = StaticCounters.createKey();

	public StubFailureHandler() {
		StaticCounters.get().increment( CREATE );
	}

	@Override
	public void handle(FailureContext context) {
		StaticCounters.get().increment( HANDLE_GENERIC_CONTEXT );
	}

	@Override
	public void handle(EntityIndexingFailureContext context) {
		StaticCounters.get().increment( HANDLE_ENTITY_INDEXING_CONTEXT );
	}

	@Override
	public void handle(IndexFailureContext context) {
		StaticCounters.get().increment( HANDLE_INDEX_CONTEXT );
	}

}
