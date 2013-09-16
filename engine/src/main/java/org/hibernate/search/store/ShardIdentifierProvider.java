/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.store;

import org.apache.lucene.document.Document;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.spi.BuildContext;

import java.io.Serializable;
import java.util.Properties;

/**
 * Provides shard identifiers when dynamic sharding is used.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface ShardIdentifierProvider {

	/**
	 * Initialize the provider.
	 *
	 * @param properties The configuration properties
	 * @param buildContext The buildContext
	 */
	void initialize(Properties properties, BuildContext buildContext);

	/**
	 * Returns the shard identifier upon addition.
	 */
	String getShardIdentifier(Class<?> entity, Serializable id, String idInString, Document document);

	/**
	 * Returns the set of shard identifiers upon deletion.
	 */
	String[] getShardIdentifiers(Class<?> entity, Serializable id, String idInString);

	/**
	 * Returns the set of shard identifiers for a query.
	 */
	String[] getShardIdentifiersForQuery(FullTextFilterImplementor[] fullTextFilters);

	/**
	 * Returns the list of all known shard identifiers.
	 * The list can vary between calls.
	 */
	String[] getAllShardIdentifiers();
}
