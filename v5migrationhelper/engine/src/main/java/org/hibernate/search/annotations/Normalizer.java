/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

/**
 * Define a Normalizer for a {@code @Field}
 *
 * Either describe an explicit implementation through the <code>impl</code> parameter
 * or use an external {@code @NormalizerDef} definition through the <code>definition</code> parameter
 *
 * @author Emmanuel Bernard
 * @deprecated No longer necessary in Hibernate Search 6.
 * Replace {@link Field} with {@link KeywordField} and pass the normalizer name directly to {@link KeywordField#normalizer()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ })
@Documented
@Deprecated
public @interface Normalizer {
	Class<?> impl() default void.class;

	String definition() default "";
}
