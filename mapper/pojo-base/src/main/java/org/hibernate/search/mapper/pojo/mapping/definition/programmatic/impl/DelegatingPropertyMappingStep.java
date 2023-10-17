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
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

class DelegatingPropertyMappingStep implements PropertyMappingStep {

	private final PropertyMappingStep delegate;

	DelegatingPropertyMappingStep(PropertyMappingStep delegate) {
		this.delegate = delegate;
	}

	@Override
	public TypeMappingStep hostingType() {
		return delegate.hostingType();
	}

	@Override
	public PropertyMappingDocumentIdOptionsStep documentId() {
		return delegate.documentId();
	}

	@Override
	public PropertyMappingStep binder(PropertyBinder binder, Map<String, Object> params) {
		return delegate.binder( binder, params );
	}

	@Override
	public PropertyMappingStep marker(MarkerBinder binder, Map<String, Object> params) {
		return delegate.marker( binder );
	}

	@Override
	public PropertyMappingGenericFieldOptionsStep genericField() {
		return delegate.genericField();
	}

	@Override
	public PropertyMappingGenericFieldOptionsStep genericField(String relativeFieldName) {
		return delegate.genericField( relativeFieldName );
	}

	@Override
	public PropertyMappingFullTextFieldOptionsStep fullTextField() {
		return delegate.fullTextField();
	}

	@Override
	public PropertyMappingFullTextFieldOptionsStep fullTextField(String relativeFieldName) {
		return delegate.fullTextField( relativeFieldName );
	}

	@Override
	public PropertyMappingKeywordFieldOptionsStep keywordField() {
		return delegate.keywordField();
	}

	@Override
	public PropertyMappingKeywordFieldOptionsStep keywordField(String relativeFieldName) {
		return delegate.keywordField( relativeFieldName );
	}

	@Override
	public PropertyMappingScaledNumberFieldOptionsStep scaledNumberField() {
		return delegate.scaledNumberField();
	}

	@Override
	public PropertyMappingScaledNumberFieldOptionsStep scaledNumberField(String relativeFieldName) {
		return delegate.scaledNumberField( relativeFieldName );
	}

	@Override
	public PropertyMappingFieldOptionsStep<?> nonStandardField() {
		return delegate.nonStandardField();
	}

	@Override
	public PropertyMappingFieldOptionsStep<?> nonStandardField(String relativeFieldName) {
		return delegate.nonStandardField( relativeFieldName );
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep indexedEmbedded() {
		return delegate.indexedEmbedded();
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep indexedEmbedded(String relativeFieldName) {
		return delegate.indexedEmbedded( relativeFieldName );
	}

	@Override
	public AssociationInverseSideOptionsStep associationInverseSide(PojoModelPathValueNode inversePath) {
		return delegate.associationInverseSide( inversePath );
	}

	@Override
	public IndexingDependencyOptionsStep indexingDependency() {
		return delegate.indexingDependency();
	}

	@Override
	public PropertyMappingVectorFieldStep vectorField(int dimension) {
		return delegate.vectorField( dimension );
	}

	@Override
	public PropertyMappingVectorFieldStep vectorField(int dimension, String relativeFieldName) {
		return delegate.vectorField( dimension, relativeFieldName );
	}
}
