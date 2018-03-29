/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.impl;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultIntegerValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultIntegerIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultLocalDateValueBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultStringValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public final class BridgeResolver {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<Class<?>, BridgeBuilder<? extends IdentifierBridge<?>>> defaultIdentifierBridgeBySourceType = new HashMap<>();
	private final Map<Class<?>, BridgeBuilder<? extends ValueBridge<?, ?>>> defaultValueBridgeBySourceType = new HashMap<>();

	public BridgeResolver() {
		// TODO add an extension point to override these maps, or at least to add defaults for other types
		// TODO add defaults for other types (byte, char, Characeter, Double, double, boolean, Boolean, ...)

		addDefaultIdentifierBridge( Integer.class, ignored -> new DefaultIntegerIdentifierBridge() );

		addDefaultValueBridge( Integer.class, ignored -> new DefaultIntegerValueBridge() );
		addDefaultValueBridge( String.class, ignored -> new DefaultStringValueBridge() );
		addDefaultValueBridge( LocalDate.class, ignored -> new DefaultLocalDateValueBridge() );
	}

	public BridgeBuilder<? extends IdentifierBridge<?>> resolveIdentifierBridgeForType(PojoTypeModel<?> sourceType) {
		// TODO handle non-raw types?
		Class<?> rawType = sourceType.getRawType().getJavaClass();
		@SuppressWarnings("unchecked")
		BridgeBuilder<? extends IdentifierBridge<?>> result = defaultIdentifierBridgeBySourceType.get( rawType );
		if ( result == null ) {
			throw log.unableToResolveDefaultIdentifierBridgeFromSourceType( rawType );
		}
		return result;
	}

	public BridgeBuilder<? extends ValueBridge<?, ?>> resolveValueBridgeForType(PojoTypeModel<?> sourceType) {
		// TODO handle non-raw types?
		Class<?> rawType = sourceType.getRawType().getJavaClass();
		@SuppressWarnings("unchecked")
		BridgeBuilder<? extends ValueBridge<?, ?>> result = defaultValueBridgeBySourceType.get( rawType );
		if ( result == null ) {
			throw log.unableToResolveDefaultValueBridgeFromSourceType( rawType );
		}
		return result;
	}

	private <T> void addDefaultIdentifierBridge(Class<T> type, BridgeBuilder<? extends IdentifierBridge<T>> builder) {
		defaultIdentifierBridgeBySourceType.put( type, builder );
	}

	private <T> void addDefaultValueBridge(Class<T> type, BridgeBuilder<? extends ValueBridge<? super T, ?>> builder) {
		defaultValueBridgeBySourceType.put( type, builder );
	}

}
