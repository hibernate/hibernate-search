/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyFullTextFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.util.impl.common.LoggerFactory;


class PropertyFullTextFieldMappingContextImpl
		extends AbstractPropertyFieldMappingContext<PropertyFullTextFieldMappingContext, StringIndexFieldTypeContext<?>>
		implements PropertyFullTextFieldMappingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	PropertyFullTextFieldMappingContextImpl(PropertyMappingContext parent, String relativeFieldName) {
		super(
				parent, relativeFieldName,
				PropertyFullTextFieldMappingContextImpl::convertFieldTypedContext
		);
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

	private static StringIndexFieldTypeContext<?> convertFieldTypedContext(StandardIndexFieldTypeContext<?,?> context) {
		if ( context instanceof StringIndexFieldTypeContext ) {
			return (StringIndexFieldTypeContext<?>) context;
		}
		else {
			throw log.invalidFieldEncodingForFullTextFieldMapping(
					context, StringIndexFieldTypeContext.class
			);
		}
	}

}
