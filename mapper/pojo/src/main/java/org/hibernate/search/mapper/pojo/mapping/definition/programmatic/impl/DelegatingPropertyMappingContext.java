/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuilder;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.AssociationInverseSideMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyDocumentIdMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyIndexedEmbeddedMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;


/**
 * @author Yoann Rodiere
 */
public class DelegatingPropertyMappingContext implements PropertyMappingContext {

	private final PropertyMappingContext delegate;

	protected DelegatingPropertyMappingContext(PropertyMappingContext delegate) {
		this.delegate = delegate;
	}

	@Override
	public PropertyDocumentIdMappingContext documentId() {
		return delegate.documentId();
	}

	@Override
	public PropertyMappingContext property(String propertyName) {
		return delegate.property( propertyName );
	}

	@Override
	public PropertyMappingContext bridge(String bridgeName) {
		return delegate.bridge( bridgeName );
	}

	@Override
	public PropertyMappingContext bridge(Class<? extends PropertyBridge> bridgeClass) {
		return delegate.bridge( bridgeClass );
	}

	@Override
	public PropertyMappingContext bridge(String bridgeName, Class<? extends PropertyBridge> bridgeClass) {
		return delegate.bridge( bridgeName, bridgeClass );
	}

	@Override
	public PropertyMappingContext bridge(BridgeBuilder<? extends PropertyBridge> builder) {
		return delegate.bridge( builder );
	}

	@Override
	public PropertyMappingContext marker(MarkerBuilder definition) {
		return delegate.marker( definition );
	}

	@Override
	public PropertyFieldMappingContext field() {
		return delegate.field();
	}

	@Override
	public PropertyFieldMappingContext field(String fieldName) {
		return delegate.field( fieldName );
	}

	@Override
	public PropertyIndexedEmbeddedMappingContext indexedEmbedded() {
		return delegate.indexedEmbedded();
	}

	@Override
	public AssociationInverseSideMappingContext associationInverseSide(PojoModelPathValueNode inversePath) {
		return delegate.associationInverseSide( inversePath );
	}

}
