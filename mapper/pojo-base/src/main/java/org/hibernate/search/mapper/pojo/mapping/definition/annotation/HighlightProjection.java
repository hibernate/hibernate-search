/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.HighlightProjectionProcessor;

/**
 * Maps a constructor parameter to a projection to highlights,
 * i.e. sequences of text that matched the query, extracted from the given field's value.
 *
 * @see SearchProjectionFactory#highlight(String)
 */
@Documented
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@MethodParameterMapping(processor = @MethodParameterMappingAnnotationProcessorRef(type = HighlightProjectionProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface HighlightProjection {

	/**
	 * @return The <a href="../../../../../engine/search/projection/dsl/SearchProjectionFactory.html#field-paths">path</a>
	 * to the index field whose value will be extracted.
	 * Defaults to the name of the annotated constructor parameter,
	 * if it can be retrieved (requires the class to be compiled with the {@code -parameters} flag;
	 * otherwise an empty {@code path} will lead to a failure).
	 * @see SearchProjectionFactory#highlight(String)
	 */
	String path() default "";

	/**
	 * @return The name of a highlighter
	 * {@link org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep#highlighter(String, Function) defined on the query}.
	 * Defaults to the default (unnamed) highlighter.
	 * @see org.hibernate.search.engine.search.projection.dsl.HighlightProjectionOptionsStep#highlighter(String)
	 */
	String highlighter() default "";

}
