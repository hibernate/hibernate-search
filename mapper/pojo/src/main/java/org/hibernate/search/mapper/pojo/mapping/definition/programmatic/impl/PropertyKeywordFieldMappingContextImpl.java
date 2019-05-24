/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyKeywordFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class PropertyKeywordFieldMappingContextImpl
		extends AbstractPropertyNotFullTextFieldMappingContext<PropertyKeywordFieldMappingContext, StringIndexFieldTypeContext<?>>
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
		fieldModelContributor.add( (c, b) -> c.normalizer( normalizerName ) );
		return thisAsS();
	}

	@Override
	public PropertyKeywordFieldMappingContext norms(Norms norms) {
		fieldModelContributor.add( (c, b) -> c.norms( norms ) );
		return thisAsS();
	}

	private static StringIndexFieldTypeContext<?> convertFieldTypedContext(
			StandardIndexFieldTypeContext<?,?> context) {
		if ( context instanceof StringIndexFieldTypeContext ) {
			return (StringIndexFieldTypeContext<?>) context;
		}
		else {
			throw log.invalidFieldEncodingForKeywordFieldMapping(
					context, StringIndexFieldTypeContext.class
			);
		}
	}

}
