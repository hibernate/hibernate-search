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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation allows a user to apply an implementation
 * class to a Lucene document to manipulate it in any way
 * the user sees fit.
 *
 * @author John Griffin
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
@Documented
public @interface ClassBridge {
	/**
	 * Field name, default to the JavaBean property name.
	 */
	String name() default "";

	/**
	 * Should the value be stored in the document.
	 * defaults to no.
	 */
	Store store() default Store.NO;

	/**
	 * Define an analyzer for the field, default to
	 * the inherited analyzer.
	 */
	Analyzer analyzer() default @Analyzer;

	/**
	 * Defines how the Field should be indexed
	 * defaults to tokenized.
	 */
	Index index() default Index.TOKENIZED;

	/**
	 * Define term vector storage requirements,
	 * default to NO.
	 */
	TermVector termVector() default TermVector.NO;

	/**
	 * A float value of the amount of Lucene defined
	 * boost to apply to a field.
	 */
	Boost boost() default @Boost(value=1.0F);

	/**
	 * User supplied class to manipulate document in
	 * whatever mysterious ways they wish to.
	 */
	public Class<?> impl();

	/**
	 * Array of fields to work with. The impl class
	 * above will work on these fields.
	 */
	public Parameter[] params() default {};

}
