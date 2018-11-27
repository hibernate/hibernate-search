/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.StringIndexSchemaFieldTypedContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyKeywordFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.util.impl.common.LoggerFactory;


class PropertyKeywordFieldMappingContextImpl
		extends AbstractPropertySortableFieldMappingContext<PropertyKeywordFieldMappingContext, StringIndexSchemaFieldTypedContext<?>>
		implements PropertyKeywordFieldMappingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	PropertyKeywordFieldMappingContextImpl(PropertyMappingContext parent, String relativeFieldName) {
		super( parent, relativeFieldName, PropertyKeywordFieldMappingContextImpl::convertFieldTypedContext );
	}

	@Override
	PropertyKeywordFieldMappingContext thisAsS() {
		return this;
	}

	@Override
	public PropertyKeywordFieldMappingContext normalizer(String normalizerName) {
		fieldModelContributor.add( c -> c.normalizer( normalizerName ) );
		return thisAsS();
	}

	private static StringIndexSchemaFieldTypedContext<?> convertFieldTypedContext(
			StandardIndexSchemaFieldTypedContext<?,?> context) {
		if ( context instanceof StringIndexSchemaFieldTypedContext ) {
			return (StringIndexSchemaFieldTypedContext<?>) context;
		}
		else {
			throw log.invalidFieldEncodingForKeywordFieldMapping(
					context, StringIndexSchemaFieldTypedContext.class
			);
		}
	}

}
