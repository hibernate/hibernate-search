/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.metadata;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.bridge.FieldBridge;

/**
 * Metadata related to a single field. It extends @{code FieldSettingsDescriptor} to add Search specific
 * information, like {@link #indexNullAs()}. It also contains the analyzer and field bridge used to create the
 * actual field for the Lucene {@code Document}.
 *
 * @author Hardy Ferentschik
 */
public interface FieldDescriptor extends FieldSettingsDescriptor {
	/**
	 * @return the string used to index {@code null} values. {@code null} in case null values are not indexed
	 */
	String indexNullAs();

	/**
	 * @return {@code true} if {@code null} values are indexed, {@code false} otherwise
	 *
	 * @see #indexNullAs()
	 */
	boolean indexNull();

	/**
	 * @return the field bridge instance used to convert the property value into a string based field value
	 */
	FieldBridge getFieldBridge();

	/**
	 * @return the analyzer used for this field, {@code null} if the field is not analyzed
	 */
	Analyzer getAnalyzer();
}
