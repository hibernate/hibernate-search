/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.PojoIdentifierBridgeDocumentValueConverter;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelValueElement;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.impl.AbstractCloser;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class DefaultIdentifierBindingContext<I> extends AbstractBindingContext
		implements IdentifierBindingContext<I> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoBootstrapIntrospector introspector;
	private final Optional<IndexedEntityBindingContext> indexedEntityBindingContext;

	private final PojoTypeModel<I> identifierTypeModel;
	private final PojoModelValue<I> bridgedElement;

	private PartialBinding<I> partialBinding;

	public DefaultIdentifierBindingContext(BeanResolver beanResolver,
			PojoBootstrapIntrospector introspector,
			Optional<IndexedEntityBindingContext> indexedEntityBindingContext,
			PojoTypeModel<I> valueTypeModel, Map<String, Object> params) {
		super( beanResolver, params );
		this.introspector = introspector;
		this.indexedEntityBindingContext = indexedEntityBindingContext;
		this.identifierTypeModel = valueTypeModel;
		this.bridgedElement = new PojoModelValueElement<>( introspector, valueTypeModel );
	}

	@Override
	public <I2> void bridge(Class<I2> expectedValueType, IdentifierBridge<I2> bridge) {
		bridge( expectedValueType, BeanHolder.of( bridge ) );
	}

	@Override
	public <I2> void bridge(Class<I2> expectedValueType, BeanHolder<? extends IdentifierBridge<I2>> bridgeHolder) {
		try {
			PojoRawTypeModel<I2> expectedValueTypeModel = introspector.typeModel( expectedValueType );
			if ( !identifierTypeModel.rawType().equals( expectedValueTypeModel ) ) {
				throw log.invalidInputTypeForBridge( bridgeHolder.get(), identifierTypeModel, expectedValueTypeModel );
			}

			@SuppressWarnings("unchecked") // We check that I2 equals I explicitly using reflection (see above)
			BeanHolder<? extends IdentifierBridge<I>> castedBridgeHolder =
					(BeanHolder<? extends IdentifierBridge<I>>) bridgeHolder;
			@SuppressWarnings("unchecked") // We check that I2 equals I explicitly using reflection (see above)
			Class<I> castedExpectedType = (Class<I>) expectedValueType;

			this.partialBinding = new PartialBinding<>( castedBridgeHolder, castedExpectedType );
		}
		catch (RuntimeException e) {
			abortBridge( new SuppressingCloser( e ), bridgeHolder );
			throw e;
		}
	}

	@Override
	public PojoModelValue<I> bridgedElement() {
		return bridgedElement;
	}

	public BoundIdentifierBridge<I> applyBinder(IdentifierBinder binder) {
		try {
			// This call should set the partial binding
			binder.bind( this );
			if ( partialBinding == null ) {
				throw log.missingBridgeForBinder( binder );
			}

			return partialBinding.complete( indexedEntityBindingContext );
		}
		catch (RuntimeException e) {
			if ( partialBinding != null ) {
				partialBinding.abort( new SuppressingCloser( e ) );
			}
			throw e;
		}
		finally {
			partialBinding = null;
		}
	}

	private static void abortBridge(AbstractCloser<?, ?> closer, BeanHolder<? extends IdentifierBridge<?>> bridgeHolder) {
		closer.push( IdentifierBridge::close, bridgeHolder, BeanHolder::get );
		closer.push( BeanHolder::close, bridgeHolder );
	}

	private static class PartialBinding<I> {
		private final BeanHolder<? extends IdentifierBridge<I>> bridgeHolder;
		private final Class<I> expectedValueType;

		private PartialBinding(BeanHolder<? extends IdentifierBridge<I>> bridgeHolder,
				Class<I> expectedValueType) {
			this.bridgeHolder = bridgeHolder;
			this.expectedValueType = expectedValueType;
		}

		void abort(AbstractCloser<?, ?> closer) {
			abortBridge( closer, bridgeHolder );
		}

		BoundIdentifierBridge<I> complete(Optional<IndexedEntityBindingContext> indexedEntityBindingContext) {
			if ( indexedEntityBindingContext.isPresent() ) {
				PojoIdentifierBridgeDocumentValueConverter<I> converter =
						new PojoIdentifierBridgeDocumentValueConverter<>( bridgeHolder.get() );
				indexedEntityBindingContext.get().idDslConverter( expectedValueType, converter );
				indexedEntityBindingContext.get().idProjectionConverter( expectedValueType, converter );
			}

			return new BoundIdentifierBridge<>( bridgeHolder );
		}
	}
}
