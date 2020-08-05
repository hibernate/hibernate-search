/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.route.impl;

import java.util.Objects;

import org.hibernate.search.mapper.pojo.route.DocumentRoute;

public class DocumentRouteImpl implements DocumentRoute {
	private String routingKey;

	@Override
	public void routingKey(String routingKey) {
		this.routingKey = routingKey;
	}

	public String routingKey() {
		return routingKey;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		DocumentRouteImpl that = (DocumentRouteImpl) o;
		return Objects.equals( routingKey, that.routingKey );
	}

	@Override
	public int hashCode() {
		return Objects.hash( routingKey );
	}
}
