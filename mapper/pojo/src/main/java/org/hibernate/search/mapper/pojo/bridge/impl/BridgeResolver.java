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

import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultIntegerFunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultIntegerIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultLocalDateFunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultStringFunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.spi.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public final class BridgeResolver {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<Class<?>, BridgeBuilder<? extends IdentifierBridge<?>>> defaultIdentifierBridgeBySourceType = new HashMap<>();
	private final Map<Class<?>, BridgeBuilder<? extends FunctionBridge<?, ?>>> defaultFunctionBridgeBySourceType = new HashMap<>();

	public BridgeResolver() {
		// TODO add an extension point to override these maps, or at least to add defaults for other types
		// TODO add defaults for other types (byte, char, Characeter, Double, double, boolean, Boolean, ...)

		addDefaultIdentifierBridge( Integer.class, ignored -> new DefaultIntegerIdentifierBridge() );

		addDefaultFunctionBridge( Integer.class, ignored -> new DefaultIntegerFunctionBridge() );
		addDefaultFunctionBridge( String.class, ignored -> new DefaultStringFunctionBridge() );
		addDefaultFunctionBridge( LocalDate.class, ignored -> new DefaultLocalDateFunctionBridge() );
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

	public BridgeBuilder<? extends FunctionBridge<?, ?>> resolveFunctionBridgeForType(PojoTypeModel<?> sourceType) {
		// TODO handle non-raw types?
		Class<?> rawType = sourceType.getRawType().getJavaClass();
		@SuppressWarnings("unchecked")
		BridgeBuilder<? extends FunctionBridge<?, ?>> result = defaultFunctionBridgeBySourceType.get( rawType );
		if ( result == null ) {
			throw log.unableToResolveDefaultFunctionBridgeFromSourceType( rawType );
		}
		return result;
	}

	private <T> void addDefaultIdentifierBridge(Class<T> type, BridgeBuilder<? extends IdentifierBridge<T>> builder) {
		defaultIdentifierBridgeBySourceType.put( type, builder );
	}

	private <T> void addDefaultFunctionBridge(Class<T> type, BridgeBuilder<? extends FunctionBridge<? super T, ?>> builder) {
		defaultFunctionBridgeBySourceType.put( type, builder );
	}

}
