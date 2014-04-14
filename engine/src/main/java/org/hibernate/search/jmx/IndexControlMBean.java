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
 * Defines the Hibernate Search exposed JMX attributes and operations for index creation and purging.
 *
 * @experimental This MBean is experimental
 * @author Hardy Ferentschik
 */
public interface IndexControlMBean {

	String INDEX_CTRL_MBEAN_OBJECT_NAME = "org.hibernate.search.jmx:type=IndexControlMBean";

	/**
	 * Sets the batch size for the mass indexer.
	 *
	 * @param batchSize the new batch size
	 */
	void setBatchSize(int batchSize);

	/**
	 * @return the current batch size for (mass) indexing
	 */
	int getBatchSize();

	/**
	 * @param numberOfThreads  the number of threads used for object loading during mass indexing.
	 */
	void setNumberOfObjectLoadingThreads(int numberOfThreads);

	/**
	 * @return the current number of threads during mass indexing
	 */
	int getNumberOfObjectLoadingThreads();

	/**
	 * @param numberOfThreads the number of threads used for collections fetching during mass indexing
	 */
	void setNumberOfFetchingThreads(int numberOfThreads);

	/**
	 * @return the current number of threads used for collection fetching
	 */
	int getNumberOfFetchingThreads();

	/**
	 * Index the specified entity using the mass indexer.
	 * <p><b>Note:<br/>
	 * This method is only available if the Hibernate {@code SessionFactory}
	 * is available via JNDI.
	 * </p>
	 *
	 * @param entity The fqc of the entity to index
	 *
	 * @throws IllegalArgumentException	  in case the entity name is not valid
	 * @throws UnsupportedOperationException in case the Hibernate {@code SessionFactory} is not bound via JNDI.
	 */
	void index(String entity);

	/**
	 * Optimizes the index for the specified entity.
	 * <p><b>Note:<br/>
	 * This method is only available if the Hibernate {@code SessionFactory}
	 * is available via JNDI.
	 * </p>
	 *
	 * @param entity The fqc of the entity to index
	 *
	 * @throws IllegalArgumentException	  in case the entity name is not valid
	 * @throws UnsupportedOperationException in case the Hibernate {@code SessionFactory} is not bound via JNDI.
	 */
	void optimize(String entity);

	/**
	 * Purge the index of the specified entity.
	 * <p><b>Note:<br/>
	 * This method is only available if the Hibernate {@code SessionFactory}
	 * is available via JNDI.
	 * </p>
	 *
	 * @param entity The fqc of the entity to index
	 *
	 * @throws IllegalArgumentException	  in case the entity name is not valid
	 * @throws UnsupportedOperationException in case the Hibernate {@code SessionFactory} is not bound via JNDI.
	 */
	void purge(String entity);
}
