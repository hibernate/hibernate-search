/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

/**
 * Mark a property as indexable into different fields
 * Useful if the field is used for sorting and searching
 *
 * @author Emmanuel Bernard
 * @deprecated Use Hibernate Search 6's field annotations instead:
 * <ul>
 *     <li>{@link FullTextField} for text fields with an analyzer.</li>
 *     <li>{@link KeywordField} for text fields with a normalizer.</li>
 *     <li>{@link GenericField} for non-text fields.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Documented
@Deprecated
public @interface Fields {

	/**
	 * @return the {@link Field} annotations to use for the property
	 */
	Field[] value();

}
