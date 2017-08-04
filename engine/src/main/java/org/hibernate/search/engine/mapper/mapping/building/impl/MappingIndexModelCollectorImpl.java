/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.document.model.spi.FieldModelContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexModelCollectorImplementor;
import org.hibernate.search.engine.backend.document.model.spi.IndexModelCollector;
import org.hibernate.search.engine.backend.document.model.spi.TypedFieldModelContext;
import org.hibernate.search.engine.backend.document.spi.IndexFieldReference;
import org.hibernate.search.engine.bridge.impl.BridgeFactory;
import org.hibernate.search.engine.bridge.impl.FunctionBridgeUtil;
import org.hibernate.search.engine.bridge.mapping.BridgeDefinition;
import org.hibernate.search.engine.bridge.spi.Bridge;
import org.hibernate.search.engine.bridge.spi.FunctionBridge;
import org.hibernate.search.engine.bridge.spi.IdentifierBridge;
import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingIndexModelCollector;
import org.hibernate.search.engine.mapper.model.spi.IndexableModel;
import org.hibernate.search.engine.mapper.model.spi.IndexableReference;
import org.hibernate.search.engine.mapper.model.spi.IndexableTypeOrdering;
import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;
import org.hibernate.search.engine.mapper.processing.impl.BridgeValueProcessor;
import org.hibernate.search.engine.mapper.processing.impl.FunctionBridgeValueProcessor;
import org.hibernate.search.engine.mapper.processing.spi.ValueProcessor;
import org.hibernate.search.util.SearchException;

/**
 * @author Yoann Rodiere
 */
public class MappingIndexModelCollectorImpl implements MappingIndexModelCollector {

	private final BridgeFactory bridgeFactory;

	private final IndexModelCollectorImplementor collector;
	private final IndexModelCollector collectorWithContext;
	private final IndexModelNestingContextImpl nestingContext;

	public MappingIndexModelCollectorImpl(BridgeFactory bridgeFactory,
			IndexModelCollectorImplementor collector,
			IndexableTypeOrdering typeOrdering) {
		this( bridgeFactory, collector, new IndexModelNestingContextImpl( typeOrdering ) );
	}

	private MappingIndexModelCollectorImpl(BridgeFactory bridgeFactory,
			IndexModelCollectorImplementor collector,
			IndexModelNestingContextImpl nestingContext) {
		this.bridgeFactory = bridgeFactory;
		this.collector = collector;
		this.nestingContext = nestingContext;
		this.collectorWithContext = collector.withContext( nestingContext );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "collector=" ).append( collector )
				.append( ",nestingContext=" ).append( nestingContext )
				.append( "]" )
				.toString();
	}

	@Override
	public IdentifierBridge<?> createIdentifierBridge(BeanReference<? extends IdentifierBridge<?>> reference) {
		return bridgeFactory.createIdentifierBridge( reference );
	}

	@Override
	public ValueProcessor addBridge(IndexableModel indexableModel, BridgeDefinition<?> definition) {
		Bridge<?> bridge = bridgeFactory.createBridge( definition );

		// FIXME if all fields are filtered out, we should ignore the processor
		bridge.bind( indexableModel, collectorWithContext );

		return new BridgeValueProcessor( bridge );
	}

	@Override
	public ValueProcessor addFunctionBridge(IndexableModel indexableModel, BeanReference<? extends FunctionBridge<?, ?>> bridgeReference,
			String fieldName, FieldModelContributor contributor) {
		FunctionBridge<?, ?> bridge = bridgeFactory.createFunctionBridge( bridgeReference );
		return doAddFunctionBridge( indexableModel, bridge, fieldName, contributor );
	}

	private <T, R> ValueProcessor doAddFunctionBridge(IndexableModel indexableModel, FunctionBridge<T, R> bridge,
			String fieldName, FieldModelContributor contributor) {
		IndexableReference<? extends T> indexableReference = getReferenceForBridge( indexableModel, bridge );
		return doAddFunctionBridge( indexableReference, bridge, fieldName, contributor );
	}

	@SuppressWarnings("unchecked")
	private <T> IndexableReference<? extends T> getReferenceForBridge(IndexableModel indexableModel, FunctionBridge<T, ?> bridge) {
		return FunctionBridgeUtil.inferParameterType( bridge )
				.map( c -> indexableModel.asReference( c ) )
				.orElse( (IndexableReference<T>) indexableModel.asReference() );
	}

	private <T, R> ValueProcessor doAddFunctionBridge(IndexableReference<? extends T> indexableReference,
			FunctionBridge<T, R> bridge, String fieldName, FieldModelContributor contributor) {
		FieldModelContext fieldContext = collectorWithContext.field( fieldName );

		// First give the bridge a chance to contribute to the model
		TypedFieldModelContext<R> typedFieldContext = bridge.bind( fieldContext );
		if ( typedFieldContext == null ) {
			Class<R> returnType = FunctionBridgeUtil.inferReturnType( bridge )
					.orElseThrow( () -> new SearchException( "Could not auto-detect the return type for bridge "
							+ bridge + "; configure encoding explicitly in the bridge." ) );
			typedFieldContext = fieldContext.from( returnType );
		}
		// Then give the mapping a chance to override some of the model (add storage, ...)
		contributor.contribute( typedFieldContext );

		// FIXME if the field is filtered out, we should ignore the processor

		IndexFieldReference<R> indexFieldReference = typedFieldContext.asReference();
		return new FunctionBridgeValueProcessor<>( bridge, indexableReference, indexFieldReference );
	}

	@Override
	public Optional<MappingIndexModelCollector> addIndexedEmbeddedIfIncluded(IndexedTypeIdentifier parentTypeId,
			String relativePrefix, Integer nestedMaxDepth, Set<String> nestedPathFilters) {
		return nestingContext.addIndexedEmbeddedIfIncluded(
				relativePrefix,
				f -> f.composeWithNested( parentTypeId, relativePrefix, nestedMaxDepth, nestedPathFilters ),
				collector, IndexModelCollectorImplementor::childObject,
				(indexModelCollector, recursionContext) ->
						new MappingIndexModelCollectorImpl( bridgeFactory, indexModelCollector, recursionContext )
				);
	}

}
