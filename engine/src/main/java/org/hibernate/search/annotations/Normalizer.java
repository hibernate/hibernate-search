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
import java.lang.annotation.Documented;

/**
 * Define a Normalizer for a {@code @Field}
 *
 * Either describe an explicit implementation through the <code>impl</code> parameter
 * or use an external {@code @NormalizerDef} definition through the <code>definition</code> parameter
 *
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target({})
@Documented
public @interface Normalizer {
	/**
	 * @return An {@link org.apache.lucene.analysis.Analyzer} implementation.
	 * @deprecated Support for direct references to analyzer implementations
	 * by class will be removed in Hibernate Search 6.
	 * Use {@link #definition()} instead.
	 */
	@Deprecated
	Class<?> impl() default void.class;

	/**
	 * @return The name of a normalizer defined using
	 * a {@link org.hibernate.search.analyzer.definition.LuceneAnalysisDefinitionProvider},
	 * or a {@code org.hibernate.search.elasticsearch.analyzer.definition.ElasticsearchAnalysisDefinitionProvider},
	 * or the deprecated {@link NormalizerDef}.
	 */
	String definition() default "";
}
