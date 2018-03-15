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
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

class NoOpPojoIdentityMappingCollector implements PojoIdentityMappingCollector {

	static final NoOpPojoIdentityMappingCollector INSTANCE = new NoOpPojoIdentityMappingCollector();

	@Override
	public <T> void identifierBridge(PojoTypeModel<T> propertyTypeModel, PropertyHandle handle, IdentifierBridge<T> bridge) {
		// No-op
	}

	@Override
	public void routingKeyBridge(RoutingKeyBridge bridge) {
		// No-op
	}
}
