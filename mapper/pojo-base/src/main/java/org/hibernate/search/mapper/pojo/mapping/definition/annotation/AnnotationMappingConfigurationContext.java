/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.util.Set;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.RootMapping;
import org.hibernate.search.util.common.annotation.Incubating;

import org.jboss.jandex.IndexView;

/**
 * A context to configure annotation mapping.
 */
public interface AnnotationMappingConfigurationContext {

	/**
	 * @param enabled {@code true} if Hibernate Search should automatically discover annotated types
	 * present in the Jandex index that are also annotated
	 * with {@link RootMapping root mapping annotations}.
	 * When enabled, if an annotation meta-annotated with {@link RootMapping}
	 * is found in the Jandex index,
	 * and a type annotated with that annotation (e.g. {@link ProjectionConstructor}) is found in the Jandex index,
	 * then that type will automatically be scanned for mapping annotations,
	 * even if the type wasn't {@link #add(Class) added explicitly}.
	 * {@code false} if that discovery should be disabled.
	 * @return {@code this}, for method chaining.
	 * @see RootMapping
	 * @see ProjectionConstructor
	 * @see #addJandexIndex(IndexView)
	 * @see #discoverJandexIndexesFromAddedTypes(boolean)
	 */
	AnnotationMappingConfigurationContext discoverAnnotatedTypesFromRootMappingAnnotations(boolean enabled);

	/**
	 * @param enabled {@code true} if Hibernate Search should automatically discover Jandex Indexes
	 * from types added through {@link #add(Class)} or {@link #add(Set)}.
	 * {@code false} if that discovery should be disabled.
	 * @return {@code this}, for method chaining.
	 * @see #addJandexIndex(IndexView)
	 * @see #buildMissingDiscoveredJandexIndexes(boolean)
	 */
	AnnotationMappingConfigurationContext discoverJandexIndexesFromAddedTypes(boolean enabled);

	/**
	 * @param enabled {@code true} if Hibernate Search should automatically build Jandex Indexes
	 * when {@link #discoverJandexIndexesFromAddedTypes(boolean) discovering Jandex indexes}.
	 * {@code false} if Hibernate Search should ignore JARs without a Jandex index.
	 * @return {@code this}, for method chaining.
	 * @see #discoverJandexIndexesFromAddedTypes(boolean)
	 * @see #addJandexIndex(IndexView)
	 */
	AnnotationMappingConfigurationContext buildMissingDiscoveredJandexIndexes(boolean enabled);

	/**
	 * @param enabled {@code true} if Hibernate Search should automatically process mapping annotations
	 * on types referenced in the mapping of other types (e.g. the target of an {@link IndexedEmbedded}, ...).
	 * {@code false} if that discovery should be disabled.
	 * @return {@code this}, for method chaining.
	 */
	AnnotationMappingConfigurationContext discoverAnnotationsFromReferencedTypes(boolean enabled);

	/**
	 * @param annotatedType A type to scan for annotations.
	 * @return {@code this}, for method chaining.
	 */
	AnnotationMappingConfigurationContext add(Class<?> annotatedType);

	/**
	 * @param annotatedTypes A set of types to scan for annotations.
	 * @return {@code this}, for method chaining.
	 */
	AnnotationMappingConfigurationContext add(Set<Class<?>> annotatedTypes);

	/**
	 * @param jandexIndex A Jandex index to use when
	 * {@link #discoverAnnotatedTypesFromRootMappingAnnotations(boolean)} discovering annotated types that are also annotated with root mapping annotations}.
	 * @return {@code this}, for method chaining.
	 * @see #discoverAnnotatedTypesFromRootMappingAnnotations(boolean)
	 * @see #discoverJandexIndexesFromAddedTypes(boolean)
	 */
	@Incubating
	AnnotationMappingConfigurationContext addJandexIndex(IndexView jandexIndex);

}
