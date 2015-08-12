/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * <li>optionally one or more filters</li>
 * </ul>
 * Filters are applied in the order they are defined.
 * <p>
 * Reuses the Solr Tokenizer and Filter architecture.
 *
 * @author Emmanuel Bernard
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PACKAGE, ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
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
