/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanDelegatingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingIndexedStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

public class IndexedProcessor implements TypeMappingAnnotationProcessor<Indexed> {

	@Override
	public void process(TypeMappingStep mappingContext, Indexed annotation,
			TypeMappingAnnotationProcessorContext context) {
		String indexName = context.toNullIfDefault( annotation.index(), "" );
		String backendName = context.toNullIfDefault( annotation.backend(), "" );
		TypeMappingIndexedStep indexedStep = mappingContext.indexed().backend( backendName ).index( indexName )
				.enabled( annotation.enabled() );

		RoutingBinderRef routingBinderReferenceAnnotation = annotation.routingBinder();
		Optional<BeanReference<? extends RoutingBinder>> routingBinderReference = context.toBeanReference(
				RoutingBinder.class,
				RoutingBinderRef.UndefinedRoutingBinderImplementationType.class,
				routingBinderReferenceAnnotation.type(), routingBinderReferenceAnnotation.name(),
				routingBinderReferenceAnnotation.retrieval()
		);

		if ( !routingBinderReference.isPresent() ) {
			return;
		}

		Map<String, Object> params = context.toMap( routingBinderReferenceAnnotation.params() );
		indexedStep.routingBinder( new BeanDelegatingBinder( routingBinderReference.get() ), params );
	}
}
