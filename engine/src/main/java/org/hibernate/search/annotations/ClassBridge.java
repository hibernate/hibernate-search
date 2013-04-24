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
 * @author Hardy Ferentschik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ClassBridge {
	/**
	 * @return default field name passed to your bridge implementation (see {@link org.hibernate.search.bridge.FieldBridge#set(String, Object, org.apache.lucene.document.Document, org.hibernate.search.bridge.LuceneOptions)}).
	 * <p><b>Note:</b><br/>
	 * You can ignore the passed field name in your bridge implementation and add a field or even fields with different names, however any
	 * analyzer specified via {@link #analyzer()} will only apply for the field name specified with this parameter.
	 */
	String name() default "";

	/**
	 * @return Returns an instance of the {@link Store} enum, indicating whether the value should be stored in the document.
	 *         Defaults to {@code Store.NO}
	 */
	Store store() default Store.NO;

	/**
	 * @return Returns a {@code Index} enum defining whether the value should be indexed or not. Defaults to {@code Index.YES}
	 */
	Index index() default Index.YES;

	/**
	 * @return Returns a {@code Analyze} enum defining whether the value should be analyzed or not. Defaults to {@code Analyze.YES}
	 */
	Analyze analyze() default Analyze.YES;

	/**
	 * @return Returns a {@code Norms} enum defining whether the norms should be stored in the index or not. Defaults to {@code Norms.YES}
	 */
	Norms norms() default Norms.YES;

	/**
	 * @return Returns an instance of the {@link TermVector} enum defining how and if term vectors should be stored.
	 *         Default is {@code TermVector.NO}
	 */
	TermVector termVector() default TermVector.NO;

	/**
	 * @return Returns a analyzer annotation defining the analyzer to be used. Defaults to
	 *         the inherited analyzer
	 */
	Analyzer analyzer() default @Analyzer;

	/**
	 * @return Returns a {@code Boost} annotation defining a float index time boost value
	 */
	Boost boost() default @Boost(value = 1.0F);

	/**
	 * @return Custom implementation of class bridge
	 */
	Class<?> impl();

	/**
	 * @return Array of {@code Parameter} instances passed to the class specified by {@link #impl} to initialize the class
	 *         bridge
	 */
	Parameter[] params() default { };
}
