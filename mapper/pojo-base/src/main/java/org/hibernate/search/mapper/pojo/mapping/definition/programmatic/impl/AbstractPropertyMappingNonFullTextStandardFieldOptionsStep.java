/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingNonFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

abstract class AbstractPropertyMappingNonFullTextStandardFieldOptionsStep<
		S extends PropertyMappingNonFullTextFieldOptionsStep<?>>
		extends AbstractPropertyMappingStandardFieldOptionsStep<S>
		implements PropertyMappingNonFullTextFieldOptionsStep<S> {

	AbstractPropertyMappingNonFullTextStandardFieldOptionsStep(PropertyMappingStep parent, String relativeFieldName,
			PojoCompositeFieldModelContributor.Contributor fieldTypeChecker) {
		super( parent, relativeFieldName, fieldTypeChecker );
	}

	@Override
	public S sortable(Sortable sortable) {
		fieldModelContributor.add( c -> c.standardTypeOptionsStep().sortable( sortable ) );
		return thisAsS();
	}

	@Override
	public S aggregable(Aggregable aggregable) {
		fieldModelContributor.add( c -> c.standardTypeOptionsStep().aggregable( aggregable ) );
		return thisAsS();
	}

	@Override
	public S indexNullAs(String indexNullAs) {
		fieldModelContributor.add( c -> c.indexNullAs( indexNullAs ) );
		return thisAsS();
	}
}
