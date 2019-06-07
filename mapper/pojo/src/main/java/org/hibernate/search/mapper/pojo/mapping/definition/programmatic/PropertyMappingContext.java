/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.environment.bean.BeanReference;
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

	/**
	 * @param bridgeClass The class of the bridge to use.
	 * @return {@code this}, for method chaining.
	 */
	PropertyMappingContext bridge(Class<? extends PropertyBridge> bridgeClass);

	/**
	 * @param bridgeReference A {@link BeanReference} pointing to the bridge to use.
	 * See the static "ofXXX()" methods of {@link BeanReference} for details about the various type of references
	 * (by name, by type, ...).
	 * @return {@code this}, for method chaining.
	 */
	PropertyMappingContext bridge(BeanReference<? extends PropertyBridge> bridgeReference);

	/**
	 * @param builder A bridge builder.
	 * @return {@code this}, for method chaining.
	 */
	PropertyMappingContext bridge(BridgeBuilder<? extends PropertyBridge> builder);

	PropertyMappingContext marker(MarkerBuilder builder);

	PropertyGenericFieldMappingContext genericField();

	PropertyGenericFieldMappingContext genericField(String relativeFieldName);

	PropertyFullTextFieldMappingContext fullTextField();

	PropertyFullTextFieldMappingContext fullTextField(String relativeFieldName);

	PropertyKeywordFieldMappingContext keywordField();

	PropertyKeywordFieldMappingContext keywordField(String relativeFieldName);

	PropertyScaledNumberFieldMappingContext scaledNumberField();

	PropertyScaledNumberFieldMappingContext scaledNumberField(String relativeFieldName);

	PropertyIndexedEmbeddedMappingContext indexedEmbedded();

	AssociationInverseSideMappingContext associationInverseSide(PojoModelPathValueNode inversePath);

	IndexingDependencyMappingContext indexingDependency();

}
