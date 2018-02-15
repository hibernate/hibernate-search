/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.function.Supplier;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.index.spi.DocumentContributor;
import org.hibernate.search.engine.backend.index.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBuilder;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSessionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoIndexableTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoProxyIntrospector;
import org.hibernate.search.mapper.pojo.processing.impl.IdentifierMapping;
import org.hibernate.search.mapper.pojo.processing.impl.PojoTypeNodeProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.RoutingKeyProvider;

/**
 * @author Yoann Rodiere
 */
public class PojoTypeManager<I, E, D extends DocumentElement> {

	private final PojoIndexableTypeModel<E> typeModel;
	private final IdentifierMapping<I, E> identifierMapping;
	private final RoutingKeyProvider<E> routingKeyProvider;
	private final PojoTypeNodeProcessor<E> processor;
	private final IndexManager<D> indexManager;

	public PojoTypeManager(PojoIndexableTypeModel<E> typeModel,
			IdentifierMapping<I, E> identifierMapping,
			RoutingKeyProvider<E> routingKeyProvider,
			PojoTypeNodeProcessor<E> processor, IndexManager<D> indexManager) {
		this.typeModel = typeModel;
		this.identifierMapping = identifierMapping;
		this.routingKeyProvider = routingKeyProvider;
		this.processor = processor;
		this.indexManager = indexManager;
	}

	public IdentifierMapping<I, E> getIdentifierMapping() {
		return identifierMapping;
	}

	public PojoIndexableTypeModel<E> getTypeModel() {
		return typeModel;
	}

	public Supplier<E> toEntitySupplier(PojoSessionContext sessionContext, Object entity) {
		PojoProxyIntrospector proxyIntrospector = sessionContext.getProxyIntrospector();
		return new CachingCastingEntitySupplier<>( typeModel, proxyIntrospector, entity );
	}

	public DocumentReferenceProvider toDocumentReferenceProvider(PojoSessionContext sessionContext,
			Object providedId, Supplier<E> entitySupplier) {
		String tenantId = sessionContext.getTenantIdentifier();
		I identifier = identifierMapping.getIdentifier( providedId, entitySupplier );
		String documentIdentifier = identifierMapping.toDocumentIdentifier( identifier );
		return new PojoDocumentReferenceProvider<>( routingKeyProvider, tenantId, identifier, documentIdentifier, entitySupplier );
	}

	public DocumentContributor<D> toDocumentContributor(Supplier<E> entitySupplier) {
		return new PojoDocumentContributor<>( processor, entitySupplier );
	}

	public ChangesetPojoTypeWorker<D, E> createWorker(PojoSessionContext sessionContext) {
		return new ChangesetPojoTypeWorker<>( this, sessionContext, indexManager.createWorker( sessionContext ) );
	}

	public StreamPojoTypeWorker<D, E> createStreamWorker(PojoSessionContext sessionContext) {
		return new StreamPojoTypeWorker<>( this, sessionContext, indexManager.createStreamWorker( sessionContext ) );
	}

	public IndexSearchTargetBuilder createSearchTarget() {
		return indexManager.createSearchTarget();
	}

	public void addToSearchTarget(IndexSearchTargetBuilder builder) {
		indexManager.addToSearchTarget( builder );
	}

}
