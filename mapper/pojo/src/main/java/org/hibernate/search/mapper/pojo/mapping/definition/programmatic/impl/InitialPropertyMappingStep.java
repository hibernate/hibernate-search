/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.spi.ErrorCollectingPojoPropertyMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.AssociationInverseSideOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.IndexingDependencyOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingDocumentIdOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingGenericFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingIndexedEmbeddedStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingKeywordFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingScaledNumberFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;

class InitialPropertyMappingStep
		implements PropertyMappingStep, PojoTypeMetadataContributor {

	private final TypeMappingStep parent;
	private final PojoPropertyModel<?> propertyModel;

	private final ErrorCollectingPojoPropertyMetadataContributor children =
			new ErrorCollectingPojoPropertyMetadataContributor();

	InitialPropertyMappingStep(TypeMappingStep parent, PojoPropertyModel<?> propertyModel) {
		this.parent = parent;
		this.propertyModel = propertyModel;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		PojoAdditionalMetadataCollectorPropertyNode collectorPropertyNode =
				collector.property( propertyModel.getName() );
		children.contributeAdditionalMetadata( collectorPropertyNode );
	}

	@Override
	public void contributeMapping(PojoMappingCollectorTypeNode collector) {
		PojoMappingCollectorPropertyNode collectorPropertyNode = collector.property( propertyModel.getName() );
		children.contributeMapping( collectorPropertyNode );
	}

	@Override
	public PropertyMappingDocumentIdOptionsStep documentId() {
		PropertyMappingDocumentIdOptionsStepImpl child = new PropertyMappingDocumentIdOptionsStepImpl( this );
		children.add( child );
		return child;
	}

	@Override
	public PropertyMappingStep property(String propertyName) {
		return parent.property( propertyName );
	}

	@Override
	public PropertyMappingStep bridge(Class<? extends PropertyBridge> bridgeClass) {
		return bridge( BeanReference.of( bridgeClass ) );
	}

	@Override
	public PropertyMappingStep bridge(BeanReference<? extends PropertyBridge> bridgeReference) {
		return bridge( new BeanBridgeBuilder<>( bridgeReference ) );
	}

	@Override
	public PropertyMappingStep bridge(BridgeBuilder<? extends PropertyBridge> builder) {
		children.add( new PropertyBridgeMappingContributor( builder ) );
		return this;
	}

	@Override
	public PropertyMappingStep marker(MarkerBuilder builder) {
		children.add( new MarkerMappingContributor( builder ) );
		return this;
	}

	@Override
	public PropertyMappingGenericFieldOptionsStep genericField() {
		return genericField( null );
	}

	@Override
	public PropertyMappingGenericFieldOptionsStep genericField(String relativeFieldName) {
		PropertyMappingGenericFieldOptionsStepImpl child = new PropertyMappingGenericFieldOptionsStepImpl( this, relativeFieldName );
		children.add( child );
		return child;
	}

	@Override
	public PropertyMappingFullTextFieldOptionsStep fullTextField() {
		return fullTextField( null );
	}

	@Override
	public PropertyMappingFullTextFieldOptionsStep fullTextField(String relativeFieldName) {
		PropertyMappingFullTextFieldOptionsStepImpl child = new PropertyMappingFullTextFieldOptionsStepImpl( this, relativeFieldName );
		children.add( child );
		return child;
	}

	@Override
	public PropertyMappingKeywordFieldOptionsStep keywordField() {
		return keywordField( null );
	}

	@Override
	public PropertyMappingKeywordFieldOptionsStep keywordField(String relativeFieldName) {
		PropertyMappingKeywordFieldOptionsStepImpl child = new PropertyMappingKeywordFieldOptionsStepImpl( this, relativeFieldName );
		children.add( child );
		return child;
	}

	@Override
	public PropertyMappingScaledNumberFieldOptionsStep scaledNumberField() {
		return scaledNumberField( null );
	}

	@Override
	public PropertyMappingScaledNumberFieldOptionsStep scaledNumberField(String relativeFieldName) {
		PropertyMappingScaledNumberFieldOptionsStepImpl child = new PropertyMappingScaledNumberFieldOptionsStepImpl( this, relativeFieldName );
		children.add( child );
		return child;
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep indexedEmbedded() {
		PropertyMappingIndexedEmbeddedStepImpl child = new PropertyMappingIndexedEmbeddedStepImpl( this );
		children.add( child );
		return child;
	}

	@Override
	public AssociationInverseSideOptionsStep associationInverseSide(PojoModelPathValueNode inversePath) {
		AssociationInverseSideOptionsStepImpl child = new AssociationInverseSideOptionsStepImpl(
				this, inversePath
		);
		children.add( child );
		return child;
	}

	@Override
	public IndexingDependencyOptionsStep indexingDependency() {
		IndexingDependencyOptionsStepImpl child = new IndexingDependencyOptionsStepImpl( this );
		children.add( child );
		return child;
	}
}
