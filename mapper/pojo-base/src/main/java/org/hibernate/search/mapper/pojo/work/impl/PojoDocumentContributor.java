/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReferenceFactoryDelegate;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @param <E> The entity type mapped to the index.
 */
public final class PojoDocumentContributor<E> implements DocumentContributor {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoRawTypeIdentifier<E> typeIdentifier;
	private final String entityName;
	private final PojoIndexingProcessor<E> processor;

	private final PojoWorkSessionContext sessionContext;
	private final PojoIndexingProcessorRootContext processorContext;

	private final Object identifier;
	private final Supplier<E> entitySupplier;

	public PojoDocumentContributor(PojoRawTypeIdentifier<E> typeIdentifier, String entityName,
			PojoIndexingProcessor<E> processor,
			PojoWorkSessionContext sessionContext, PojoIndexingProcessorRootContext processorContext,
			Object identifier, Supplier<E> entitySupplier) {
		this.typeIdentifier = typeIdentifier;
		this.entityName = entityName;
		this.processor = processor;
		this.sessionContext = sessionContext;
		this.processorContext = processorContext;
		this.identifier = identifier;
		this.entitySupplier = entitySupplier;
	}

	@Override
	public void contribute(DocumentElement state) {
		try {
			processor.process( state, entitySupplier.get(), processorContext );
		}
		catch (RuntimeException e) {
			PojoEntityReferenceFactoryDelegate entityReferenceFactoryDelegate =
					sessionContext.mappingContext().entityReferenceFactoryDelegate();
			EntityReference entityReference = entityReferenceFactoryDelegate.create( typeIdentifier, entityName,
					identifier );
			throw log.errorBuildingDocument( entityReference, e.getMessage(), e );
		}
	}
}
