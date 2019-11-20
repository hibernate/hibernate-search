/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to the index.
 * @param <D> The document type for the index.
 */
public interface PojoWorkIndexedTypeContext<I, E, D extends DocumentElement> {

	PojoRawTypeIdentifier<E> getTypeIdentifier();

	IdentifierMappingImplementor<I, E> getIdentifierMapping();

	Supplier<E> toEntitySupplier(AbstractPojoBackendSessionContext sessionContext, Object entity);

	DocumentReferenceProvider toDocumentReferenceProvider(AbstractPojoBackendSessionContext sessionContext,
			I identifier, Supplier<E> entitySupplier);

	PojoDocumentContributor<D, E> toDocumentContributor(Supplier<E> entitySupplier,
			AbstractPojoBackendSessionContext sessionContext);

	boolean requiresSelfReindexing(Set<String> dirtyPaths);

	void resolveEntitiesToReindex(PojoReindexingCollector collector, PojoRuntimeIntrospector runtimeIntrospector,
			Supplier<E> entitySupplier, Set<String> dirtyPaths);

	PojoIndexedTypeIndexingPlan<I, E, D> createIndexingPlan(AbstractPojoBackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

	PojoTypeIndexer<I, E, D> createIndexer(
			AbstractPojoBackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy);

	IndexWorkspace createWorkspace(DetachedBackendSessionContext sessionContext);

}
