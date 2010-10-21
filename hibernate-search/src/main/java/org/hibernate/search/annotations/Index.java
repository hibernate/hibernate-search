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
package org.hibernate.search.annotations;

/**
 * Defines how an Field should be indexed
 */
public enum Index {
	/**
	 * Do not index the field value. This field can thus not be searched,
	 * but one can still access its contents provided it is
	 * {@link Store stored}.
	 */
	NO,
	/**
	 * Index the field's value so it can be searched. An Analyzer will be used
	 * to tokenize and possibly further normalize the text before its
	 * terms will be stored in the index. This is useful for common text.
	 */
	TOKENIZED,
	/**
	 * Index the field's value without using an Analyzer, so it can be searched.
	 * As no analyzer is used the value will be stored as a single term. This is
	 * useful for unique Ids like product numbers.
	 */
	UN_TOKENIZED,
	/**
	 * Index the field's value without an Analyzer, and disable
	 * the storing of norms.  No norms means that index-time boosting
	 * and field length normalization will be disabled.  The benefit is
	 * less memory usage as norms take up one byte per indexed field
	 * for every document in the index.
	 */
	NO_NORMS
}
