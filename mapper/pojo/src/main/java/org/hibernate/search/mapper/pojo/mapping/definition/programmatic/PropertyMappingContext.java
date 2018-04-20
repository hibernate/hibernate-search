/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuilder;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

/**
 * @author Yoann Rodiere
 */
public interface PropertyMappingContext {

	/**
	 * @param propertyName The name of another property <strong>on the same type</strong> as the current property
	 * (not a nested property).
	 * @return A mapping context for that property.
	 */
	PropertyMappingContext property(String propertyName);

	PropertyDocumentIdMappingContext documentId();

	PropertyMappingContext bridge(String bridgeName);

	PropertyMappingContext bridge(Class<? extends PropertyBridge> bridgeClass);

	PropertyMappingContext bridge(String bridgeName, Class<? extends PropertyBridge> bridgeClass);

	PropertyMappingContext bridge(BridgeBuilder<? extends PropertyBridge> builder);

	PropertyMappingContext marker(MarkerBuilder builder);

	PropertyFieldMappingContext field();

	PropertyFieldMappingContext field(String relativeFieldName);

	PropertyIndexedEmbeddedMappingContext indexedEmbedded();

	AssociationInverseSideMappingContext associationInverseSide(PojoModelPathValueNode inversePath);

}
