/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;


import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.reference.object.ObjectFieldReference;
import org.hibernate.search.engine.search.reference.projection.DistanceProjectionFieldReference;
import org.hibernate.search.engine.search.reference.projection.FieldProjectionFieldReference;
import org.hibernate.search.engine.search.reference.projection.HighlightProjectionFieldReference;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A factory for search projections.
 *
 * <h2 id="field-paths">Field paths</h2>
 * <p>
 * By default, field paths passed to this DSL are interpreted as absolute,
 * i.e. relative to the index root.
 * <p>
 * However, a new, "relative" factory can be created with {@link #withRoot(String)}:
 * the new factory interprets paths as relative to the object field passed as argument to the method.
 * <p>
 * This can be useful when calling reusable methods that can apply the same projection
 * on different object fields that have same structure (same sub-fields).
 * <p>
 * Such a factory can also transform relative paths into absolute paths using {@link #toAbsolutePath(String)};
 * this can be useful for native projections in particular.
 *
 * <h2 id="field-references">Field references</h2>
 * <p>
 * A {@link org.hibernate.search.engine.search.reference field reference} is always represented by the absolute field path and,
 * if applicable, i.e. when a field reference is typed, a combination of the {@link org.hibernate.search.engine.search.common.ValueModel} and the type.
 * <p>
 * Field references are usually accessed from the generated Hibernate Search's static metamodel classes that describe the index structure.
 * Such reference provides the information on which search capabilities the particular index field possesses, and allows switching between different
 * {@link org.hibernate.search.engine.search.common.ValueModel value model representations}.
 *
 * @param <SR> Scope root type.
 * @param <R> The type of entity references, i.e. the type of objects returned for
 * {@link #entityReference() entity reference projections}.
 * @param <E> The type of entities, i.e. the type of objects returned for
 * {@link #entity() entity projections}.
 */
public interface TypedSearchProjectionFactory<SR, R, E> extends SearchProjectionFactory<R, E> {

	/**
	 * Project to the value of a field in the indexed document.
	 *
	 * @param fieldReference The field reference representing a <a href="#field-references">definition</a> of the index field whose value will be extracted.
	 * @param <T> The resulting type of the projection.
	 * @return A DSL step where the "field" projection can be defined in more details.
	 */
	@Incubating
	default <T> FieldProjectionValueStep<?, T> field(FieldProjectionFieldReference<? super SR, T> fieldReference) {
		return field( fieldReference.absolutePath(), fieldReference.projectionType(), fieldReference.valueModel() );
	}

	/**
	 * Project on the distance from the center to a {@link GeoPoint} field.
	 *
	 * @param fieldReference The field reference representing a <a href="#field-references">definition</a> of the index field containing the location
	 * to compute the distance from.
	 * @param center The center to compute the distance from.
	 * @return A DSL step where the "distance" projection can be defined in more details.
	 */
	@Incubating
	default DistanceToFieldProjectionValueStep<?, Double> distance(
			DistanceProjectionFieldReference<? super SR> fieldReference,
			GeoPoint center) {
		return distance( fieldReference.absolutePath(), center );
	}

	/**
	 * Starts the definition of an object projection,
	 * which will yield one value per object in a given object field,
	 * the value being the result of combining multiple given projections
	 * (usually on fields within the object field).
	 * <p>
	 * Compared to the basic {@link #composite() composite projection},
	 * an object projection is bound to a specific object field,
	 * and thus it yields zero, one or many values, as many as there are objects in the targeted object field.
	 * Therefore, you must take care of calling {@link CompositeProjectionValueStep#multi()}
	 * if the object field is multi-valued.
	 *
	 * @param objectFieldReference The field reference representing a <a href="#field-references">definition</a> of the index object field whose object(s) will be extracted.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 */
	@Incubating
	default CompositeProjectionInnerStep object(ObjectFieldReference<? super SR> objectFieldReference) {
		return object( objectFieldReference.absolutePath() );
	}

	/**
	 * Create a DSL step allowing multiple attempts to apply extensions one after the other,
	 * failing only if <em>none</em> of the extensions is supported.
	 * <p>
	 * If you only need to apply a single extension and fail if it is not supported,
	 * use the simpler {@link #extension(SearchProjectionFactoryExtension)} method instead.
	 * <p>
	 * This method is generic, and you should set the generic type explicitly to the expected projected type,
	 * e.g. {@code .<MyProjectedType>extension()}.
	 *
	 * @param <T> The expected projected type.
	 * @return A DSL step.
	 */
	@Override
	<T> SearchProjectionFactoryExtensionIfSupportedStep<SR, T, R, E> extension();

	/**
	 * Create a new projection factory whose root for all paths passed to the DSL
	 * will be the given object field.
	 * <p>
	 * This is used to call reusable methods that can apply the same projection
	 * on different object fields that have same structure (same sub-fields).
	 *
	 * @param objectFieldPath The path from the current root to an object field that will become the new root.
	 * @return A new projection factory using the given object field as root.
	 */
	@Override
	@Incubating
	TypedSearchProjectionFactory<SR, R, E> withRoot(String objectFieldPath);

	/**
	 * Project to highlights, i.e. sequences of text that matched the query, extracted from the given field's value.
	 *
	 * @param fieldReference The field reference representing a <a href="#field-references">definition</a> of the index field whose highlights will be extracted.
	 * @return A DSL step where the "highlight" projection can be defined in more details.
	 */
	@Incubating
	default HighlightProjectionOptionsStep highlight(HighlightProjectionFieldReference<? super SR> fieldReference) {
		return highlight( fieldReference.absolutePath() );
	}
}
