/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.impl.OutboxPollingCoordinationStrategy;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.mapping.OutboxPollingSearchMapping;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.mapping.SearchMappingExtension;
import org.hibernate.search.mapper.orm.mapping.spi.CoordinationStrategyContext;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * An extension to the search mapping, giving access to features specific to the outbox polling coordination strategy.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called directly by users.
 * In short, users are only expected to get instances of this type from an API and pass it to another API.
 *
 * @see #get()
 */
@Incubating
public class OutboxPollingExtension implements SearchMappingExtension<OutboxPollingSearchMapping> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final OutboxPollingExtension instance = new OutboxPollingExtension();

	@Override
	public OutboxPollingSearchMapping extendOrFail(SearchMapping original) {
		if ( !( original instanceof CoordinationStrategyContext ) ) {
			throw log.outboxPollingExtensionOnUnknownType( original );
		}

		CoordinationStrategy coordinationStrategy = ( (CoordinationStrategyContext) original ).coordinationStrategy();
		if ( coordinationStrategy instanceof OutboxPollingCoordinationStrategy ) {
			return ( (OutboxPollingCoordinationStrategy) coordinationStrategy ).outboxPollingSearchMapping();
		}

		throw log.outboxPollingExtensionOnUnknownType( original );
	}

	public static OutboxPollingExtension get() {
		return instance;
	}
}
