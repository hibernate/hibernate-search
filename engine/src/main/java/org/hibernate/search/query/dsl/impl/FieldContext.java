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

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;

/**
 * @author Emmanuel Bernard
 */
public class FieldContext {
	private final String field;
	private boolean ignoreAnalyzer;
	private final QueryCustomizer fieldCustomizer;
	private boolean ignoreFieldBridge;
	private FieldBridge fieldBridge;

	public FieldContext(String field) {
		this.field = field;
		this.fieldCustomizer = new QueryCustomizer();
	}

	public String getField() {
		return field;
	}

	public boolean isIgnoreAnalyzer() {
		return ignoreAnalyzer;
	}

	public void setIgnoreAnalyzer(boolean ignoreAnalyzer) {
		this.ignoreAnalyzer = ignoreAnalyzer;
	}

	public QueryCustomizer getFieldCustomizer() {
		return fieldCustomizer;
	}

	public boolean isIgnoreFieldBridge() {
		return ignoreFieldBridge;
	}

	public void setIgnoreFieldBridge(boolean ignoreFieldBridge) {
		this.ignoreFieldBridge = ignoreFieldBridge;
	}

	public void setFieldBridge(FieldBridge fieldBridge) {
		this.fieldBridge = fieldBridge;
	}

	public FieldBridge getFieldBridge() {
		return fieldBridge;
	}

	public String objectToString(DocumentBuilderIndexedEntity<?> documentBuilder, Object value, ConversionContext conversionContext) {
		if ( isIgnoreFieldBridge() ) {
			return value == null ? null : value.toString();
		}
		else if ( fieldBridge != null ) {
			return documentBuilder.objectToString( field, fieldBridge, value, conversionContext );
		}
		else {
			return documentBuilder.objectToString( field, value, conversionContext );
		}
	}
}
