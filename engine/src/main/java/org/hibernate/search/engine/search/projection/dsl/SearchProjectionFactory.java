/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.function.TriFunction;

/**
 * A factory for search projections.
 *
 * <h2 id="field-paths">Field paths</h2>
 *
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
 * @param <R> The type of entity references, i.e. the type of objects returned for
 * {@link #entityReference() entity reference projections}.
 * @param <E> The type of entities, i.e. the type of objects returned for
 * {@link #entity() entity projections}.
 */
public interface SearchProjectionFactory<R, E> {

	/**
	 * Project the match to a {@link DocumentReference}.
	 *
	 * @return A DSL step where the "document reference" projection can be defined in more details.
	 */
	DocumentReferenceProjectionOptionsStep<?> documentReference();

	/**
	 * Project to a reference to the entity that was originally indexed.
	 * <p>
	 * Entity references are instances of type {@link EntityReference},
	 * but some mappers may expose a different type for backwards compatibility reasons.
	 * {@link EntityReference} should be favored wherever possible
	 * as mapper-specific types will eventually be removed.
	 *
	 * @return A DSL step where the "entity reference" projection can be defined in more details.
	 */
	EntityReferenceProjectionOptionsStep<?, R> entityReference();

	/**
	 * Project to the identifier of the referenced entity,
	 * i.e. the value of the property marked as {@code @DocumentId}.
	 *
	 * @return A DSL step where the "id" projection can be defined in more details.
	 */
	default IdProjectionOptionsStep<?, Object> id() {
		return id( Object.class );
	}

	/**
	 * Project to the identifier of the referenced entity,
	 * i.e. the value of the property marked as {@code @DocumentId}.
	 *
	 * @param <I> The requested type for returned identifiers.
	 * @param requestedIdentifierType The requested type for returned identifiers.
	 * Must be exactly the type of identifiers of the entity types targeted by the search, or a supertype.
	 * @return A DSL step where the "id" projection can be defined in more details.
	 * @throws SearchException if the identifier type doesn't match
	 */
	<I> IdProjectionOptionsStep<?, I> id(Class<I> requestedIdentifierType);

	/**
	 * Project to the entity that was originally indexed.
	 * <p>
	 * The actual type of the entity depends on the mapper used to create the query
	 * and on the indexes targeted by your query:
	 * the Hibernate ORM mapper will return a managed entity loaded from the database, for example.
	 *
	 * @return A DSL step where the "entity" projection can be defined in more details.
	 */
	EntityProjectionOptionsStep<?, E> entity();

	/**
	 * Project to the entity that was originally indexed.
	 * <p>
	 * The expected type will be checked against the actual type of the entity,
	 * which depends on the mapper used to create the query
	 * and on the indexes targeted by your query:
	 * the Hibernate ORM mapper will return a managed entity loaded from the database, for example.
	 *
	 * @param requestedEntityType The requested type for returned entities.
	 * Must be exactly the type of entities targeted by the search, or a supertype.
	 * @return A DSL step where the "entity" projection can be defined in more details.
	 */
	<T> EntityProjectionOptionsStep<?, T> entity(Class<T> requestedEntityType);

	/**
	 * Project to the value of a field in the indexed document.
	 * <p>
	 * This method will apply projection converters on data fetched from the backend.
	 * See {@link ValueConvert#YES}.
	 *
	 * @param fieldPath The <a href="#field-paths">path</a> to the index field whose value will be extracted.
	 * @param type The resulting type of the projection.
	 * @param <T> The resulting type of the projection.
	 * @return A DSL step where the "field" projection can be defined in more details.
	 */
	default <T> FieldProjectionValueStep<?, T> field(String fieldPath, Class<T> type) {
		return field( fieldPath, type, ValueConvert.YES );
	}

	/**
	 * Project to the value of a field in the indexed document.
	 *
	 * @param fieldPath The <a href="#field-paths">path</a> to the index field whose value will be extracted.
	 * @param type The resulting type of the projection.
	 * @param <T> The resulting type of the projection.
	 * @param convert Controls how the data fetched from the backend should be converted.
	 * See {@link ValueConvert}.
	 * @return A DSL step where the "field" projection can be defined in more details.
	 */
	<T> FieldProjectionValueStep<?, T> field(String fieldPath, Class<T> type, ValueConvert convert);

	/**
	 * Project to the value of a field in the indexed document, without specifying a type.
	 * <p>
	 * This method will apply projection converters on data fetched from the backend.
	 * See {@link ValueConvert#YES}.
	 *
	 * @param fieldPath The <a href="#field-paths">path</a> to the index field whose value will be extracted.
	 * @return A DSL step where the "field" projection can be defined in more details.
	 */
	default FieldProjectionValueStep<?, Object> field(String fieldPath) {
		return field( fieldPath, ValueConvert.YES );
	}

	/**
	 * Project to the value of a field in the indexed document, without specifying a type.
	 *
	 * @param fieldPath The <a href="#field-paths">path</a> to the index field whose value will be extracted.
	 * @param convert Controls how the data fetched from the backend should be converted.
	 * See {@link ValueConvert}.
	 * @return A DSL step where the "field" projection can be defined in more details.
	 */
	FieldProjectionValueStep<?, Object> field(String fieldPath, ValueConvert convert);

	/**
	 * Project on the score of the hit.
	 *
	 * @return A DSL step where the "score" projection can be defined in more details.
	 */
	ScoreProjectionOptionsStep<?> score();

	/**
	 * Project on the distance from the center to a {@link GeoPoint} field.
	 *
	 * @param fieldPath The <a href="#field-paths">path</a> to the index field containing the location
	 * to compute the distance from.
	 * @param center The center to compute the distance from.
	 * @return A DSL step where the "distance" projection can be defined in more details.
	 */
	DistanceToFieldProjectionValueStep<?, Double> distance(String fieldPath, GeoPoint center);

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
	 * @param objectFieldPath The <a href="#field-paths">path</a> to the object field whose object(s) will be extracted.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 */
	CompositeProjectionInnerStep object(String objectFieldPath);

	/**
	 * Starts the definition of a composite projection,
	 * which will combine multiple given projections.
	 * <p>
	 * On contrary to the {@link #object(String)  object projection},
	 * a composite projection is not bound to a specific object field,
	 * and thus it will always yield one and only one value,
	 * regardless of whether {@link CompositeProjectionValueStep#multi()} is called.
	 *
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 */
	CompositeProjectionInnerStep composite();

	/**
	 * Create a projection that will compose a {@link List} based on the given projections.
	 *
	 * @param projections The projections used to populate the list, in order.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 */
	CompositeProjectionValueStep<?, List<?>> composite(SearchProjection<?>... projections);

	/**
	 * Create a projection that will compose a {@link List} based on the given almost-built projections.
	 *
	 * @param dslFinalSteps The final steps in the projection DSL allowing the retrieval of {@link SearchProjection}s.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 */
	default CompositeProjectionValueStep<?, List<?>> composite(ProjectionFinalStep<?>... dslFinalSteps) {
		SearchProjection<?>[] projections = new SearchProjection<?>[dslFinalSteps.length];
		for ( int i = 0; i < dslFinalSteps.length; i++ ) {
			projections[i] = dslFinalSteps[i].toProjection();
		}
		return composite( projections );
	}

	/**
	 * Create a projection that will compose a custom object based on the given projections.
	 *
	 * @param transformer The function that will transform the list of projected elements into a custom object.
	 * @param projections The projections used to populate the list, in order.
	 * @param <T> The type of the custom object composing the projected elements.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 * @deprecated Use {@code .composite().from( projections ).asList( transformer )} instead.
	 */
	@Deprecated
	default <T> CompositeProjectionOptionsStep<?, T> composite(Function<List<?>, T> transformer,
			SearchProjection<?>... projections) {
		return composite().from( projections ).asList( transformer );
	}

	/**
	 * Create a projection that will compose a custom object based on the given almost-built projections.
	 *
	 * @param transformer The function that will transform the projected element into a custom object.
	 * @param dslFinalSteps The final steps in the projection DSL allowing the retrieval of {@link SearchProjection}s.
	 * @param <T> The type of the custom object composing the projected elements.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 * @deprecated Use {@code .composite().from( dslFinalSteps ).asList( transformer )} instead.
	 */
	@Deprecated
	default <T> CompositeProjectionOptionsStep<?, T> composite(Function<List<?>, T> transformer,
			ProjectionFinalStep<?>... dslFinalSteps) {
		return composite().from( dslFinalSteps ).asList( transformer );
	}

	/**
	 * Create a projection that will compose a custom object based on one given projection.
	 *
	 * @param transformer The function that will transform the projected element into a custom object.
	 * @param projection The original projection used to produce the element passed to the transformer.
	 * @param <P> The type of the element passed to the transformer.
	 * @param <T> The type of the custom object composing the projected element.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 * @deprecated Use {@code .composite().from( projection ).as( transformer )} instead.
	 */
	@Deprecated
	default <P, T> CompositeProjectionOptionsStep<?, T> composite(Function<P, T> transformer, SearchProjection<P> projection) {
		return composite().from( projection ).as( transformer );
	}

	/**
	 * Create a projection that will compose a custom object based on one almost-built projection.
	 *
	 * @param transformer The function that will transform the projected element into a custom object.
	 * @param dslFinalStep The final step in the projection DSL allowing the retrieval of the {@link SearchProjection}
	 * that will be used to produce the element passed to the transformer.
	 * @param <P> The type of the element passed to the transformer.
	 * @param <T> The type of the custom object composing the projected element.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 * @deprecated Use {@code .composite().from( dslFinalStep ).as( transformer )} instead.
	 */
	@Deprecated
	default <P, T> CompositeProjectionOptionsStep<?, T> composite(Function<P, T> transformer,
			ProjectionFinalStep<P> dslFinalStep) {
		return composite().from( dslFinalStep ).as( transformer );
	}

	/**
	 * Create a projection that will compose a custom object based on two given projections.
	 *
	 * @param transformer The function that will transform the projected elements into a custom object.
	 * @param projection1 The projection used to produce the first element passed to the transformer.
	 * @param projection2 The projection used to produce the second element passed to the transformer.
	 * @param <P1> The type of the first element passed to the transformer.
	 * @param <P2> The type of the second element passed to the transformer.
	 * @param <T> The type of the custom object composing the projected elements.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 * @deprecated Use {@code .composite().from( projection1, projection2 ).as( transformer )}
	 * instead.
	 */
	@Deprecated
	default <P1, P2, T> CompositeProjectionOptionsStep<?, T> composite(BiFunction<P1, P2, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		return composite().from( projection1, projection2 ).as( transformer );
	}

	/**
	 * Create a projection that will compose a custom object based on two almost-built projections.
	 *
	 * @param transformer The function that will transform the projected elements into a custom object.
	 * @param dslFinalStep1 The final step in the projection DSL allowing the retrieval of the {@link SearchProjection}
	 * that will be used to produce the first element passed to the transformer.
	 * @param dslFinalStep2 The final step in the projection DSL allowing the retrieval of the {@link SearchProjection}
	 * that will be used to produce the second element passed to the transformer.
	 * @param <P1> The type of the first element passed to the transformer.
	 * @param <P2> The type of the second element passed to the transformer.
	 * @param <T> The type of the custom object composing the projected elements.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 * @deprecated Use {@code .composite().from( dslFinalStep1, dslFinalStep2 ).as( transformer )}
	 * instead.
	 */
	@Deprecated
	default <P1, P2, T> CompositeProjectionOptionsStep<?, T> composite(BiFunction<P1, P2, T> transformer,
			ProjectionFinalStep<P1> dslFinalStep1, ProjectionFinalStep<P2> dslFinalStep2) {
		return composite().from( dslFinalStep1, dslFinalStep2 ).as( transformer );
	}

	/**
	 * Create a projection that will compose a custom object based on three given projections.
	 *
	 * @param transformer The function that will transform the projected elements into a custom object.
	 * @param projection1 The projection used to produce the first element passed to the transformer.
	 * @param projection2 The projection used to produce the second element passed to the transformer.
	 * @param projection3 The projection used to produce the third element passed to the transformer.
	 * @param <P1> The type of the first element passed to the transformer.
	 * @param <P2> The type of the second element passed to the transformer.
	 * @param <P3> The type of the third element passed to the transformer.
	 * @param <T> The type of the custom object composing the projected elements.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 * @deprecated Use {@code .composite().from( projection1, projection2, projection3 ).as( transformer )}
	 * instead.
	 */
	@Deprecated
	default <P1, P2, P3, T> CompositeProjectionOptionsStep<?, T> composite(TriFunction<P1, P2, P3, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		return composite().from( projection1, projection2, projection3 ).as( transformer );
	}

	/**
	 * Create a projection that will compose a custom object based on three almost-built projections.
	 *
	 * @param transformer The function that will transform the projected elements into a custom object.
	 * @param dslFinalStep1 The final step in the projection DSL allowing the retrieval of the {@link SearchProjection}
	 * that will be used to produce the first element passed to the transformer.
	 * @param dslFinalStep2 The final step in the projection DSL allowing the retrieval of the {@link SearchProjection}
	 * that will be used to produce the second element passed to the transformer.
	 * @param dslFinalStep3 The final step in the projection DSL allowing the retrieval of the {@link SearchProjection}
	 * that will be used to produce the third element passed to the transformer.
	 * @param <P1> The type of the first element passed to the transformer.
	 * @param <P2> The type of the second element passed to the transformer.
	 * @param <P3> The type of the third element passed to the transformer.
	 * @param <T> The type of the custom object composing the projected elements.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 * @deprecated Use {@code .composite().from( dslFinalStep1, dslFinalStep2, dslFinalStep3 ).as( transformer )}
	 * instead.
	 */
	@Deprecated
	default <P1, P2, P3, T> CompositeProjectionOptionsStep<?, T> composite(TriFunction<P1, P2, P3, T> transformer,
			ProjectionFinalStep<P1> dslFinalStep1, ProjectionFinalStep<P2> dslFinalStep2,
			ProjectionFinalStep<P3> dslFinalStep3) {
		return composite().from( dslFinalStep1, dslFinalStep2, dslFinalStep3 ).as( transformer );
	}

	/**
	 * Project to a given constant.
	 * <p>
	 * The projection will return the same value for every single hit.
	 *
	 * @param value The constant value that the projection should return.
	 * @return A DSL step where the "entity reference" projection can be defined in more details.
	 */
	<T> ProjectionFinalStep<T> constant(T value);

	/**
	 * Extend the current factory with the given extension,
	 * resulting in an extended factory offering different types of projections.
	 *
	 * @param extension The extension to the projection DSL.
	 * @param <T> The type of factory provided by the extension.
	 * @return The extended factory.
	 * @throws SearchException If the extension cannot be applied (wrong underlying backend, ...).
	 */
	<T> T extension(SearchProjectionFactoryExtension<T, R, E> extension);

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
	<T> SearchProjectionFactoryExtensionIfSupportedStep<T, R, E> extension();

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
	@Incubating
	SearchProjectionFactory<R, E> withRoot(String objectFieldPath);

	/**
	 * @param relativeFieldPath The path to a field, relative to the {@link #withRoot(String) root} of this factory.
	 * @return The absolute path of the field, for use in native projections for example.
	 * Note the path is returned even if the field doesn't exist.
	 */
	@Incubating
	String toAbsolutePath(String relativeFieldPath);

	/**
	 * Project to highlights, i.e. sequences of text that matched the query, extracted from the given field's value.
	 *
	 * @param fieldPath The <a href="#field-paths">path</a> to the index field whose highlights will be extracted.
	 * @return A DSL step where the "highlight" projection can be defined in more details.
	 */
	@Incubating
	HighlightProjectionOptionsStep highlight(String fieldPath);
}
