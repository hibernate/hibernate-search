/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.impl;

import org.hibernate.search.engine.impl.MutableEntityIndexBinding;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * Manages the binding of entities to index, i.e. initializes the environment
 * when a new entity type is added to an {@link IndexManagerGroupHolder}.
 *
 * @author Yoann Rodiere
 */
interface EntityIndexBinder {

	MutableEntityIndexBinding bind(IndexManagerGroupHolder holder, Class<?> entityType,
			EntityIndexingInterceptor<?> interceptor, WorkerBuildContext buildContext);

}
