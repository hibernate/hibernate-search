/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;

public interface PojoMappingCollectorPropertyNode extends PojoMappingCollector {

	void bridge(BridgeBuilder<? extends PropertyBridge> builder);

	void identifierBridge(BridgeBuilder<? extends IdentifierBridge<?>> reference);

	PojoMappingCollectorValueNode value(ContainerExtractorPath extractorPath);

}
