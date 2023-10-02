/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.util.Set;

import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.impl.CollectionHelper;

import org.jboss.jandex.IndexView;

/**
 * A source of types to be processed for annotations by Hibernate Search.
 * <p>
 * This is mainly useful when the following options are enabled:
 * <ul>
 * <li>{@link AnnotationMappingConfigurationContext#discoverAnnotatedTypesFromRootMappingAnnotations(boolean)} discovering annotated types that are also annotated with root mapping annotations}.
 * <li>{@link AnnotationMappingConfigurationContext#discoverJandexIndexesFromAddedTypes(boolean)} discovering Jandex indexes from types added}
 * </ul>
 */
@Incubating
public abstract class AnnotatedTypeSource {

	/**
	 * @param clazz A class annotated with Hibernate Search annotations,
	 * or (if taking advantage of {@link AnnotationMappingConfigurationContext#discoverJandexIndexesFromAddedTypes(boolean) Jandex-based discovery})
	 * a class within a JAR that contains other classes annotated with Hibernate Search annotations.
	 * @param otherClasses Other classes following the same requirements as {@code firstClass}.
	 * @return A source that {@link AnnotationMappingConfigurationContext#add(Set) adds} the provided classes explicitly,
	 * and by default also allows {@link AnnotationMappingConfigurationContext#discoverJandexIndexesFromAddedTypes(boolean) other classes from the same JAR to be discovered automatically}
	 * if they are {@link AnnotationMappingConfigurationContext#discoverAnnotatedTypesFromRootMappingAnnotations(boolean) annotated with root mapping annotations}
	 * such as {@link SearchEntity} or {@link ProjectionConstructor}.
	 */
	public static AnnotatedTypeSource fromClasses(Class<?> clazz, Class<?>... otherClasses) {
		return fromClasses( Set.copyOf( CollectionHelper.asList( clazz, otherClasses ) ) );
	}

	/**
	 * @param annotatedClasses Classes annotated with Hibernate Search annotations.
	 * @return A source that {@link AnnotationMappingConfigurationContext#add(Set) adds} the provided classes explicitly.
	 * Note that {@link AnnotationMappingConfigurationContext#discoverJandexIndexesFromAddedTypes(boolean) other classes from the same JAR may be discovered automatically}
	 * if they are {@link AnnotationMappingConfigurationContext#discoverAnnotatedTypesFromRootMappingAnnotations(boolean) annotated with root mapping annotations}
	 * such as {@link SearchEntity} or {@link ProjectionConstructor}.
	 */
	public static AnnotatedTypeSource fromClasses(Set<Class<?>> annotatedClasses) {
		return new AnnotatedTypeSource() {
			@Override
			public void apply(AnnotationMappingConfigurationContext context) {
				context.add( annotatedClasses );
			}
		};
	}

	/**
	 * @return An empty source. Only useful if you're planning on using {@link org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext programmatic mapping}.
	 */
	public static AnnotatedTypeSource empty() {
		return new AnnotatedTypeSource() {
			@Override
			public void apply(AnnotationMappingConfigurationContext context) {
				// No-op
			}
		};
	}

	/**
	 * @param jandexIndex A Jandex index containing types annotated with Hibernate Search annotations.
	 * @return A source for {@link AnnotationMappingConfigurationContext#discoverAnnotatedTypesFromRootMappingAnnotations(boolean) discovering types annotated with root mapping annotations} within the given index.
	 */
	public static AnnotatedTypeSource fromJandexIndex(IndexView jandexIndex) {
		return new AnnotatedTypeSource() {
			@Override
			public void apply(AnnotationMappingConfigurationContext context) {
				context.addJandexIndex( jandexIndex );
			}
		};
	}

	private AnnotatedTypeSource() {
	}

	public abstract void apply(AnnotationMappingConfigurationContext context);
}
