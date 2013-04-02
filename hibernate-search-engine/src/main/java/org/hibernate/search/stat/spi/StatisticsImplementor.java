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

// $Id:$
package org.hibernate.search.stat.spi;

/**
 * Statistics SPI for the Search. This is essentially the "statistic collector" API.
 *
 * @author Hardy Ferentschik
 */
public interface StatisticsImplementor {
	/**
	 * Callback for number of object loaded from the db.
	 *
	 * @param numberOfObjectsLoaded  Number of objects loaded
	 * @param time time in nanoseconds to load the objects
	 */
	void objectLoadExecuted(long numberOfObjectsLoaded, long time);

	/**
	 * Callback for an executed Lucene search.
	 *
	 * @param searchString executed query string
	 * @param time time in nanoseconds to execute the search
	 */
	void searchExecuted(String searchString, long time);
}
