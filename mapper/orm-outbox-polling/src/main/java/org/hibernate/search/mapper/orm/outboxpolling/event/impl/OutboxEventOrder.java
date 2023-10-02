/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.OutboxEventProcessingOrder;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.UuidGenerationStrategy;
import org.hibernate.search.util.common.AssertionFailure;

public enum OutboxEventOrder {

	NONE {
		@Override
		String queryPart(String eventAlias) {
			return "";
		}
	},
	TIME {
		@Override
		String queryPart(String eventAlias) {
			return " order by " + eventAlias + ".processAfter";
		}
	},
	ID {
		@Override
		String queryPart(String eventAlias) {
			return " order by " + eventAlias + ".id";
		}
	};

	abstract String queryPart(String eventAlias);

	public static OutboxEventOrder of(OutboxEventProcessingOrder order, UuidGenerationStrategy uuidGenerationStrategy,
			Dialect dialect) {
		switch ( order ) {
			case NONE:
				return NONE;
			case TIME:
				return TIME;
			case ID:
				return ID;
			case AUTO:
				if ( UuidGenerationStrategy.TIME.equals( uuidGenerationStrategy ) ) {
					return ID;
				}
				else if ( dialect instanceof SQLServerDialect ) {
					return NONE;
				}
				else {
					return TIME;
				}
			default:
				throw new AssertionFailure( "Unknown order: " + order );
		}
	}

}
