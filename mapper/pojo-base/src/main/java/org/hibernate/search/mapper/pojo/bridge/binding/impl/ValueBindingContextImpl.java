/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.PojoValueBridgeDocumentValueConverter;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelValueElement;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.AbstractCloser;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.impl.GenericTypeContext;

public class ValueBindingContextImpl<V> extends AbstractBindingContext
		implements ValueBindingContext<V> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoBootstrapIntrospector introspector;

	private final PojoTypeModel<V> valueTypeModel;
	private final boolean multiValued;
	private final PojoModelValue<V> bridgedElement;

	private final IndexFieldTypeFactory indexFieldTypeFactory;
	private final PojoIndexSchemaContributionListener listener;
	private final IndexSchemaElement schemaElement;
	private final String relativeFieldName;
	private final FieldModelContributor contributor;

	private PartialBinding<V, ?> partialBinding;

	public ValueBindingContextImpl(BeanResolver beanResolver,
			PojoBootstrapIntrospector introspector,
			PojoTypeModel<V> valueTypeModel, boolean multiValued,
			IndexBindingContext indexBindingContext,
			IndexFieldTypeDefaultsProvider defaultsProvider,
			String relativeFieldName, FieldModelContributor contributor,
			Map<String, Object> params) {
		super( beanResolver, params );
		this.introspector = introspector;
		this.valueTypeModel = valueTypeModel;
		this.multiValued = multiValued;
		this.bridgedElement = new PojoModelValueElement<>( introspector, valueTypeModel );

		this.indexFieldTypeFactory = indexBindingContext.createTypeFactory( defaultsProvider );
		this.listener = new PojoIndexSchemaContributionListener();
		this.schemaElement = indexBindingContext.schemaElement( listener );
		this.relativeFieldName = relativeFieldName;
		this.contributor = contributor;
	}

	@Override
	public <V2, F> void bridge(Class<V2> expectedValueType, ValueBridge<V2, F> bridge) {
		bridge( expectedValueType, bridge, null );
	}

	@Override
	public <V2, F> void bridge(Class<V2> expectedValueType, ValueBridge<V2, F> bridge,
			IndexFieldTypeOptionsStep<?, F> fieldTypeOptionsStep) {
		bridge( expectedValueType, BeanHolder.of( bridge ), fieldTypeOptionsStep );
	}

	@Override
	public <V2, F> void bridge(Class<V2> expectedValueType, BeanHolder<? extends ValueBridge<V2, F>> bridgeHolder,
			IndexFieldTypeOptionsStep<?, F> fieldTypeOptionsStep) {
		try {
			PojoRawTypeModel<V2> expectedValueTypeModel = introspector.typeModel( expectedValueType );
			if ( !valueTypeModel.rawType().isSubTypeOf( expectedValueTypeModel ) ) {
				throw log.invalidInputTypeForBridge( bridgeHolder.get(), valueTypeModel, expectedValueTypeModel );
			}

			IndexFieldReference<F> indexFieldReference = createFieldReference(
					expectedValueType, bridgeHolder.get(), fieldTypeOptionsStep
			);

			@SuppressWarnings("unchecked") // We check that V extends V2 explicitly using reflection (see above)
			BeanHolder<? extends ValueBridge<? super V, F>> castedBridgeHolder =
					(BeanHolder<? extends ValueBridge<? super V, F>>) bridgeHolder;

			this.partialBinding = new PartialBinding<>( castedBridgeHolder, indexFieldReference );
		}
		catch (RuntimeException e) {
			abortBridge( new SuppressingCloser( e ), bridgeHolder );
			throw e;
		}
	}

	@Override
	public PojoModelValue<V> bridgedElement() {
		return bridgedElement;
	}

	@Override
	public IndexFieldTypeFactory typeFactory() {
		return indexFieldTypeFactory;
	}

	public Optional<BoundValueBridge<V, ?>> applyBinder(ValueBinder binder) {
		try {
			// This call should set the partial binding
			binder.bind( this );
			if ( partialBinding == null ) {
				throw log.missingBridgeForBinder( binder );
			}

			// If all fields are filtered out, we should ignore the bridge
			if ( !listener.isAnySchemaContributed() ) {
				try ( Closer<RuntimeException> closer = new Closer<>() ) {
					partialBinding.abort( closer );
				}
				return Optional.empty();
			}

			return Optional.of( partialBinding.complete() );
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

	private <V2, F> IndexFieldReference<F> createFieldReference(Class<V2> expectedValueType,
			ValueBridge<V2, F> bridge, IndexFieldTypeOptionsStep<?, F> fieldTypeOptionsStep) {
		// If the bridge did not contribute anything, infer the field type using reflection on the bridge
		if ( fieldTypeOptionsStep == null ) {
			fieldTypeOptionsStep = inferFieldType( bridge );
		}

		PojoValueBridgeDocumentValueConverter<V2, F> converter = new PojoValueBridgeDocumentValueConverter<>( bridge );

		// Then register the bridge itself as a converter to use in the DSL
		fieldTypeOptionsStep.dslConverter( expectedValueType, converter );

		// Then register the bridge itself as a converter to use in projections
		fieldTypeOptionsStep.projectionConverter( expectedValueType, converter );

		// Then give the mapping a chance to override some of the model (make projectable, ...)
		contributor.contribute(
				new FieldModelContributorContextImpl<>( bridge, fieldTypeOptionsStep )
		);

		// Finally, create the field
		IndexSchemaFieldOptionsStep<?, ? extends IndexFieldReference<F>> fieldContext =
				schemaElement.field( relativeFieldName, fieldTypeOptionsStep );
		if ( multiValued ) {
			fieldContext.multiValued();
		}
		return fieldContext.toReference();
	}

	@SuppressWarnings( "unchecked" ) // We ensure this cast is safe through reflection
	private <F> IndexFieldTypeOptionsStep<?, F> inferFieldType(ValueBridge<?, F> bridge) {
		GenericTypeContext bridgeTypeContext = new GenericTypeContext( bridge.getClass() );
		Type typeArgument = bridgeTypeContext.resolveTypeArgument( ValueBridge.class, 1 )
				.orElseThrow( () -> new AssertionFailure( "Could not auto-detect the return type for value bridge '"
						+ bridge + "'." ) );
		if ( typeArgument instanceof Class ) {
			return indexFieldTypeFactory.as( (Class<F>) typeArgument );
		}
		else {
			throw log.invalidGenericParameterToInferFieldType( bridge, typeArgument );
		}
	}

	private static void abortBridge(AbstractCloser<?, ?> closer, BeanHolder<? extends ValueBridge<?, ?>> bridgeHolder) {
		closer.push( ValueBridge::close, bridgeHolder, BeanHolder::get );
		closer.push( BeanHolder::close, bridgeHolder );
	}

	private static class PartialBinding<V, F> {
		private final BeanHolder<? extends ValueBridge<? super V, F>> bridgeHolder;
		private final IndexFieldReference<F> indexFieldReference;

		private PartialBinding(BeanHolder<? extends ValueBridge<? super V, F>> bridgeHolder,
				IndexFieldReference<F> indexFieldReference) {
			this.bridgeHolder = bridgeHolder;
			this.indexFieldReference = indexFieldReference;
		}

		void abort(AbstractCloser<?, ?> closer) {
			abortBridge( closer, bridgeHolder );
		}

		BoundValueBridge<V, F> complete() {
			// Nothing specific to do in the case of value bridges
			return new BoundValueBridge<>( bridgeHolder, indexFieldReference );
		}
	}
}
