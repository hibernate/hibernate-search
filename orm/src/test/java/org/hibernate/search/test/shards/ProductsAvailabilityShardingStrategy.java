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
package org.hibernate.search.test.shards;

import java.io.Serializable;
import java.util.Properties;

import org.apache.lucene.document.Document;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class ProductsAvailabilityShardingStrategy implements IndexShardingStrategy {

	private IndexManager availableProductsIndex;
	private IndexManager toOrderProductsIndex;
	private IndexManager[] both;

	@Override
	public void initialize(Properties properties, IndexManager[] indexManagers) {
		availableProductsIndex = indexManagers[0];
		toOrderProductsIndex = indexManagers[1];
		both = indexManagers;
	}

	@Override
	public IndexManager[] getIndexManagersForAllShards() {
		return both;
	}

	@Override
	public IndexManager getIndexManagerForAddition(Class<?> entity, Serializable id, String idInString, Document document) {
		String isAvailable = document.get( "available" );
		if ( "true".equals( isAvailable ) ) {
			return availableProductsIndex;
		}
		else {
			return toOrderProductsIndex;
		}
	}

	@Override
	public IndexManager[] getIndexManagersForDeletion(Class<?> entity, Serializable id, String idInString) {
		return both;
	}

	@Override
	public IndexManager[] getIndexManagersForQuery(FullTextFilterImplementor[] fullTextFilters) {
		return both;
	}

}
