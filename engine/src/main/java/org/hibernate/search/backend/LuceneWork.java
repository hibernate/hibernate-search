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
package org.hibernate.search.backend;

import java.io.Serializable;
import java.util.Map;

import org.apache.lucene.document.Document;

import org.hibernate.search.backend.impl.WorkVisitor;

/**
 * Represent a Serializable Lucene unit work
 *
 * WARNING: This class aims to be serializable and passed in an asynchronous way across VMs
 *          any non backward compatible serialization change should be done with great care
 *          and publically announced. Specifically, new versions of Hibernate Search should be
 *          able to handle changes produced by older versions of Hibernate Search if reasonably possible.
 *          That is why each subclass susceptible to be pass along have a magic serialization number.
 *          NOTE: we are relying on Lucene's Document to play nice unfortunately
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public abstract class LuceneWork implements Serializable {

	private final Document document;
	private final Class<?> entityClass;
	private final Serializable id;
	private final String idInString;

	public LuceneWork(Serializable id, String idInString, Class<?> entity) {
		this( id, idInString, entity, null );
	}

	public LuceneWork(Serializable id, String idInString, Class<?> entity, Document document) {
		this.id = id;
		this.idInString = idInString;
		this.entityClass = entity;
		this.document = document;
	}

	public Document getDocument() {
		return document;
	}

	public Class<?> getEntityClass() {
		return entityClass;
	}

	public Serializable getId() {
		return id;
	}

	public String getIdInString() {
		return idInString;
	}

	public abstract <T> T getWorkDelegate(WorkVisitor<T> visitor);

	public Map<String, String> getFieldToAnalyzerMap() {
		return null;
	}

}
