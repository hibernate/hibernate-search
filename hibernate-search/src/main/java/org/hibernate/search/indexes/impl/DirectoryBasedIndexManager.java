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
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Similarity;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.IndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.DirectoryProvider;

/**
 * First implementation will use the "legacy" DirectoryProvider which served us so well.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class DirectoryBasedIndexManager implements IndexManager {
	
	private String indexName;
	private final DirectoryProvider directoryProvider;
	private Similarity similarity;
	
	public DirectoryBasedIndexManager(DirectoryProvider directoryProvider) {
		this.directoryProvider = directoryProvider;
	}

	@Override
	public String getIndexName() {
		return indexName;
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
	public void destroy() {
	}

	@Override
	public void initialize(String indexName, Properties props, BuildContext context) {
		this.indexName = indexName;
	}

	@Override
	public Set<Class<?>> getContainedTypes() {
		return null;
	}

	@Override
	public Similarity getSimilarity() {
		return similarity;
	}

	@Override
	public void setSimilarity(Similarity newSimilarity) {
		this.similarity = newSimilarity;
	}

}
