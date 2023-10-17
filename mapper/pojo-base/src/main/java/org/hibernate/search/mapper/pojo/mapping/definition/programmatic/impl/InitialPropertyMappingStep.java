/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.mapping.building.spi.ErrorCollectingPojoPropertyMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.AssociationInverseSideOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.IndexingDependencyOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingDocumentIdOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingGenericFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingIndexedEmbeddedStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingKeywordFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingScaledNumberFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingVectorFieldStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;

class InitialPropertyMappingStep
		implements PropertyMappingStep, PojoTypeMetadataContributor {

	private final TypeMappingStepImpl parent;
	private final PojoPropertyModel<?> propertyModel;

	private final ErrorCollectingPojoPropertyMetadataContributor children =
			new ErrorCollectingPojoPropertyMetadataContributor();

	InitialPropertyMappingStep(TypeMappingStepImpl parent, PojoPropertyModel<?> propertyModel) {
		this.parent = parent;
		this.propertyModel = propertyModel;
	}

	@Override
	public TypeMappingStep hostingType() {
		return parent;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		if ( children.hasContent() ) {
			collector.property( propertyModel.name(), children::contributeAdditionalMetadata );
		}
	}

	@Override
	public void contributeIndexMapping(PojoIndexMappingCollectorTypeNode collector) {
		if ( children.hasContent() ) {
			PojoIndexMappingCollectorPropertyNode collectorPropertyNode = collector.property( propertyModel.name() );
			children.contributeIndexMapping( collectorPropertyNode );
		}
	}

	@Override
	public PropertyMappingDocumentIdOptionsStep documentId() {
		PropertyMappingDocumentIdOptionsStepImpl child = new PropertyMappingDocumentIdOptionsStepImpl( this );
		children.add( child );
		return child;
	}

	@Override
	public PropertyMappingStep binder(PropertyBinder binder, Map<String, Object> params) {
		children.add( new PropertyBridgeMappingContributor( binder, params ) );
		return this;
	}

	@Override
	public PropertyMappingStep marker(MarkerBinder binder, Map<String, Object> params) {
		children.add( new MarkerMappingContributor( binder, params ) );
		return this;
	}

	@Override
	public PropertyMappingGenericFieldOptionsStep genericField() {
		return genericField( null );
	}

	@Override
	public PropertyMappingGenericFieldOptionsStep genericField(String relativeFieldName) {
		PropertyMappingGenericFieldOptionsStepImpl child =
				new PropertyMappingGenericFieldOptionsStepImpl( this, relativeFieldName );
		children.add( child );
		return child;
	}

	@Override
	public PropertyMappingFullTextFieldOptionsStep fullTextField() {
		return fullTextField( null );
	}

	@Override
	public PropertyMappingFullTextFieldOptionsStep fullTextField(String relativeFieldName) {
		PropertyMappingFullTextFieldOptionsStepImpl child =
				new PropertyMappingFullTextFieldOptionsStepImpl( this, relativeFieldName );
		children.add( child );
		return child;
	}

	@Override
	public PropertyMappingKeywordFieldOptionsStep keywordField() {
		return keywordField( null );
	}

	@Override
	public PropertyMappingKeywordFieldOptionsStep keywordField(String relativeFieldName) {
		PropertyMappingKeywordFieldOptionsStepImpl child =
				new PropertyMappingKeywordFieldOptionsStepImpl( this, relativeFieldName );
		children.add( child );
		return child;
	}

	@Override
	public PropertyMappingScaledNumberFieldOptionsStep scaledNumberField() {
		return scaledNumberField( null );
	}

	@Override
	public PropertyMappingScaledNumberFieldOptionsStep scaledNumberField(String relativeFieldName) {
		PropertyMappingScaledNumberFieldOptionsStepImpl child =
				new PropertyMappingScaledNumberFieldOptionsStepImpl( this, relativeFieldName );
		children.add( child );
		return child;
	}

	@Override
	public PropertyMappingFieldOptionsStep<?> nonStandardField() {
		return nonStandardField( null );
	}

	@Override
	public PropertyMappingFieldOptionsStep<?> nonStandardField(String relativeFieldName) {
		PropertyMappingNonStandardFieldOptionsStep child =
				new PropertyMappingNonStandardFieldOptionsStep( this, relativeFieldName );
		children.add( child );
		return child;
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep indexedEmbedded() {
		return indexedEmbedded( null );
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep indexedEmbedded(String relativeFieldName) {
		PropertyMappingIndexedEmbeddedStepImpl child = new PropertyMappingIndexedEmbeddedStepImpl(
				this, parent.getTypeModel().typeIdentifier(), relativeFieldName
		);
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

	@Override
	public PropertyMappingVectorFieldStep vectorField(int dimension) {
		PropertyMappingVectorFieldStepImpl child = new PropertyMappingVectorFieldStepImpl( this, dimension, null );
		children.add( child );
		return child;
	}

	@Override
	public PropertyMappingVectorFieldStep vectorField(int dimension, String relativeFieldName) {
		PropertyMappingVectorFieldStepImpl child = new PropertyMappingVectorFieldStepImpl( this, dimension, relativeFieldName );
		children.add( child );
		return child;
	}
}
