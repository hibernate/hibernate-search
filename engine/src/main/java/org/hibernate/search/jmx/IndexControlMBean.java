/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jmx;

/**
 * Defines the Hibernate Search exposed JMX attributes and operations for index creation and purging.
 *
 * @hsearch.experimental This MBean is experimental
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
	 * <p><b>Note:</b><br>
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
	 * <p><b>Note:</b><br>
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
	 * <p><b>Note:</b><br>
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
