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
package org.hibernate.search.store.impl;

import java.io.Serializable;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.AdvancedShardIdentifierProvider;
import org.hibernate.search.store.ShardIdentifierProvider;

public class ShardIdentifierWrapper implements AdvancedShardIdentifierProvider {

	private final ShardIdentifierProvider wrapped;

	public ShardIdentifierWrapper(ShardIdentifierProvider wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public void initialize(Properties properties, BuildContext buildContext) {
		wrapped.initialize( properties, buildContext );
	}

	@Override
	public String getShardIdentifier(Class<?> entityType, Serializable id, String idAsString, Document document) {
		return wrapped.getShardIdentifier( entityType, id, idAsString, document );
	}

	@Override
	public Set<String> getShardIdentifiersForQuery(FullTextFilterImplementor[] fullTextFilters) {
		return wrapped.getShardIdentifiersForQuery( fullTextFilters );
	}

	@Override
	public Set<String> getAllShardIdentifiers() {
		return wrapped.getAllShardIdentifiers();
	}

	@Override
	public Set<String> getShardIdentifiersForDeletion(Class<?> entityType, Serializable id, String idAsString) {
		return wrapped.getAllShardIdentifiers();
	}

	public ShardIdentifierProvider unwrap() {
		return wrapped;
	}

}
