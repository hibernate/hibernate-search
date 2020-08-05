/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.List;

import org.hibernate.search.mapper.pojo.route.impl.DocumentRouteImpl;

public interface PojoWorkRouter {

	DocumentRouteImpl currentRoute(String providedRoutingKey);

	List<DocumentRouteImpl> previousRoutes(DocumentRouteImpl currentRoute);

}
