/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;

public interface PojoMappingCollectorTypeNode extends PojoMappingCollector {

	void typeBinder(TypeBinder builder);

	void routingKeyBinder(RoutingKeyBinder reference);

	PojoMappingCollectorPropertyNode property(String propertyName);

}
