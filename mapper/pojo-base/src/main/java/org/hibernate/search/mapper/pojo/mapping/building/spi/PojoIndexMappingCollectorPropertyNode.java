/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;

public interface PojoIndexMappingCollectorPropertyNode extends PojoMappingCollector {

	void propertyBinder(PropertyBinder binder, Map<String, Object> params);

	void identifierBinder(IdentifierBinder binder, Map<String, Object> params);

	PojoIndexMappingCollectorValueNode value(ContainerExtractorPath extractorPath);

}
