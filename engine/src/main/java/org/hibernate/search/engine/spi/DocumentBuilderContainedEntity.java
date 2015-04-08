/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.spi;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.spi.InstanceInitializer;

/**
 * Set up and provide a manager for classes which are indexed via {@code @IndexedEmbedded}, but themselves do not
 * contain the {@code @Indexed} annotation.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 * @author Richard Hallier
 * @author Hardy Ferentschik
 */
public class DocumentBuilderContainedEntity extends AbstractDocumentBuilder {
	/**
	 * Constructor used on contained entities not annotated with {@code @Indexed} themselves.
	 *
	 * @param xClass The class for which to build a {@code DocumentBuilderContainedEntity}
	 * @param typeMetadata metadata for the given type
	 * @param reflectionManager Reflection manager to use for processing the annotations
	 * @param optimizationBlackList mutable register, keeps track of types on which we need to disable collection events optimizations
	 * @param instanceInitializer a {@link org.hibernate.search.spi.InstanceInitializer} object
	 */
	public DocumentBuilderContainedEntity(XClass xClass,
			TypeMetadata typeMetadata,
			ReflectionManager reflectionManager,
			Set<XClass> optimizationBlackList,
			InstanceInitializer instanceInitializer) {
		super( xClass, typeMetadata, reflectionManager, optimizationBlackList, instanceInitializer );

		//done after init:
		if ( getTypeMetadata().getContainedInMetadata().isEmpty() ) {
			this.entityState = EntityState.NON_INDEXABLE;
		}
	}

	@Override
	public void addWorkToQueue(
			String tenantId,
			Class<?> entityClass,
			Object entity,
			Serializable id,
			boolean delete,
			boolean add,
			List<LuceneWork> queue,
			ConversionContext contextualBridge) {
		// nothing to do
	}

	@Override
	public Serializable getId(Object entity) {
		//this is not an indexed entity
		return null;
	}
}
