/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationMarkerBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuilder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.util.impl.GenericTypeContext;
import org.hibernate.search.mapper.pojo.util.impl.ReflectionUtils;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A marker builder that upon building retrieves an {@link AnnotationMarkerBuilder} from the bean provider,
 * initializes it using a pre-defined annotation, and then delegates to that marker builder.
 *
 * @param <A> The type of annotations accepted by the delegate {@link AnnotationMarkerBuilder}.
 */
@SuppressWarnings("rawtypes") // Clients cannot provide a level of guarantee stronger than raw types
public final class AnnotationInitializingBeanDelegatingMarkerBuilder<A extends Annotation>
		implements MarkerBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
			return doBuild( buildContext, delegateHolder.get() );
		}
	}

	private Object doBuild(MarkerBuildContext buildContext, AnnotationMarkerBuilder<?> delegate) {
		Class<?> annotationType = annotation.annotationType();
		GenericTypeContext bridgeTypeContext = new GenericTypeContext( delegate.getClass() );
		Class<?> builderAnnotationType = bridgeTypeContext.resolveTypeArgument( AnnotationMarkerBuilder.class, 0 )
				.map( ReflectionUtils::getRawType )
				.orElseThrow( () -> new AssertionFailure(
						"Could not auto-detect the annotation type accepted by builder '"
								+ delegate + "'."
								+ " There is a bug in Hibernate Search, please report it."
				) );
		if ( !builderAnnotationType.isAssignableFrom( annotationType ) ) {
			throw log.invalidAnnotationTypeForBuilder( delegate, annotationType );
		}

		@SuppressWarnings("unchecked") // Checked using reflection just above
		AnnotationMarkerBuilder<? super A> castedDelegate = (AnnotationMarkerBuilder<? super A>) delegate;
		castedDelegate.initialize( annotation );
		return castedDelegate.build( buildContext );
	}

}
