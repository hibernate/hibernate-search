/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBuilder;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.AssociationInverseSideMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.IndexingDependencyMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyDocumentIdMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyFullTextFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyGenericFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyIndexedEmbeddedMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyKeywordFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyScaledNumberFieldMappingContext;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;



class DelegatingPropertyMappingContext implements PropertyMappingContext {

	private final PropertyMappingContext delegate;

	DelegatingPropertyMappingContext(PropertyMappingContext delegate) {
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
	public PropertyMappingContext bridge(Class<? extends PropertyBridge> bridgeClass) {
		return delegate.bridge( bridgeClass );
	}

	@Override
	public PropertyMappingContext bridge(BeanReference<? extends PropertyBridge> bridgeReference) {
		return delegate.bridge( bridgeReference );
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
	public PropertyGenericFieldMappingContext genericField() {
		return delegate.genericField();
	}

	@Override
	public PropertyGenericFieldMappingContext genericField(String relativeFieldName) {
		return delegate.genericField( relativeFieldName );
	}

	@Override
	public PropertyFullTextFieldMappingContext fullTextField() {
		return delegate.fullTextField();
	}

	@Override
	public PropertyFullTextFieldMappingContext fullTextField(String relativeFieldName) {
		return delegate.fullTextField( relativeFieldName );
	}

	@Override
	public PropertyKeywordFieldMappingContext keywordField() {
		return delegate.keywordField();
	}

	@Override
	public PropertyKeywordFieldMappingContext keywordField(String relativeFieldName) {
		return delegate.keywordField( relativeFieldName );
	}

	@Override
	public PropertyScaledNumberFieldMappingContext scaledNumberField() {
		return delegate.scaledNumberField();
	}

	@Override
	public PropertyScaledNumberFieldMappingContext scaledNumberField(String relativeFieldName) {
		return delegate.scaledNumberField( relativeFieldName );
	}

	@Override
	public PropertyIndexedEmbeddedMappingContext indexedEmbedded() {
		return delegate.indexedEmbedded();
	}

	@Override
	public AssociationInverseSideMappingContext associationInverseSide(PojoModelPathValueNode inversePath) {
		return delegate.associationInverseSide( inversePath );
	}

	@Override
	public IndexingDependencyMappingContext indexingDependency() {
		return delegate.indexingDependency();
	}
}
