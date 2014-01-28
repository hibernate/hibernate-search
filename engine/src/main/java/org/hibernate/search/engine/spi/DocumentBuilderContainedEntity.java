/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
public class DocumentBuilderContainedEntity<T> extends AbstractDocumentBuilder<T> {
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
	public void addWorkToQueue(Class<T> entityClass,
			T entity,
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
