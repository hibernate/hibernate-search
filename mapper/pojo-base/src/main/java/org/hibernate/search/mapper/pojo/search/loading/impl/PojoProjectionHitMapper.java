/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReferenceFactoryDelegate;
import org.hibernate.search.mapper.pojo.loading.impl.PojoLoadingPlan;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class PojoProjectionHitMapper<E> implements ProjectionHitMapper<E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<String, PojoSearchLoadingIndexedTypeContext<? extends E>> targetTypesByEntityName;
	private final PojoEntityReferenceFactoryDelegate entityReferenceFactoryDelegate;
	private final BridgeSessionContext sessionContext;
	private final PojoLoadingPlan<E> loadingPlan;

	public PojoProjectionHitMapper(Map<String, PojoSearchLoadingIndexedTypeContext<? extends E>> targetTypesByEntityName,
			PojoEntityReferenceFactoryDelegate entityReferenceFactoryDelegate,
			BridgeSessionContext sessionContext,
			PojoLoadingPlan<E> loadingPlan) {
		this.targetTypesByEntityName = targetTypesByEntityName;
		this.entityReferenceFactoryDelegate = entityReferenceFactoryDelegate;
		this.sessionContext = sessionContext;
		this.loadingPlan = loadingPlan;
	}

	@Override
	public Object planLoading(DocumentReference reference) {
		PojoSearchLoadingIndexedTypeContext<? extends E> type = toType( reference );
		Object identifier = toEntityIdentifier( type, reference );
		int ordinal = loadingPlan.planLoading( type, identifier );
		if ( targetTypesByEntityName.size() == 1 ) {
			// Optimization: take advantage of Integer unboxing cache when possible.
			// This should avoid unnecessary allocations when
			// we target a single type and there are fewer than ~128 entities to load.
			return ordinal;
		}
		else {
			return new TypeAndOrdinal<>( type, ordinal );
		}
	}

	@Override
	public LoadingResult<E> loadBlocking(Deadline deadline) {
		loadingPlan.loadBlocking( deadline );
		if ( targetTypesByEntityName.size() == 1 ) {
			// Optimization, see planLoading().
			return new SingleTypeLoadingResult( targetTypesByEntityName.values().iterator().next() );
		}
		else {
			return new MultiTypeLoadingResult();
		}
	}

	private PojoSearchLoadingIndexedTypeContext<? extends E> toType(DocumentReference reference) {
		PojoSearchLoadingIndexedTypeContext<? extends E> type = targetTypesByEntityName.get( reference.typeName() );
		if ( type == null ) {
			throw log.unexpectedEntityNameForEntityLoading( reference.typeName(), targetTypesByEntityName.keySet() );
		}
		return type;
	}

	private Object toEntityIdentifier(PojoSearchLoadingIndexedTypeContext<?> type, DocumentReference reference) {
		return type.identifierMapping().fromDocumentIdentifier( reference.id(), sessionContext );
	}

	private EntityReference toEntityReference(PojoSearchLoadingIndexedTypeContext<?> type, DocumentReference reference) {
		return entityReferenceFactoryDelegate.create( type.typeIdentifier(), type.entityName(),
				toEntityIdentifier( type, reference ) );
	}

	private class SingleTypeLoadingResult implements LoadingResult<E> {
		private final PojoSearchLoadingIndexedTypeContext<? extends E> type;

		private SingleTypeLoadingResult(PojoSearchLoadingIndexedTypeContext<? extends E> type) {
			this.type = type;
		}

		@Override
		public E get(Object key) {
			return loadingPlan.retrieve( type, (int) key );
		}

		@Override
		public EntityReference convertReference(DocumentReference reference) {
			return PojoProjectionHitMapper.this.toEntityReference( type, reference );
		}
	}

	private class MultiTypeLoadingResult implements LoadingResult<E> {
		private MultiTypeLoadingResult() {
		}

		@Override
		public E get(Object key) {
			@SuppressWarnings("unchecked")
			TypeAndOrdinal<? extends E> typeAndOrdinal = (TypeAndOrdinal<? extends E>) key;
			return loadingPlan.retrieve( typeAndOrdinal.type, typeAndOrdinal.ordinal );
		}

		@Override
		public EntityReference convertReference(DocumentReference reference) {
			return PojoProjectionHitMapper.this.toEntityReference( toType( reference ), reference );
		}
	}

	private static final class TypeAndOrdinal<E> {
		private final PojoSearchLoadingIndexedTypeContext<? extends E> type;
		private final int ordinal;

		public TypeAndOrdinal(PojoSearchLoadingIndexedTypeContext<? extends E> type, int ordinal) {
			this.type = type;
			this.ordinal = ordinal;
		}
	}
}
