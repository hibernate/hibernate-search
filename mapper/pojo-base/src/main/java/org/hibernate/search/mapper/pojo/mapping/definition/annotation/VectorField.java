/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.VectorFieldAnnotationProcessor;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Maps an entity property to a vector field in the index.
 * <p>
 * This annotation will work for any {@code float/byte} array.
 * <p>
 * Vector fields are to be used in k-NN search, when the distance between a queried and stored vectors is computed
 * and k nearest vectors are selected as the results.
 *
 * @see org.hibernate.search.engine.search.predicate.dsl.KnnPredicateFieldStep
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(VectorField.List.class)
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = VectorFieldAnnotationProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
@Incubating
public @interface VectorField {

	/**
	 * @return The name of the index field.
	 */
	String name() default "";

	/**
	 * @return Whether this field should be projectable.
	 * @see Projectable
	 */
	Projectable projectable() default Projectable.DEFAULT;

	/**
	 * @return Whether this field should be searchable.
	 * @see Searchable
	 */
	Searchable searchable() default Searchable.DEFAULT;

	/**
	 * @return The size of the vector.
	 */
	int dimension();

	/**
	 * @return How vector similarity is calculated.
	 * @see VectorSimilarity
	 */
	VectorSimilarity vectorSimilarity() default VectorSimilarity.DEFAULT;

	/**
	 * @return A value used instead of null values when indexing.
	 */
	String indexNullAs() default AnnotationDefaultValues.DO_NOT_INDEX_NULL;

	/**
	 * @return The size of the dynamic list used during k-NN graph creation.
	 * Higher values lead to a more accurate graph but slower indexing speed.
	 * Default value is backend-specific.
	 */
	int beamWidth() default AnnotationDefaultValues.DEFAULT_BEAM_WIDTH;

	/**
	 * @return The number of neighbors each node will be connected to in the HNSW graph.
	 * Modifying this value will have an impact on memory consumption.
	 * It is recommended to keep this value between 2 and 100.
	 * Default value is backend-specific.
	 */
	int maxConnections() default AnnotationDefaultValues.DEFAULT_MAX_CONNECTIONS;

	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		VectorField[] value();
	}

}
