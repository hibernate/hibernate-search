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
package org.hibernate.search.backend;

import java.util.Set;

import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.DirectoryProvider;

/**
 * Allow a BackendQueueProcessorFactory to be notified of {@code DirectoryProvider} changes.
 *
 * @author Emmanuel Bernard
 * @experimental This API is experimental
 */
public interface UpdatableBackendQueueProcessorFactory extends BackendQueueProcessorFactory {
	/**
	 * Update the list of <code>DirectoryProvider</code>s in case the SearchFactory is updated.
	 * The processor factory should react and update its state accordingly.
	 */
	void updateDirectoryProviders(Set<DirectoryProvider<?>> providers, WorkerBuildContext context);
}
