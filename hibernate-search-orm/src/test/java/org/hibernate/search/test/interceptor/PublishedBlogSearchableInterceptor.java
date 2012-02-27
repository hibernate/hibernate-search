/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.interceptor;

import org.hibernate.search.interceptor.indexingaction.EntityIndexingInterceptor;
import org.hibernate.search.interceptor.indexingaction.IndexingOperationType;

/**
 * Only index when it's published
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class PublishedBlogSearchableInterceptor implements EntityIndexingInterceptor<Blog> {
	@Override
	public IndexingOperationType onAdd(Blog entity) {
		if (entity.getStatus() == BlogStatus.PUBLISHED) {
			return IndexingOperationType.UNCHANGED;
		}
		return IndexingOperationType.SKIP;
	}

	@Override
	public IndexingOperationType onUpdate(Blog entity) {
		if (entity.getStatus() == BlogStatus.PUBLISHED) {
			return IndexingOperationType.UPDATE;
		}
		return IndexingOperationType.REMOVE;
	}

	@Override
	public IndexingOperationType onDelete(Blog entity) {
		return IndexingOperationType.UNCHANGED;
	}

	@Override
	public IndexingOperationType onCollectionUpdate(Blog entity) {
		return onUpdate(entity);
	}
}
