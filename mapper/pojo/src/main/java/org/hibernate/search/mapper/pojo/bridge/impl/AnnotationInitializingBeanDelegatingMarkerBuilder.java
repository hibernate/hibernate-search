/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.impl;

import java.lang.annotation.Annotation;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationMarkerBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuilder;

/**
 * A marker builder that upon building retrieves an {@link AnnotationMarkerBuilder} from the bean provider,
 * initializes it using a pre-defined annotation, and then delegates to that marker builder.
 *
 * @param <A> The type of annotations accepted by the delegate {@link AnnotationMarkerBuilder}.
 */
@SuppressWarnings("rawtypes") // Clients cannot provide a level of guarantee stronger than raw types
public final class AnnotationInitializingBeanDelegatingMarkerBuilder<A extends Annotation>
		implements MarkerBuilder {

	private final BeanReference<? extends AnnotationMarkerBuilder> delegateReference;
	private final A annotation;

	public AnnotationInitializingBeanDelegatingMarkerBuilder(
			BeanReference<? extends AnnotationMarkerBuilder> delegateReference,
			A annotation) {
		this.delegateReference = delegateReference;
		this.annotation = annotation;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[delegateReference=" + delegateReference + ", annotation=" + annotation + "]";
	}

	@Override
	public Object build(MarkerBuildContext buildContext) {
		try ( BeanHolder<? extends AnnotationMarkerBuilder> delegateHolder =
				delegateReference.getBean( buildContext.getBeanProvider() ) ) {
			/*
			 * TODO HSEARCH-3077 make this raw type use safer by checking the generic parameters of delegate.getClass() somehow,
			 * maybe in a similar way to what we do in PojoIndexModelBinderImpl#addValueBridge,
			 * and throwing an exception with a detailed explanation if something is wrong.
			 */
			@SuppressWarnings("unchecked")
			AnnotationMarkerBuilder<A> castedDelegate = delegateHolder.get();
			castedDelegate.initialize( annotation );
			return castedDelegate.build( buildContext );
		}
	}

}
