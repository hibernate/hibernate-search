/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.logging.spi;

import java.util.Set;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.logging.impl.EngineFailureContextMessages;
import org.hibernate.search.util.FailureContext;
import org.hibernate.search.util.FailureContextElement;
import org.hibernate.search.util.impl.common.CollectionHelper;

import org.jboss.logging.Messages;

public class FailureContexts {

	private static final EngineFailureContextMessages MESSAGES = Messages.getBundle( EngineFailureContextMessages.class );

	private static final FailureContext DEFAULT = FailureContext.create(
			new FailureContextElement() {
				@Override
				public String toString() {
					return "FailureContextElement[" + render() + "]";
				}

				@Override
				public String render() {
					return MESSAGES.defaultOnMissingContextElement();
				}
			}
	);

	private static final FailureContext INDEX_SCHEMA_ROOT = FailureContext.create(
			new FailureContextElement() {
				@Override
				public String toString() {
					return "FailureContextElement[" + render() + "]";
				}

				@Override
				public String render() {
					return MESSAGES.indexSchemaRoot();
				}
			}
	);

	private FailureContexts() {
	}

	public static FailureContext getDefault() {
		return DEFAULT;
	}

	public static FailureContext fromType(MappableTypeModel typeModel) {
		return FailureContext.create( new AbstractSimpleFailureContextElement<MappableTypeModel>( typeModel ) {
			@Override
			public String render(MappableTypeModel param) {
				String typeName = param.getName();
				return MESSAGES.type( typeName );
			}
		} );
	}

	public static FailureContext fromBackendName(String name) {
		return FailureContext.create( new AbstractSimpleFailureContextElement<String>( name ) {
			@Override
			public String render(String param) {
				return MESSAGES.backend( param );
			}
		} );
	}

	public static FailureContext fromIndexName(String name) {
		return FailureContext.create( new AbstractSimpleFailureContextElement<String>( name ) {
			@Override
			public String render(String param) {
				return MESSAGES.index( param );
			}
		} );
	}
	public static FailureContext fromIndexNames(String ... indexNames) {
		return fromIndexNames( CollectionHelper.asLinkedHashSet( indexNames ) );
	}

	public static FailureContext fromIndexNames(Set<String> indexNames) {
		return FailureContext.create( new AbstractSimpleFailureContextElement<Set<String>>( indexNames ) {
			@Override
			public String render(Set<String> indexNames) {
				return MESSAGES.indexes( indexNames );
			}
		} );
	}


	public static FailureContext indexSchemaRoot() {
		return INDEX_SCHEMA_ROOT;
	}

	public static FailureContext fromIndexFieldAbsolutePath(String absolutePath) {
		return FailureContext.create( new AbstractSimpleFailureContextElement<String>( absolutePath ) {
			@Override
			public String render(String param) {
				return MESSAGES.indexFieldAbsolutePath( param );
			}
		} );
	}

}
