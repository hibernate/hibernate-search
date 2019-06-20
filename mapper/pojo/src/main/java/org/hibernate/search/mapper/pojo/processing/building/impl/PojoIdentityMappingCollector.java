/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.building.impl;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.BoundRoutingKeyBridge;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;

public interface PojoIdentityMappingCollector {

	<T> void identifierBridge(BoundPojoModelPathPropertyNode<?, T> modelPath,
			BridgeBuilder<? extends IdentifierBridge<?>> builder);

	<T> BoundRoutingKeyBridge<T> routingKeyBridge(BoundPojoModelPathTypeNode<T> modelPath,
			BridgeBuilder<? extends RoutingKeyBridge> builder);
}
