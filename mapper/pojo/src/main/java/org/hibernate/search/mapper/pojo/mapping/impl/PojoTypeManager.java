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
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoProxyIntrospector;
import org.hibernate.search.mapper.pojo.processing.impl.IdentifierConverter;
import org.hibernate.search.mapper.pojo.processing.impl.PojoTypeNodeProcessor;

/**
 * @author Yoann Rodiere
 */
public class PojoTypeManager<I, E, D extends DocumentState> {

	private final PojoProxyIntrospector proxyIntrospector;
	private final IdentifierConverter<I, E> identifierConverter;
	private final Class<E> entityType;
	private final PojoTypeNodeProcessor processor;
	private final IndexManager<D> indexManager;

	public PojoTypeManager(PojoProxyIntrospector proxyIntrospector,
			IdentifierConverter<I, E> identifierMapping, Class<E> entityType,
			PojoTypeNodeProcessor processor, IndexManager<D> indexManager) {
		this.proxyIntrospector = proxyIntrospector;
		this.identifierConverter = identifierMapping;
		this.entityType = entityType;
		this.processor = processor;
		this.indexManager = indexManager;
	}

	public IdentifierConverter<I, E> getIdentifierMapping() {
		return identifierConverter;
	}

	public String toDocumentIdentifier(Object providedId, Object entity) {
		Object unproxied = proxyIntrospector.unproxy( entity ); // TODO Move this to the ID converter? See HibernateOrmMapper, item 4: we don't want to unproxy needlessly.
		return identifierConverter.toDocumentId( providedId, entityType.cast( unproxied ) );
	}

	public DocumentContributor<D> toDocumentContributor(Object entity) {
		Object unproxied = proxyIntrospector.unproxy( entity );
		return state -> processor.process( entityType.cast( unproxied ), state );
	}

	public ChangesetPojoTypeWorker<D> createWorker(SessionContext context) {
		return new ChangesetPojoTypeWorker<>( this, indexManager.createWorker( context ) );
	}

	public StreamPojoTypeWorker<D> createStreamWorker(SessionContext context) {
		return new StreamPojoTypeWorker<>( this, indexManager.createStreamWorker( context ) );
	}

}
