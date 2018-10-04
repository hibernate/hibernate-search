/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyFullTextFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;


class PropertyFullTextFieldMappingContextImpl extends PropertyFieldMappingContextImpl<PropertyFullTextFieldMappingContext>
		implements PropertyFullTextFieldMappingContext {

	PropertyFullTextFieldMappingContextImpl(PropertyMappingContext parent, String relativeFieldName) {
		super( parent, relativeFieldName );
	}

	@Override
	PropertyFullTextFieldMappingContext thisAsS() {
		return this;
	}

	@Override
	public PropertyFullTextFieldMappingContext analyzer(String normalizerName) {
		fieldModelContributor.add( c -> c.analyzer( normalizerName ) );
		return thisAsS();
	}

}
