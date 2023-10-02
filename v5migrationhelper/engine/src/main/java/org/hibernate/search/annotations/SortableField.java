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
 * Makes a property sortable.
 * <p>
 * A field for that property must be declared via the {@link Field} annotation from which the field bridge configuration
 * will be inherited. In the rare case that a property should be sortable but not searchable, declare a field which is
 * not indexed nor stored. Then only the sort field will be added to the document, but no standard index field.
 * <p>
 * Sorting on a field without a declared sort field will still work, but it will be slower and cause a higher memory
 * consumption. Therefore it's strongly recommended to declare each required sort field.
 *
 * @author Gunnar Morling
 * @deprecated Use Hibernate Search 6's field annotations ({@link GenericField}, {@link KeywordField}, ...)
 * and enable sorts with <code>{@link GenericField#sortable() @GenericField(sortable = Sortable.YES)}</code>
 * instead.
 * Note that {@link FullTextField} cannot be marked as sortable, but you can define a {@link KeywordField}
 * alongside your {@link FullTextField}, with a different name, and that field can be marked as sortable.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Documented
@Deprecated
@Repeatable(SortableFields.class)
public @interface SortableField {

	/**
	 * @return the name of the field whose field bridge to apply to obtain the value of this sort field. Can be omitted in case only
	 * a single field exists for the annotated property.
	 */
	String forField() default "";
}
