/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuilder;
import org.hibernate.search.mapper.pojo.bridge.Bridge;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyDocumentIdMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyIndexedEmbeddedMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;


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
	public PropertyMappingContext bridge(Class<? extends Bridge> bridgeClass) {
		return delegate.bridge( bridgeClass );
	}

	@Override
	public PropertyMappingContext bridge(String bridgeName, Class<? extends Bridge> bridgeClass) {
		return delegate.bridge( bridgeName, bridgeClass );
	}

	@Override
	public PropertyMappingContext bridge(BridgeBuilder<? extends Bridge> builder) {
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
	public PropertyIndexedEmbeddedMappingContext indexedEmbedded() {
		return delegate.indexedEmbedded();
	}

	@Override
	public PropertyMappingContext containedIn() {
		return delegate.containedIn();
	}

}
