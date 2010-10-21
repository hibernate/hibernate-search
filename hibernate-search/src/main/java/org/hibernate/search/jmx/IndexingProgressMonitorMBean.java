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
package org.hibernate.search.jmx;

/**
 * A MBean for following the progress of mass indexing.
 *
 * @author Hardy Ferentschik
 */
public interface IndexingProgressMonitorMBean {

	public static final String INDEXING_PROGRESS_MONITOR_MBEAN_OBJECT_NAME = "org.hibernate.search.jmx:type=IndexingProgressMBean";

	/**
	 * @return the number of entities loaded so far
	 */
	long getLoadedEntitiesCount();

	/**
	 * @return the number of Lucene {@code Document}s added so far
	 */
	long getDocumentsAddedCount();

	/**
	 * @return the total number of entities which need indexing
	 */
	long getNumberOfEntitiesToIndex();
}


