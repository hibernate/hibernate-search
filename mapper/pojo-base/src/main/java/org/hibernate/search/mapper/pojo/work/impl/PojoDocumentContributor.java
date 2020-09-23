/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @param <E> The entity type mapped to the index.
 */
public final class PojoDocumentContributor<E> implements DocumentContributor {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String entityName;
	private final PojoIndexingProcessor<E> processor;

	private final PojoWorkSessionContext<?> sessionContext;

	private final Object identifier;
	private final Supplier<E> entitySupplier;

	public PojoDocumentContributor(String entityName, PojoIndexingProcessor<E> processor,
			PojoWorkSessionContext<?> sessionContext, Object identifier, Supplier<E> entitySupplier) {
		this.entityName = entityName;
		this.processor = processor;
		this.sessionContext = sessionContext;
		this.identifier = identifier;
		this.entitySupplier = entitySupplier;
	}

	@Override
	public void contribute(DocumentElement state) {
		try {
			processor.process( state, entitySupplier.get(), sessionContext );
		}
		catch (RuntimeException e) {
			Object entityReference = EntityReferenceFactory.safeCreateEntityReference(
					sessionContext.entityReferenceFactory(), entityName, identifier, e::addSuppressed );
			throw log.errorBuildingDocument( entityReference, e.getMessage(), e );
		}
	}
}
