/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.mapping.SearchMappingExtension;
import org.hibernate.search.mapper.orm.mapping.spi.CoordinationStrategyContext;
import org.hibernate.search.mapper.orm.outboxpolling.impl.OutboxPollingCoordinationStrategy;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.Log;
import org.hibernate.search.mapper.orm.outboxpolling.mapping.OutboxPollingSearchMapping;
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
