/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 *  Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 *  indicated by the @author tags or express copyright attribution
 *  statements applied by the authors.  All third-party contributions are
 *  distributed under license by Red Hat, Inc.
 *
 *  This copyrighted material is made available to anyone wishing to use, modify,
 *  copy, or redistribute it subject to the terms and conditions of the GNU
 *  Lesser General Public License, as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this distribution; if not, write to:
 *  Free Software Foundation, Inc.
 *  51 Franklin Street, Fifth Floor
 *  Boston, MA  02110-1301  USA
 */
package org.hibernate.search.engine;

import java.io.Serializable;
import java.util.List;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.impl.ConfigContext;

/**
 * Set up and provide a manager for classes which are indexed via <code>@IndexedEmbedded</code>, but themselves do not
 * contain the <code>@Indexed</code> annotation.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 * @author Richard Hallier
 * @author Hardy Ferentschik
 */
public class DocumentBuilderContainedEntity<T> extends AbstractDocumentBuilder<T> implements DocumentBuilder {
	/**
	 * Constructor used on contained entities not annotated with <code>@Indexed</code> themselves.
	 *
	 * @param xClass The class for which to build a <code>DocumentBuilderContainedEntity</code>.
	 * @param context Handle to default configuration settings.
	 * @param reflectionManager Reflection manager to use for processing the annotations.
	 */
	public DocumentBuilderContainedEntity(XClass xClass, ConfigContext context, ReflectionManager reflectionManager) {
		super( xClass, context, reflectionManager );
	}

	protected void initSubClass(XClass clazz, ConfigContext context) {	
	}

	protected void subClassSpecificCheck(XProperty member, PropertiesMetadata propertiesMetadata, boolean isRoot, String prefix, ConfigContext context) {
	}

	public void addWorkToQueue(Class<T> entityClass, T entity, Serializable id, WorkType workType, List<LuceneWork> queue, SearchFactoryImplementor searchFactoryImplementor) {
		/**
		 * When references are changed, either null or another one, we expect dirty checking to be triggered (both sides
		 * have to be updated)
		 * When the internal object is changed, we apply the {Add|Update}Work on containedIns
		 */
		if ( workType.searchForContainers() ) {
			processContainedInInstances( entity, queue, metadata, searchFactoryImplementor );
		}
	}
}
