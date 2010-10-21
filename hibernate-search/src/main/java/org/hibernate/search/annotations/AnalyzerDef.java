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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;

/**
 * Reusable analyzer definition.
 * An analyzer definition defines:
 * <ul>
 * <li>one tokenizer</li>
 * </li>optionally one or more filters</li>
 * </ul>
 * Filters are applied in the order they are defined.
 * <p/>
 * Reuses the Solr Tokenizer and Filter architecture.
 *
 * @author Emmanuel Bernard
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Documented
public @interface AnalyzerDef {
	/**
	 * @return Reference name to be used on {#org.hibernate.search.annotations.Analyzer}
	 */
	String name();

	/**
	 * @return CharFilters used. The filters are applied in the defined order
	 */
	CharFilterDef[] charFilters() default { };

	/**
	 * @return Tokenizer used.
	 */
	TokenizerDef tokenizer();

	/**
	 * @return Filters used. The filters are applied in the defined order
	 */
	TokenFilterDef[] filters() default { };
}
