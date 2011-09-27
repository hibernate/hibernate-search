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
package org.hibernate.search.backend.spi;

/**
 * Enumeration of the different types of Lucene work. This enumeration is used to specify the type
 * of index operation to be executed. 
 * 
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 */
public enum WorkType {
	ADD(true),
	UPDATE(true),
	DELETE(false),
	COLLECTION(true),
	/**
	 * Used to remove a specific instance
	 * of a class from an index.
	 */
	PURGE(false),
	/**
	 * Used to remove all instances of a
	 * class from an index.
	 */
	PURGE_ALL(false),
	
	/**
	 * This type is used for batch indexing.
	 */
	INDEX(true);

	private final boolean searchForContainers;

	private WorkType(boolean searchForContainers) {
		this.searchForContainers = searchForContainers;
	}

	/**
	 * When references are changed, either null or another one, we expect dirty checking to be triggered (both sides
	 * have to be updated)
	 * When the internal object is changed, we apply the {Add|Update}Work on containedIns
	 */
	public boolean searchForContainers() {
		return this.searchForContainers;
	}
}
