/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting.spi;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.reporting.impl.EngineEventContextMessages;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.EventContextElement;
import org.hibernate.search.util.common.reporting.impl.AbstractSimpleEventContextElement;

public class EventContexts {

	private static final EngineEventContextMessages MESSAGES = EngineEventContextMessages.INSTANCE;

	private static final EventContext DEFAULT = singleton( MESSAGES::defaultOnMissingContextElement );

	private static final EventContext DEFAULT_BACKEND = singleton( MESSAGES::defaultBackend );

	private static final EventContext INDEX_SCHEMA_ROOT = singleton( MESSAGES::indexSchemaRoot );

	private static final EventContext INDEX_SCHEMA_IDENTIFIER = singleton( MESSAGES::indexSchemaIdentifier );

	private EventContexts() {
	}

	public static EventContext singleton(Supplier<String> render) {
		return EventContext.create( new EventContextElement() {
			@Override
			public String toString() {
				return "EventContextElement[" + render() + "]";
			}

			@Override
			public String render() {
				return render.get();
			}
		} );
	}

	public static EventContext defaultContext() {
		return DEFAULT;
	}

	public static EventContext indexSchemaRoot() {
		return INDEX_SCHEMA_ROOT;
	}

	public static EventContext fromType(MappableTypeModel typeModel) {
		return EventContext.create( new AbstractSimpleEventContextElement<MappableTypeModel>( typeModel ) {
			@Override
			public String render(MappableTypeModel param) {
				String typeName = param.name();
				return MESSAGES.type( typeName );
			}
		} );
	}

	public static EventContext fromType(Object typeIdentifier) {
		return EventContext.create( new AbstractSimpleEventContextElement<Object>( typeIdentifier ) {
			@Override
			public String render(Object param) {
				String typeName = param.toString();
				return MESSAGES.type( typeName );
			}
		} );
	}

	public static EventContext defaultBackend() {
		return DEFAULT_BACKEND;
	}

	public static EventContext fromBackendName(String name) {
		if ( name == null ) {
			return DEFAULT_BACKEND;
		}
		else {
			return EventContext.create( new AbstractSimpleEventContextElement<String>( name ) {
				@Override
				public String render(String param) {
					return MESSAGES.backend( param );
				}
			} );
		}
	}

	public static EventContext fromIndexName(String name) {
		return EventContext.create( new AbstractSimpleEventContextElement<String>( name ) {
			@Override
			public String render(String param) {
				return MESSAGES.index( param );
			}
		} );
	}

	public static EventContext fromIndexNames(String... indexNames) {
		return fromIndexNames( CollectionHelper.asLinkedHashSet( indexNames ) );
	}

	public static EventContext fromIndexNames(Set<String> indexNames) {
		return EventContext.create( new AbstractSimpleEventContextElement<Set<String>>( indexNames ) {
			@Override
			public String render(Set<String> indexNames) {
				return MESSAGES.indexes( indexNames );
			}
		} );
	}

	public static EventContext fromIndexNameAndShardId(String name, Optional<String> shardId) {
		EventContext result = EventContext.create( new AbstractSimpleEventContextElement<String>( name ) {
			@Override
			public String render(String param) {
				return MESSAGES.index( param );
			}
		} );
		if ( shardId.isPresent() ) {
			result = result.append( fromShardId( shardId.get() ) );
		}
		return result;
	}

	public static EventContext fromShardId(String shardId) {
		return EventContext.create( new AbstractSimpleEventContextElement<String>( shardId ) {
			@Override
			public String render(String param) {
				return MESSAGES.shard( param );
			}
		} );
	}

	public static EventContext fromIndexFieldAbsolutePath(String absolutePath) {
		return EventContext.create( new AbstractSimpleEventContextElement<String>( absolutePath ) {
			@Override
			public String render(String param) {
				return MESSAGES.indexFieldAbsolutePath( param );
			}
		} );
	}

	public static EventContext indexSchemaIdentifier() {
		return INDEX_SCHEMA_IDENTIFIER;
	}

	public static EventContext fromIndexFieldAbsolutePaths(List<String> absolutePaths) {
		return EventContext.create( new AbstractSimpleEventContextElement<List<String>>( absolutePaths ) {
			@Override
			public String render(List<String> param) {
				return MESSAGES.indexFieldAbsolutePaths( param );
			}
		} );
	}

	public static EventContext fromFieldTemplateAbsolutePath(String templateAbsolutePath) {
		return EventContext.create( new AbstractSimpleEventContextElement<String>( templateAbsolutePath ) {
			@Override
			public String render(String param) {
				return MESSAGES.fieldTemplate( param );
			}
		} );
	}

	public static EventContext fromAnalyzer(String analyzerName) {
		return EventContext.create( new AbstractSimpleEventContextElement<String>( analyzerName ) {
			@Override
			public String render(String param) {
				String analyzerName = param == null ? "" : param;
				return MESSAGES.analyzer( analyzerName );
			}
		} );
	}

	public static EventContext fromNormalizer(String normalizerName) {
		return EventContext.create( new AbstractSimpleEventContextElement<String>( normalizerName ) {
			@Override
			public String render(String param) {
				String normalizerName = param == null ? "" : param;
				return MESSAGES.normalizer( normalizerName );
			}
		} );
	}

	public static EventContext fromCharFilter(String charFilterName) {
		return EventContext.create( new AbstractSimpleEventContextElement<String>( charFilterName ) {
			@Override
			public String render(String param) {
				String charFilterName = param == null ? "" : param;
				return MESSAGES.charFilter( charFilterName );
			}
		} );
	}

	public static EventContext fromTokenizer(String tokenizerName) {
		return EventContext.create( new AbstractSimpleEventContextElement<String>( tokenizerName ) {
			@Override
			public String render(String param) {
				String tokenizerName = param == null ? "" : param;
				return MESSAGES.tokenizer( tokenizerName );
			}
		} );
	}

	public static EventContext fromTokenFilter(String tokenFilterName) {
		return EventContext.create( new AbstractSimpleEventContextElement<String>( tokenFilterName ) {
			@Override
			public String render(String param) {
				String tokenFilterName = param == null ? "" : param;
				return MESSAGES.tokenFilter( tokenFilterName );
			}
		} );
	}
}
