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
import org.hibernate.search.engine.backend.document.IndexFilterReference;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.engine.search.predicate.factories.FilterFactory;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.util.common.impl.AbstractCloser;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.FilterBinder;
import org.hibernate.search.mapper.pojo.bridge.binding.FilterBindingContext;

public class FilterBindingContextImpl<T extends FilterFactory> extends AbstractBindingContext
	implements FilterBindingContext<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoBootstrapIntrospector introspector;
	private final IndexSchemaElement schemaElement;
	private final String name;

	private PartialBinding<T> partialBinding;

	public FilterBindingContextImpl(BeanResolver beanResolver,
		PojoBootstrapIntrospector introspector,
		IndexBindingContext indexBindingContext,
		String name) {
		super( beanResolver );
		this.introspector = introspector;
		this.name = name;

		this.schemaElement = indexBindingContext.getSchemaElement();
	}

	@Override
	public void setFactory(T factory, Map<String, Object> params) {
		setFactory( BeanHolder.of( factory ), params );
	}

	@Override
	public void setFactory(BeanHolder<T> factoryHolder, Map<String, Object> params) {
		try {
			IndexFilterReference<T> indexFilterReference = schemaElement
				.filter( name, factoryHolder.get() )
				.params( params ).toReference();

			this.partialBinding = new PartialBinding<>( factoryHolder,
				indexFilterReference );
		}
		catch (RuntimeException e) {
			abortFactory( new SuppressingCloser( e ), factoryHolder );
			throw e;
		}
	}

	private static void abortFactory(AbstractCloser<?, ?> closer, BeanHolder<? extends FilterFactory> factoryHolder) {
		closer.push( BeanHolder::close, factoryHolder );
	}

	public Optional<BoundFilterBridge<T>> applyBinder(FilterBinder binder) {
		try {
			// This call should set the partial binding
			binder.bind( this );
			if ( partialBinding == null ) {
				throw log.missingFactoryForBinder( binder );
			}

			return Optional.of( partialBinding.complete( name ) );
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

	private static class PartialBinding<T extends FilterFactory> {
		private final BeanHolder<T> factoryHolder;
		private final IndexFilterReference<T> indexFilterReference;

		private PartialBinding(BeanHolder<T> bridgeHolder,
			IndexFilterReference<T> indexFilterReference) {
			this.factoryHolder = bridgeHolder;
			this.indexFilterReference = indexFilterReference;
		}

		void abort(AbstractCloser<?, ?> closer) {
			closer.push( BeanHolder::close, factoryHolder );
		}

		BoundFilterBridge<T> complete(String name) {
			return new BoundFilterBridge<>( factoryHolder, indexFilterReference );
		}
	}

}
