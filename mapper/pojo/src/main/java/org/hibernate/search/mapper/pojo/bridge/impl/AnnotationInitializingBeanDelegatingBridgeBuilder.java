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
import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.util.impl.GenericTypeContext;
import org.hibernate.search.mapper.pojo.util.impl.ReflectionUtils;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A bridge builder that upon building retrieves an {@link AnnotationBridgeBuilder} from the bean provider,
 * initializes it using a pre-defined annotation, and then delegates to that bridge builder.
 *
 * @param <B> The type of bridges returned by this builder.
 * @param <A> The type of annotations accepted by the delegate {@link AnnotationBridgeBuilder}.
 */
@SuppressWarnings("rawtypes") // Clients cannot provide a level of guarantee stronger than raw types
public final class AnnotationInitializingBeanDelegatingBridgeBuilder<B, A extends Annotation>
		implements BridgeBuilder<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanReference<? extends AnnotationBridgeBuilder> delegateReference;
	private final Class<B> expectedBridgeType;
	private final A annotation;

	public AnnotationInitializingBeanDelegatingBridgeBuilder(
			BeanReference<? extends AnnotationBridgeBuilder> delegateReference,
			Class<B> expectedBridgeType,
			A annotation) {
		this.delegateReference = delegateReference;
		this.expectedBridgeType = expectedBridgeType;
		this.annotation = annotation;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[delegateReference=" + delegateReference + ", annotation=" + annotation + "]";
	}

	@Override
	public BeanHolder<? extends B> build(BridgeBuildContext buildContext) {
		BeanHolder<?> bridgeHolder;
		try ( BeanHolder<? extends AnnotationBridgeBuilder> delegateHolder =
				delegateReference.getBean( buildContext.getBeanProvider() ) ) {
			bridgeHolder = doBuild( buildContext, delegateHolder.get() );
		}

		expectedBridgeType.cast( bridgeHolder.get() );
		@SuppressWarnings( "unchecked" ) // The cast above is enough, since BeanHolder must return the same instance for each call to get()
		BeanHolder<? extends B> castedBridgeHolder = (BeanHolder<? extends B>) bridgeHolder;
		return castedBridgeHolder;
	}

	private BeanHolder<?> doBuild(BridgeBuildContext buildContext, AnnotationBridgeBuilder<?, ?> delegate) {
		Class<?> annotationType = annotation.annotationType();
		GenericTypeContext bridgeTypeContext = new GenericTypeContext( delegate.getClass() );
		Class<?> builderAnnotationType = bridgeTypeContext.resolveTypeArgument( AnnotationBridgeBuilder.class, 1 )
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
		AnnotationBridgeBuilder<?, ? super A> castedDelegate = (AnnotationBridgeBuilder<?, ? super A>) delegate;
		castedDelegate.initialize( annotation );
		return castedDelegate.build( buildContext );
	}

}
