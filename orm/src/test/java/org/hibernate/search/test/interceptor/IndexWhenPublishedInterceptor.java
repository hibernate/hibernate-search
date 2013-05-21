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

import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;

/**
 * Only index blog when it is in published state
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class IndexWhenPublishedInterceptor implements EntityIndexingInterceptor<Blog> {
	@Override
	public IndexingOverride onAdd(Blog entity) {
		if ( entity.getStatus() == BlogStatus.PUBLISHED ) {
			return IndexingOverride.APPLY_DEFAULT;
		}
		return IndexingOverride.SKIP;
	}

	@Override
	public IndexingOverride onUpdate(Blog entity) {
		if ( entity.getStatus() == BlogStatus.PUBLISHED ) {
			return IndexingOverride.UPDATE;
		}
		return IndexingOverride.REMOVE;
	}

	@Override
	public IndexingOverride onDelete(Blog entity) {
		return IndexingOverride.APPLY_DEFAULT;
	}

	@Override
	public IndexingOverride onCollectionUpdate(Blog entity) {
		return onUpdate( entity );
	}
}
