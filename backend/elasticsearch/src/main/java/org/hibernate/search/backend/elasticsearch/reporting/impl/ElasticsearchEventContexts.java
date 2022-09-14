/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.reporting.impl;

import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.impl.AbstractSimpleEventContextElement;

public final class ElasticsearchEventContexts {

	private static final ElasticsearchEventContextMessages MESSAGES = ElasticsearchEventContextMessages.INSTANCE;

	private ElasticsearchEventContexts() {
	}

	public static EventContext fromMappingAttribute(String attributeName) {
		return EventContext.create( new AbstractSimpleEventContextElement<String>( attributeName ) {
			@Override
			public String render(String name) {
				return MESSAGES.mappingAttribute( name );
			}
		} );
	}

	public static EventContext fromAnalysisDefinitionParameter(String parameterName) {
		return EventContext.create( new AbstractSimpleEventContextElement<String>( parameterName ) {
			@Override
			public String render(String name) {
				return MESSAGES.analysisDefinitionParameter( name );
			}
		} );
	}

	public static EventContext fromAliasDefinition(String name) {
		return EventContext.create( new AbstractSimpleEventContextElement<String>( name ) {
			@Override
			public String render(String name) {
				return MESSAGES.aliasDefinition( name );
			}
		} );
	}

	public static EventContext fromAliasDefinitionAttribute(String name) {
		return EventContext.create( new AbstractSimpleEventContextElement<String>( name ) {
			@Override
			public String render(String name) {
				return MESSAGES.analysisDefinitionAttribute( name );
			}
		} );
	}

	public static EventContext fromFieldTemplateAttribute(String name) {
		return EventContext.create( new AbstractSimpleEventContextElement<String>( name ) {
			@Override
			public String render(String name) {
				return MESSAGES.fieldTemplateAttribute( name );
			}
		} );
	}

	public static EventContext fromCustomIndexSettingAttribute(String name) {
		return EventContext.create( new AbstractSimpleEventContextElement<String>( name ) {
			@Override
			public String render(String name) {
				return MESSAGES.customIndexSettingAttribute( name );
			}
		} );
	}

	public static EventContext fromCustomIndexMappingAttribute(String name) {
		return EventContext.create( new AbstractSimpleEventContextElement<String>( name ) {
			@Override
			public String render(String name) {
				return MESSAGES.customIndexSettingAttribute( name );
			}
		} );
	}
}
