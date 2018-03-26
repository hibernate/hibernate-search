/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.function.Supplier;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.index.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBuilder;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSessionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.util.impl.common.Closer;
import org.hibernate.search.util.impl.common.ToStringTreeAppendable;
import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

public class PojoIndexedTypeManager<I, E, D extends DocumentElement> implements AutoCloseable, ToStringTreeAppendable {

	private final Class<E> indexedJavaClass;
	private final PojoCaster<E> caster;
	private final IdentifierMapping<I, E> identifierMapping;
	private final RoutingKeyProvider<E> routingKeyProvider;
	private final PojoIndexingProcessor<E> processor;
	private final IndexManager<D> indexManager;

	public PojoIndexedTypeManager(Class<E> indexedJavaClass,
			PojoCaster<E> caster,
			IdentifierMapping<I, E> identifierMapping,
			RoutingKeyProvider<E> routingKeyProvider,
			PojoIndexingProcessor<E> processor, IndexManager<D> indexManager) {
		this.indexedJavaClass = indexedJavaClass;
		this.caster = caster;
		this.identifierMapping = identifierMapping;
		this.routingKeyProvider = routingKeyProvider;
		this.processor = processor;
		this.indexManager = indexManager;
	}

	@Override
	public String toString() {
		return new ToStringTreeBuilder().value( this ).toString();
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( identifierMapping::close );
			closer.push( routingKeyProvider::close );
			closer.push( processor::close );
			closer.push( indexManager::close );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "indexedJavaClass", indexedJavaClass )
				.attribute( "indexManager", indexManager )
				.attribute( "identifierMapping", identifierMapping )
				.attribute( "routingKeyProvider", routingKeyProvider )
				.attribute( "processor", processor );
	}

	IdentifierMapping<I, E> getIdentifierMapping() {
		return identifierMapping;
	}

	Class<E> getIndexedJavaClass() {
		return indexedJavaClass;
	}

	Supplier<E> toEntitySupplier(PojoSessionContext sessionContext, Object entity) {
		PojoRuntimeIntrospector proxyIntrospector = sessionContext.getRuntimeIntrospector();
		return new CachingCastingEntitySupplier<>( caster, proxyIntrospector, entity );
	}

	DocumentReferenceProvider toDocumentReferenceProvider(PojoSessionContext sessionContext,
			I identifier, Supplier<E> entitySupplier) {
		String tenantId = sessionContext.getTenantIdentifier();
		String documentIdentifier = identifierMapping.toDocumentIdentifier( identifier );
		return new PojoDocumentReferenceProvider<>( routingKeyProvider, tenantId, identifier, documentIdentifier, entitySupplier );
	}

	PojoDocumentContributor<D, E> toDocumentContributor(Supplier<E> entitySupplier) {
		return new PojoDocumentContributor<>( processor, entitySupplier );
	}

	ChangesetPojoIndexedTypeWorker<I, E, D> createWorker(PojoSessionContext sessionContext) {
		return new ChangesetPojoIndexedTypeWorker<>(
				this, sessionContext, indexManager.createWorker( sessionContext )
		);
	}

	StreamPojoIndexedTypeWorker<I, E, D> createStreamWorker(PojoSessionContext sessionContext) {
		return new StreamPojoIndexedTypeWorker<>( this, sessionContext, indexManager.createStreamWorker( sessionContext ) );
	}

	IndexSearchTargetBuilder createSearchTarget() {
		return indexManager.createSearchTarget();
	}

	void addToSearchTarget(IndexSearchTargetBuilder builder) {
		indexManager.addToSearchTarget( builder );
	}
}
