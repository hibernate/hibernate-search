/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.ObjectProjectionProcessor;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Maps a constructor parameter to an object projection bound to a specific object field in the indexed document.
 * <p>
 * The content of the object projection is defined in the constructor parameter type
 * by another {@link ProjectionConstructor}.
 * <p>
 * Compared to the basic {@link CompositeProjection composite projection},
 * an object projection is bound to a specific object field,
 * and thus it yields zero, one or many values, as many as there are objects in the targeted object field.
 * Therefore, you must take care of using a {@code List<...>} as your constructor parameter type
 * if the object field is multi-valued.
 *
 * @see SearchProjectionFactory#object(String)
 * @see org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep#as(Class)
 */
@Documented
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@MethodParameterMapping(processor = @MethodParameterMappingAnnotationProcessorRef(type = ObjectProjectionProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface ObjectProjection {

	/**
	 * @return The <a href="../../../../../engine/search/projection/dsl/SearchProjectionFactory.html#field-paths">path</a>
	 * to the object field whose object(s) will be extracted.
	 * Defaults to the name of the annotated constructor parameter,
	 * if it can be retrieved (requires the class to be compiled with the {@code -parameters} flag;
	 * otherwise an empty {@code path} will lead to a failure).
	 * @see SearchProjectionFactory
	 */
	String path() default "";

	/**
	 * The paths of nested index field to be included,
	 * i.e. for which the corresponding nested projections will actually be retrieved from the index
	 * (projections on excluded fields will be ignored
	 * and will have their value set to {@code null}, or an empty collection for multi-valued fields).
	 * <p>
	 * This takes precedence over {@link #includeDepth()}.
	 * <p>
	 * Cannot be used when {@link #excludePaths()} contains any paths.
	 * <p>
	 * By default, if none of {@code includePaths}, {@link #excludePaths()} or {@link #includeDepth()} are defined,
	 * all index fields are included.
	 *
	 * @return The paths of index fields to include explicitly.
	 * Provided paths must be relative to the projected object field,
	 * i.e. they must not include the {@link #path()}.
	 */
	String[] includePaths() default { };

	/**
	 * The paths of nested index field to be excluded,
	 * i.e. for which the corresponding nested projections will not be retrieved from the index
	 * and will instead have their value set to {@code null}, or an empty collection for multi-valued fields.
	 * <p>
	 * This takes precedence over {@link #includeDepth()}.
	 * <p>
	 * Cannot be used when {@link #includePaths()} contains any paths.
	 * <p>
	 * By default, if none of {@link #includePaths()}, {@code excludePaths} or {@link #includeDepth()} are defined,
	 * all index fields are included.
	 * <p>
	 * Index fields that are represented in object projections but are excluded through filters
	 * ({@link #includePaths()}/{@code excludePaths()}/{@link #includeDepth()})
	 * will not be retrieved from the index and will have their value set to {@code null},
	 * or an empty collection for multi-valued fields.
	 *
	 * @return The paths of index fields to exclude explicitly.
	 * Provided paths must be relative to the object projection,
	 * i.e. they must not include the {@link #path()}.
	 */
	@Incubating
	String[] excludePaths() default { };

	/**
	 * The number of levels of object projections that will have all their nested field/object projections
	 * included by default and actually be retrieved from the index
	 * (projections on excluded fields will be ignored
	 * and will have their value set to {@code null}, or an empty collection for multi-valued fields).
	 * <p>
	 * Up to and including that depth, object projections
	 * will be included along with their nested (non-object) field projections,
	 * even if these fields are not included explicitly through {@code includePaths},
	 * unless these fields are excluded explicitly through {@code excludePaths}:
	 * <ul>
	 * <li>{@code includeDepth=0} means fields of this object projection are <strong>not</strong> included,
	 * nor is any field of nested object projections,
	 * unless these fields are included explicitly through {@link #includePaths()}.
	 * <li>{@code includeDepth=1} means fields of this object projection <strong>are</strong> included,
	 * unless these fields are explicitly excluded through {@code excludePaths},
	 * but <strong>not</strong> fields of nested object projections ({@code @ObjectProjection} within this {@code @ObjectProjection}),
	 * unless these fields are included explicitly through {@link #includePaths()}.
	 * <li>{@code includeDepth=2} means fields of this object projection <strong>are</strong> included,
	 * and so are fields of immediately nested object projections ({@code @ObjectProjection} within this {@code @ObjectProjection}),
	 * unless these fields are explicitly excluded through {@code excludePaths},
	 * but <strong>not</strong> fields of nested object projections beyond that
	 * ({@code @ObjectProjection} within an {@code @ObjectProjection} within this {@code @ObjectProjection}),
	 * unless these fields are included explicitly through {@link #includePaths()}.
	 * <li>And so on.
	 * </ul>
	 * The default value depends on the value of {@link #includePaths()} attribute:
	 * <ul>
	 * <li>if {@link #includePaths()} is empty, the default is {@code Integer.MAX_VALUE} (include all fields at every level)</li>
	 * <li>if {@link #includePaths()} is <strong>not</strong> empty, the default is {@code 0} (only include fields included explicitly).</li>
	 * </ul>
	 *
	 * @return The number of levels of object projections that will have all their fields included by default.
	 */
	int includeDepth() default -1;


}
