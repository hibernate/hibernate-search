/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;

class NoOpPojoTypeNodeIdentityMappingCollector implements PojoTypeNodeIdentityMappingCollector {

	static final NoOpPojoTypeNodeIdentityMappingCollector INSTANCE = new NoOpPojoTypeNodeIdentityMappingCollector();

	@Override
	public void identifierBridge(PropertyHandle handle, IdentifierBridge<?> bridge) {
		// No-op
	}

	@Override
	public void routingKeyBridge(RoutingKeyBridge bridge) {
		// No-op
	}
}
