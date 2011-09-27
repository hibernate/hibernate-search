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

package org.hibernate.search.backend;

import java.io.Serializable;
import java.util.Map;

import org.apache.lucene.document.Document;

import org.hibernate.search.backend.impl.WorkVisitor;

/**
 * Carries a Lucene update operation from the engine to the backend
 * 
 * @since 4.0
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class UpdateLuceneWork extends LuceneWork implements Serializable {

	private static final long serialVersionUID = -5267394465359187688L;
	
	private final Map<String, String> fieldToAnalyzerMap;

	public UpdateLuceneWork(Serializable id, String idInString, Class<?> entity, Document document) {
		this( id, idInString, entity, document, null );
	}

	public UpdateLuceneWork(Serializable id, String idInString, Class<?> entity, Document document, Map<String, String> fieldToAnalyzerMap) {
		super( id, idInString, entity, document );
		this.fieldToAnalyzerMap = fieldToAnalyzerMap;
	}

	@Override
	public Map<String, String> getFieldToAnalyzerMap() {
		return fieldToAnalyzerMap;
	}

	@Override
	public <T> T getWorkDelegate(final WorkVisitor<T> visitor) {
		return visitor.getDelegate( this );
	}
	
	@Override
	public String toString() {
		return "UpdateLuceneWork: " + this.getEntityClass().getName() + "#" + this.getIdInString();
	}

}
