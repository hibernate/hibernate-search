/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.common.ValueConvert;
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
	 * The actual type of the reference depends on the mapper used to create the query:
	 * the ORM mapper will return a class/identifier pair, for example.
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
	 * @param <I> The expected type of the identifier
	 * @param identifierType The expected type of the identifier
	 * @return A DSL step where the "id" projection can be defined in more details.
	 * @throws SearchException if the identifier type doesn't match
	 */
	<I> IdProjectionOptionsStep<?, I> id(Class<I> identifierType);

	/**
	 * Project to the entity was originally indexed.
	 * <p>
	 * The actual type of the entity depends on the mapper used to create the query
	 * and on the indexes targeted by your query:
	 * the ORM mapper will return a managed entity loaded from the database, for example.
	 *
	 * @return A DSL step where the "entity" projection can be defined in more details.
	 */
	EntityProjectionOptionsStep<?, E> entity();

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
	 * Starts the definition of a composite projection,
	 * which will combine multiple given projections.
	 *
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 */
	CompositeProjectionComponent1Step composite();

	/**
	 * Create a projection that will compose a {@link List} based on the given projections.
	 *
	 * @param projections The projections used to populate the list, in order.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 */
	default CompositeProjectionOptionsStep<?, List<?>> composite(SearchProjection<?>... projections) {
		return composite( Function.identity(), projections );
	}

	/**
	 * Create a projection that will compose a {@link List} based on the given almost-built projections.
	 *
	 * @param dslFinalSteps The final steps in the projection DSL allowing the retrieval of {@link SearchProjection}s.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 */
	default CompositeProjectionOptionsStep<?, List<?>> composite(ProjectionFinalStep<?>... dslFinalSteps) {
		return composite( Function.identity(), dslFinalSteps );
	}

	/**
	 * Create a projection that will compose a custom object based on the given projections.
	 *
	 * @param transformer The function that will transform the list of projected elements into a custom object.
	 * @param projections The projections used to populate the list, in order.
	 * @param <T> The type of the custom object composing the projected elements.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 */
	<T> CompositeProjectionOptionsStep<?, T> composite(Function<List<?>, T> transformer, SearchProjection<?>... projections);

	/**
	 * Create a projection that will compose a custom object based on the given almost-built projections.
	 *
	 * @param transformer The function that will transform the projected element into a custom object.
	 * @param dslFinalSteps The final steps in the projection DSL allowing the retrieval of {@link SearchProjection}s.
	 * @param <T> The type of the custom object composing the projected elements.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 */
	default <T> CompositeProjectionOptionsStep<?, T> composite(Function<List<?>, T> transformer,
			ProjectionFinalStep<?>... dslFinalSteps) {
		SearchProjection<?>[] projections = new SearchProjection<?>[dslFinalSteps.length];
		for ( int i = 0; i < dslFinalSteps.length; i++ ) {
			projections[i] = dslFinalSteps[i].toProjection();
		}
		return composite( transformer, projections );
	}

	/**
	 * Create a projection that will compose a custom object based on one given projection.
	 *
	 * @param transformer The function that will transform the projected element into a custom object.
	 * @param projection The original projection used to produce the element passed to the transformer.
	 * @param <P> The type of the element passed to the transformer.
	 * @param <T> The type of the custom object composing the projected element.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 */
	<P, T> CompositeProjectionOptionsStep<?, T> composite(Function<P, T> transformer, SearchProjection<P> projection);

	/**
	 * Create a projection that will compose a custom object based on one almost-built projection.
	 *
	 * @param transformer The function that will transform the projected element into a custom object.
	 * @param dslFinalStep The final step in the projection DSL allowing the retrieval of the {@link SearchProjection}
	 * that will be used to produce the element passed to the transformer.
	 * @param <P> The type of the element passed to the transformer.
	 * @param <T> The type of the custom object composing the projected element.
	 * @return A DSL step where the "composite" projection can be defined in more details.
	 */
	default <P, T> CompositeProjectionOptionsStep<?, T> composite(Function<P, T> transformer, ProjectionFinalStep<P> dslFinalStep) {
		return composite( transformer, dslFinalStep.toProjection() );
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
	 */
	<P1, P2, T> CompositeProjectionOptionsStep<?, T> composite(BiFunction<P1, P2, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2);

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
	 */
	default <P1, P2, T> CompositeProjectionOptionsStep<?, T> composite(BiFunction<P1, P2, T> transformer,
			ProjectionFinalStep<P1> dslFinalStep1, ProjectionFinalStep<P2> dslFinalStep2) {
		return composite(
				transformer,
				dslFinalStep1.toProjection(), dslFinalStep2.toProjection()
		);
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
	 */
	<P1, P2, P3, T> CompositeProjectionOptionsStep<?, T> composite(TriFunction<P1, P2, P3, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3);

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
	 */
	default <P1, P2, P3, T> CompositeProjectionOptionsStep<?, T> composite(TriFunction<P1, P2, P3, T> transformer,
			ProjectionFinalStep<P1> dslFinalStep1, ProjectionFinalStep<P2> dslFinalStep2,
			ProjectionFinalStep<P3> dslFinalStep3) {
		return composite(
				transformer,
				dslFinalStep1.toProjection(), dslFinalStep2.toProjection(), dslFinalStep3.toProjection()
		);
	}

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

}
