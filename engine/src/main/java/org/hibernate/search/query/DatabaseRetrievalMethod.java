/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.query;

/**
 * Defines the method used to initialize an object
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public enum DatabaseRetrievalMethod {
	/**
	 * Use a criteria query to load the objects.
	 * This is done in batch to minimize the number of queries
	 *
	 * Default approach
	 */
	QUERY,

	/**
	 * Load each object by its identifier one by one.
	 * Useful if a batch size is set in the entity's mapping
	 */
	FIND_BY_ID
}
