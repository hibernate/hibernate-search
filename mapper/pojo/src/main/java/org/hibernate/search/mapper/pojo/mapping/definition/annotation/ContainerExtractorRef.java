/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;

import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractor;

/**
 * @author Yoann Rodiere
 */
@Documented
@Target({}) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerExtractorRef {

	@SuppressWarnings("deprecation")
	BuiltinContainerExtractor value() default BuiltinContainerExtractor.UNDEFINED;

	@SuppressWarnings("rawtypes") // We need to allow raw container types, e.g. MapValueExtractor.class
	Class<? extends ContainerExtractor> type() default UndefinedContainerExtractorImplementationType.class;

	abstract class UndefinedContainerExtractorImplementationType implements ContainerExtractor<Object, Object> {
		private UndefinedContainerExtractorImplementationType() {
		}
	}
}
