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
package org.hibernate.search.indexes.impl;

import java.util.List;
import java.util.Properties;

import org.apache.lucene.index.IndexReader;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.IndexManager;
import org.hibernate.search.spi.BuildContext;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class DefaultIndexManager implements IndexManager {

	@Override
	public String getIndexName() {
		return null;
	}

	@Override
	public IndexReader openReader() {
		return null;
	}

	@Override
	public void closeReader(IndexReader indexReader) {
	}

	@Override
	public void applyIndexOperations(List<LuceneWork> queue) {
	}

	@Override
	public void initialize(Properties props, BuildContext context) {
	}

	@Override
	public void destroy() {
	}

}
