/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import java.util.Optional;

import org.hibernate.search.util.common.impl.ToStringTreeAppendable;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

public abstract class OutboxEventFinderProvider implements ToStringTreeAppendable {

	@Override
	public String toString() {
		return new ToStringTreeBuilder().value( this ).toString();
	}

	public abstract OutboxEventFinder create(Optional<OutboxEventPredicate> predicate);

}
