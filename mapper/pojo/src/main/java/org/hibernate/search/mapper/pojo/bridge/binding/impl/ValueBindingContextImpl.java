/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBridgeBuilder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelValueElement;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.AbstractCloser;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.impl.GenericTypeContext;
import org.hibernate.search.util.common.reflect.impl.ReflectionUtils;

public class ValueBindingContextImpl<V> extends AbstractBindingContext
		implements ValueBindingContext<V> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoGenericTypeModel<V> valueTypeModel;
	private final boolean multiValued;
	private final PojoModelValue<V> bridgedElement;

	private final IndexFieldTypeFactory indexFieldTypeFactory;
	private final PojoIndexSchemaContributionListener listener;
	private final IndexSchemaElement schemaElement;
	private final String relativeFieldName;
	private final FieldModelContributor contributor;

	private PartialBinding<V, ?> partialBinding;

	public ValueBindingContextImpl(BeanResolver beanResolver,
			PojoGenericTypeModel<V> valueTypeModel, boolean multiValued,
			IndexBindingContext indexBindingContext,
			IndexFieldTypeDefaultsProvider defaultsProvider,
			String relativeFieldName, FieldModelContributor contributor) {
		super( beanResolver );
		this.valueTypeModel = valueTypeModel;
		this.multiValued = multiValued;
		this.bridgedElement = new PojoModelValueElement<>( valueTypeModel );

		this.indexFieldTypeFactory = indexBindingContext.createTypeFactory( defaultsProvider );
		this.listener = new PojoIndexSchemaContributionListener();
		this.schemaElement = indexBindingContext.getSchemaElement( listener );
		this.relativeFieldName = relativeFieldName;
		this.contributor = contributor;
	}

	@Override
	public <V2, F> void setBridge(Class<V2> expectedValueType, ValueBridge<V2, F> bridge) {
		setBridge( expectedValueType, bridge, null );
	}

	@Override
	public <V2, F> void setBridge(Class<V2> expectedValueType, ValueBridge<V2, F> bridge,
			StandardIndexFieldTypeOptionsStep<?, F> fieldTypeOptionsStep) {
		setBridge( expectedValueType, BeanHolder.of( bridge ), fieldTypeOptionsStep );
	}

	@Override
	public <V2, F> void setBridge(Class<V2> expectedValueType, BeanHolder<? extends ValueBridge<V2, F>> bridgeHolder,
			StandardIndexFieldTypeOptionsStep<?, F> fieldTypeOptionsStep) {
		try {
			// TODO HSEARCH-3243 perform more precise checks, we're just comparing raw types here and we might miss some type errors
			if ( !valueTypeModel.getRawType().isSubTypeOf( expectedValueType ) ) {
				throw log.invalidInputTypeForBridge( bridgeHolder.get(), valueTypeModel );
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
	public PojoModelValue<V> getBridgedElement() {
		return bridgedElement;
	}

	@Override
	public IndexFieldTypeFactory getTypeFactory() {
		return indexFieldTypeFactory;
	}

	public Optional<BoundValueBridge<V, ?>> applyBuilder(ValueBridgeBuilder bridgeBuilder) {
		try {
			// This call should set the partial binding
			bridgeBuilder.bind( this );
			if ( partialBinding == null ) {
				throw log.missingBridgeForBridgeBuilder( bridgeBuilder );
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
			ValueBridge<V2, F> bridge, StandardIndexFieldTypeOptionsStep<?, F> fieldTypeOptionsStep) {
		// If the bridge did not contribute anything, infer the field type using reflection on the bridge
		if ( fieldTypeOptionsStep == null ) {
			fieldTypeOptionsStep = inferFieldType( bridge );
		}

		// Then register the bridge itself as a converter to use in the DSL
		fieldTypeOptionsStep.dslConverter(
				new PojoValueBridgeToDocumentFieldValueConverter<>( bridge )
		);

		// Then register the bridge itself as a converter to use in projections
		fieldTypeOptionsStep.projectionConverter(
				new PojoValueBridgeFromDocumentFieldValueConverter<>( bridge, expectedValueType )
		);

		// Then give the mapping a chance to override some of the model (add storage, ...)
		contributor.contribute(
				fieldTypeOptionsStep,
				new FieldModelContributorBridgeContextImpl<>( bridge, fieldTypeOptionsStep )
		);

		// Finally, create the field
		IndexSchemaFieldOptionsStep<?, ? extends IndexFieldReference<F>> fieldContext =
				schemaElement.field( relativeFieldName, fieldTypeOptionsStep );
		if ( multiValued ) {
			fieldContext.multiValued();
		}
		return fieldContext.toReference();
	}

	private <F> StandardIndexFieldTypeOptionsStep<?, F> inferFieldType(ValueBridge<?, F> bridge) {
		GenericTypeContext bridgeTypeContext = new GenericTypeContext( bridge.getClass() );
		// TODO HSEARCH-3243 We're assuming the field type is raw here, maybe we should enforce it?
		@SuppressWarnings( "unchecked" ) // We ensure this cast is safe through reflection
		Class<F> returnType =
				(Class<F>) bridgeTypeContext.resolveTypeArgument( ValueBridge.class, 1 )
						.map( ReflectionUtils::getRawType )
						.orElseThrow( () -> new AssertionFailure(
								"Could not auto-detect the return type for value bridge '"
										+ bridge + "'."
										+ " There is a bug in Hibernate Search, please report it."
						) );
		return indexFieldTypeFactory.as( returnType );
	}

	private static void abortBridge(AbstractCloser<?, ?> closer, BeanHolder<? extends ValueBridge<?, ?>> bridgeHolder) {
		closer.push( holder -> holder.get().close(), bridgeHolder );
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
