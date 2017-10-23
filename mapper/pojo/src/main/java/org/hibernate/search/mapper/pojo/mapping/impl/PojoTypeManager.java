/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import org.hibernate.search.engine.backend.document.spi.DocumentContributor;
import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.index.spi.SearchTarget;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSessionContext;
import org.hibernate.search.mapper.pojo.processing.impl.IdentifierConverter;
import org.hibernate.search.mapper.pojo.processing.impl.PojoTypeNodeProcessor;

/**
 * @author Yoann Rodiere
 */
public class PojoTypeManager<I, E, D extends DocumentState> {

	private final IdentifierConverter<I, E> identifierConverter;
	private final Class<E> entityType;
	private final PojoTypeNodeProcessor processor;
	private final IndexManager<D> indexManager;

	public PojoTypeManager(IdentifierConverter<I, E> identifierMapping, Class<E> entityType,
			PojoTypeNodeProcessor processor, IndexManager<D> indexManager) {
		this.identifierConverter = identifierMapping;
		this.entityType = entityType;
		this.processor = processor;
		this.indexManager = indexManager;
	}

	public IdentifierConverter<I, E> getIdentifierMapping() {
		return identifierConverter;
	}

	public Class<E> getEntityType() {
		return entityType;
	}

	public String toDocumentIdentifier(PojoSessionContext sessionContext, Object providedId, Object entity) {
		Object unproxied = sessionContext.getProxyIntrospector().unproxy( entity ); // TODO Move this to the ID converter? See HibernateOrmMapper, item 4: we don't want to unproxy needlessly.
		return identifierConverter.toDocumentId( providedId, entityType.cast( unproxied ) );
	}

	public DocumentContributor<D> toDocumentContributor(PojoSessionContext sessionContext, Object entity) {
		Object unproxied = sessionContext.getProxyIntrospector().unproxy( entity );
		return state -> processor.process( entityType.cast( unproxied ), state );
	}

	public ChangesetPojoTypeWorker<D> createWorker(PojoSessionContext sessionContext) {
		return new ChangesetPojoTypeWorker<>( this, sessionContext, indexManager.createWorker( sessionContext ) );
	}

	public StreamPojoTypeWorker<D> createStreamWorker(PojoSessionContext sessionContext) {
		return new StreamPojoTypeWorker<>( this, sessionContext, indexManager.createStreamWorker( sessionContext ) );
	}

	public SearchTarget createSearchTarget() {
		return indexManager.createSearchTarget();
	}

}
