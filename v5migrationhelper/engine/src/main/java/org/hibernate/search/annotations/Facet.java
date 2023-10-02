/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

/**
 * Extension annotation for {@code @Field} enabling it for faceting.
 *
 * @author Hardy Ferentschik
 * @hsearch.experimental : This feature is experimental
 * @deprecated Use Hibernate Search 6's field annotations ({@link GenericField}, {@link KeywordField},
 * {@link FullTextField}, ...)
 * and enable faceting with <code>{@link GenericField#aggregable() @GenericField(aggregable = Aggregable.YES)}</code>
 * instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Documented
@Deprecated
@Repeatable(Facets.class)
public @interface Facet {

	/**
	 * @return the facet name. Defaults to the name of the field this facet refers to
	 */
	String name() default "";

	/**
	 * @return the field name this annotation refers to. It can be omitted in case there is only a single {@code @Field}
	 * annotation
	 */
	String forField() default "";

}
