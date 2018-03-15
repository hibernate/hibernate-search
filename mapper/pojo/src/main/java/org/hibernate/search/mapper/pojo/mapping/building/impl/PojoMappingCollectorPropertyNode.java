/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.util.List;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;

public interface PojoMappingCollectorPropertyNode extends PojoMappingCollector {

	void bridge(BridgeBuilder<? extends PropertyBridge> builder);

	void identifierBridge(BridgeBuilder<? extends IdentifierBridge<?>> reference);

	void containedIn();

	PojoMappingCollectorValueNode valueWithoutExtractors();

	PojoMappingCollectorValueNode valueWithDefaultExtractors();

	PojoMappingCollectorValueNode valueWithExtractors(
			List<? extends Class<? extends ContainerValueExtractor>> extractorClasses);

}
