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
package org.hibernate.search.indexes.serialization.javaserialization.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class Update implements Operation {

	private String entityClassName;
	private Serializable id;
	private Map<String,String> fieldToAnalyzerMap = new HashMap<String, String>();
	private SerializableDocument document;

	public Update(String entityClassName, Serializable id, SerializableDocument document, Map<String, String> fieldToAnalyzerMap) {
		this.entityClassName = entityClassName;
		this.id = id;
		this.document = document;
		this.fieldToAnalyzerMap = fieldToAnalyzerMap;
	}

	public String getEntityClassName() {
		return entityClassName;
	}

	public Serializable getId() {
		return id;
	}

	public Map<String, String> getFieldToAnalyzerMap() {
		return fieldToAnalyzerMap;
	}

	public SerializableDocument getDocument() {
		return document;
	}
}
