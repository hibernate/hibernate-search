/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.search.predicate.factories.FilterFactory;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextFilter;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.FilterFactoryRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FilterParam;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.FilterBinder;

public final class FullTextFilterProcessor implements TypeMappingAnnotationProcessor<FullTextFilter> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public void process(TypeMappingStep mapping, FullTextFilter annotation, TypeMappingAnnotationProcessorContext context) {
		String name = annotation.name();

		Map<String, Object> filterParams = new LinkedHashMap<>();
		FilterParam[] params = annotation.params();
		if ( params != null ) {
			for ( FilterParam param : params ) {
				filterParams.put( param.name(), param.value() );
			}
		}
		FilterBinder binder = createFactoryBinder( annotation.factory(), context, filterParams );

		mapping.filter( name, binder );
	}

	private FilterBinder createFactoryBinder(FilterFactoryRef binderReferenceAnnotation, MappingAnnotationProcessorContext context, Map<String, Object> filterParams) {
		Optional<BeanReference<? extends FilterFactory>> binderReference = context.toBeanReference( FilterFactory.class,
			FilterFactoryRef.UndefinedBinderImplementationType.class,
			binderReferenceAnnotation.type(), binderReferenceAnnotation.name()
		);

		if ( !binderReference.isPresent() ) {
			throw log.missingBinderReferenceInBinding();
		}

		return new BeanBinder( binderReference.get(), filterParams );
	}

}
