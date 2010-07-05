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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines the Hibernate Search exposed JMX attributes and operations for index configuration.
 *
 * @author Hardy Ferentschik
 */
public interface HibernateSearchConfigInfoMBean {

	public static final String CONFIG_MBEAN_OBJECT_NAME = "org.hibernate.search.jmx:type=HibernateSearchConfigInfoMBean";

	/**
	 * Returns a list of all indexed classes.
	 *
	 * @return list of all indexed classes
	 */
	Set<String> getIndexedClassNames();

	/**
	 * Returns the indexing strategy - <i>manual</i> vs. event <i>event</i>.
	 *
	 * @return the indexing strategy
	 */
	String getIndexingStrategy();

	/**
	 * Returns the number of documents for the given entity.
	 *
	 * @param entity the fqc of the entity
	 *
	 * @return number of documents for the specified entity name
	 *
	 * @throws IllegalArgumentException in case the entity name is not valid
	 */
	int getNumberOfIndexedEntities(String entity);

	/**
	 * A list of string representations of the indexing parameters for each directory of the specified entity.
	 * Defaults are not displayed, but only parameters which are explicitly set via the configuration.
	 *
	 * @param entity the fqc of the entity
	 *
	 * @return A list of string representations of the indexing parameters for each directory of the specified entity
	 *
	 * @throws IllegalArgumentException in case the entity name is not valid
	 */
	List<String> getIndexingParameters(String entity);

	/**
	 * Returns a map of all indexed entities and their document count in the index.
	 *
	 * @return a map of all indexed entities and their document count. The map key is the fqc of the entity and
	 *         the map value is the document count.
	 */
	Map<String, Integer> indexedEntitiesCount();
}
