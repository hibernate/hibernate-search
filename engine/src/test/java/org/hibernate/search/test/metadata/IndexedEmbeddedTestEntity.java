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
package org.hibernate.search.test.metadata;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

@Indexed
public class IndexedEmbeddedTestEntity {

	@DocumentId
	private Long id;

	@Field(analyze = Analyze.NO)
	private String name;

	@IndexedEmbedded(includePaths = "name")
	private IndexedEmbeddedTestEntity indexedEmbeddedWithIncludePath;

	@IndexedEmbedded(depth = 1)
	private IndexedEmbeddedTestEntity indexedEmbeddedWithDepth;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public IndexedEmbeddedTestEntity getIndexedEmbeddedWithIncludePath() {
		return indexedEmbeddedWithIncludePath;
	}

	public void setIndexedEmbeddedWithIncludePath(IndexedEmbeddedTestEntity indexedEmbeddedWithIncludePath) {
		this.indexedEmbeddedWithIncludePath = indexedEmbeddedWithIncludePath;
	}

	public IndexedEmbeddedTestEntity getIndexedEmbeddedWithDepth() {
		return indexedEmbeddedWithDepth;
	}

	public void setIndexedEmbeddedWithDepth(IndexedEmbeddedTestEntity indexedEmbeddedWithDepth) {
		this.indexedEmbeddedWithDepth = indexedEmbeddedWithDepth;
	}

}
