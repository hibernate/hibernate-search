/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.logging.spi;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.logging.impl.FailureContextMessages;

import org.jboss.logging.Messages;

public class FailureContexts {

	private static final FailureContextMessages MESSAGES = Messages.getBundle( FailureContextMessages.class );

	private static final FailureContextElement INDEX_SCHEMA_ROOT = new FailureContextElement() {
		@Override
		public String toString() {
			return "FailureContextElement[" + render() + "]";
		}

		@Override
		public String render() {
			return MESSAGES.indexSchemaRoot();
		}
	};

	private FailureContexts() {
	}

	public static FailureContextElement fromType(MappableTypeModel typeModel) {
		return new AbstractSimpleFailureContextElement<MappableTypeModel>( typeModel ) {
			@Override
			public String render(MappableTypeModel param) {
				String typeName = param.getName();
				return MESSAGES.type( typeName );
			}
		};
	}

	public static FailureContextElement fromBackendName(String name) {
		return new AbstractSimpleFailureContextElement<String>( name ) {
			@Override
			public String render(String param) {
				return MESSAGES.backend( param );
			}
		};
	}

	public static FailureContextElement fromIndexName(String name) {
		return new AbstractSimpleFailureContextElement<String>( name ) {
			@Override
			public String render(String param) {
				return MESSAGES.index( param );
			}
		};
	}

	public static FailureContextElement indexSchemaRoot() {
		return INDEX_SCHEMA_ROOT;
	}

	public static FailureContextElement fromIndexFieldAbsolutePath(String absolutePath) {
		return new AbstractSimpleFailureContextElement<String>( absolutePath ) {
			@Override
			public String render(String param) {
				return MESSAGES.indexFieldAbsolutePath( param );
			}
		};
	}

}
