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

/**
 * JavaDoc copy/pastle from the Apache Lucene project
 * Available under the ASL 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used for marking a property as indexable.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Documented
public @interface Field {

	/**
	 * Default value for {@link #indexNullAs} parameter. Indicates that {@code null} values should not be indexed.
	 */
	String DO_NOT_INDEX_NULL = "__DO_NOT_INDEX_NULL__";

	/**
	 * Value for {@link #indexNullAs} parameter indicating that {@code null} values should not indexed using the
	 */
	String DEFAULT_NULL_TOKEN = "__DEFAULT_NULL_TOKEN__";

	/**
	 * @return Returns the field name. Defaults to the JavaBean property name.
	 */
	String name() default "";

	/**
	 * @return Returns a {@code Store} enum type indicating whether the value should be stored in the document. Defaults to {@code Store.NO}.
	 */
	Store store() default Store.NO;

	/**
	 * @return Returns a {@code Index} enum defining whether the value should be indexed or not. Defaults to {@code Index.YES}.
	 */
	Index index() default Index.YES;

	/**
	 * @return Returns a {@code Analyze} enum defining whether the value should be analyzed or not. Defaults to {@code Analyze.YES}.
	 */
	Analyze analyze() default Analyze.YES;

	/**
	 * @return Returns a {@code Norms} enum defining whether the norms should be stored in the index or not. Defaults to {@code Norms.YES}.
	 */
	Norms norms() default Norms.YES;

	/**
	 * @return Returns a {@code TermVector} enum defining if and how term vectors are stored. Defaults to {@code TermVector.NO}.
	 */
	TermVector termVector() default TermVector.NO;

	/**
	 * @return Returns the analyzer for the field. Defaults to the inherited analyzer.
	 */
	Analyzer analyzer() default @Analyzer;

	/**
	 * @return Returns the boost factor for the field. Default boost factor is 1.0.
	 */
	Boost boost() default @Boost(value = 1.0F);

	/**
	 * @return Returns the field bridge used for this field. Default is auto-wired.
	 */
	FieldBridge bridge() default @FieldBridge;

	/**
	 * @return Returns the value to be used for indexing {@code null}. Per default {@code Field.NO_NULL_INDEXING} is returned indicating that
	 *         null values are not indexed.
	 */
	String indexNullAs() default DO_NOT_INDEX_NULL;
}
