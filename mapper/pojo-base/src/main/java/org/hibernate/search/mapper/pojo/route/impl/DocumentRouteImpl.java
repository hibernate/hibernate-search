/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.route.impl;

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
}
