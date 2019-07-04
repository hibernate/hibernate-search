/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelValueElement;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.util.common.impl.AbstractCloser;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class IdentifierBindingContextImpl<I> extends AbstractBindingContext
		implements IdentifierBindingContext<I> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexedEntityBindingContext indexedEntityBindingContext;
	private final PojoGenericTypeModel<I> identifierTypeModel;
	private final PojoModelValue<I> bridgedElement;

	private PartialBinding<I> partialBinding;

	public IdentifierBindingContextImpl(BeanResolver beanResolver,
			IndexedEntityBindingContext indexedEntityBindingContext,
			PojoGenericTypeModel<I> valueTypeModel) {
		super( beanResolver );
		this.indexedEntityBindingContext = indexedEntityBindingContext;
		this.identifierTypeModel = valueTypeModel;
		this.bridgedElement = new PojoModelValueElement<>( valueTypeModel );
	}

	@Override
	public <I2> void setBridge(Class<I2> expectedValueType, IdentifierBridge<I2> bridge) {
		setBridge( expectedValueType, BeanHolder.of( bridge ) );
	}

	@Override
	public <I2> void setBridge(Class<I2> expectedValueType, BeanHolder<? extends IdentifierBridge<I2>> bridgeHolder) {
		try {
			// TODO HSEARCH-3243 perform more precise checks, we're just comparing raw types here and we might miss some type errors
			//  Also we're checking that the bridge parameter type is a subtype, but we really should be checking it
			//  is either the same type or a generic type parameter that can represent the same type.
			if ( !identifierTypeModel.getRawType().isSubTypeOf( expectedValueType ) ) {
				throw log.invalidInputTypeForBridge( bridgeHolder.get(), identifierTypeModel );
			}

			@SuppressWarnings("unchecked") // We check that I2 equals I explicitly using reflection (see above)
			BeanHolder<? extends IdentifierBridge<I>> castedBridgeHolder =
					(BeanHolder<? extends IdentifierBridge<I>>) bridgeHolder;

			this.partialBinding = new PartialBinding<>( castedBridgeHolder );
		}
		catch (RuntimeException e) {
			abortBridge( new SuppressingCloser( e ), bridgeHolder );
			throw e;
		}
	}

	@Override
	public PojoModelValue<I> getBridgedElement() {
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
		closer.push( holder -> holder.get().close(), bridgeHolder );
		closer.push( BeanHolder::close, bridgeHolder );
	}

	private static class PartialBinding<I> {
		private final BeanHolder<? extends IdentifierBridge<I>> bridgeHolder;

		private PartialBinding(BeanHolder<? extends IdentifierBridge<I>> bridgeHolder) {
			this.bridgeHolder = bridgeHolder;
		}

		void abort(AbstractCloser<?, ?> closer) {
			abortBridge( closer, bridgeHolder );
		}

		BoundIdentifierBridge<I> complete(IndexedEntityBindingContext indexedEntityBindingContext) {
			indexedEntityBindingContext.idDslConverter(
					new PojoIdentifierBridgeToDocumentIdentifierValueConverter<>( bridgeHolder.get() )
			);

			return new BoundIdentifierBridge<>( bridgeHolder );
		}
	}
}
