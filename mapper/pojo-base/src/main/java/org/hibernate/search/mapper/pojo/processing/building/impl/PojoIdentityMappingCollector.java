/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.building.impl;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;

public interface PojoIdentityMappingCollector {

	<T> void identifierBridge(BoundPojoModelPathPropertyNode<?, T> modelPath,
			IdentifierBinder binder);

	<T> void routingKeyBridge(BoundPojoModelPathTypeNode<T> modelPath,
			RoutingKeyBinder binder);
}
