/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBridgeBuilder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.reflect.impl.GenericTypeContext;
import org.hibernate.search.util.common.reflect.impl.ReflectionUtils;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A bridge builder that upon building retrieves a delegate bridge builder from the bean provider,
 * initializes it using a pre-defined annotation, and then delegates to that bridge builder.
 *
 * @param <A> The type of annotations accepted by the delegate builder.
 */
@SuppressWarnings("rawtypes") // Clients cannot provide a level of guarantee stronger than raw types
public final class AnnotationInitializingBeanDelegatingBridgeBuilder<A extends Annotation>
		implements TypeBridgeBuilder<A>, PropertyBridgeBuilder<A>, RoutingKeyBridgeBuilder<A> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanReference<?> delegateReference;

	private A annotation;

	public AnnotationInitializingBeanDelegatingBridgeBuilder(BeanReference<?> delegateReference) {
		this.delegateReference = delegateReference;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[delegateReference=" + delegateReference + ", annotation=" + annotation + "]";
	}

	@Override
	public void initialize(A annotation) {
		// Delay initialization to the build() call
		this.annotation = annotation;
	}

	@Override
	public BeanHolder<? extends TypeBridge> buildForType(BridgeBuildContext buildContext) {
		try ( BeanHolder<? extends TypeBridgeBuilder> delegateHolder =
				createDelegate( buildContext, TypeBridgeBuilder.class ) ) {
			@SuppressWarnings("unchecked") // Checked using reflection in createDelegate
			TypeBridgeBuilder<A> castedDelegate = delegateHolder.get();
			castedDelegate.initialize( annotation );
			return castedDelegate.buildForType( buildContext );
		}
	}

	@Override
	public BeanHolder<? extends PropertyBridge> buildForProperty(BridgeBuildContext buildContext) {
		try ( BeanHolder<? extends PropertyBridgeBuilder> delegateHolder =
				createDelegate( buildContext, PropertyBridgeBuilder.class ) ) {
			@SuppressWarnings("unchecked") // Checked using reflection in createDelegate
			PropertyBridgeBuilder<A> castedDelegate = delegateHolder.get();
			castedDelegate.initialize( annotation );
			return castedDelegate.buildForProperty( buildContext );
		}
	}

	@Override
	public BeanHolder<? extends RoutingKeyBridge> buildForRoutingKey(BridgeBuildContext buildContext) {
		try ( BeanHolder<? extends RoutingKeyBridgeBuilder> delegateHolder =
				createDelegate( buildContext, RoutingKeyBridgeBuilder.class ) ) {
			@SuppressWarnings("unchecked") // Checked using reflection in createDelegate
			RoutingKeyBridgeBuilder<A> castedDelegate = delegateHolder.get();
			castedDelegate.initialize( annotation );
			return castedDelegate.buildForRoutingKey( buildContext );
		}
	}

	private <B> BeanHolder<? extends B> createDelegate(BridgeBuildContext buildContext, Class<B> expectedType) {
		BeanResolver beanResolver = buildContext.getBeanResolver();
		BeanHolder<? extends B> delegateHolder = delegateReference.asSubTypeOf( expectedType ).resolve( beanResolver );
		try {
			B delegate = delegateHolder.get();
			Class<?> annotationType = annotation.annotationType();
			GenericTypeContext bridgeTypeContext = new GenericTypeContext( delegate.getClass() );
			Class<?> builderAnnotationType = bridgeTypeContext.resolveTypeArgument( expectedType, 0 )
					.map( ReflectionUtils::getRawType )
					.orElseThrow( () -> new AssertionFailure(
							"Could not auto-detect the annotation type accepted by builder '"
									+ delegate + "'."
									+ " There is a bug in Hibernate Search, please report it."
					) );
			if ( !builderAnnotationType.isAssignableFrom( annotationType ) ) {
				throw log.invalidAnnotationTypeForBuilder( delegate, annotationType );
			}

			return delegateHolder;
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( delegateHolder );
			throw e;
		}
	}

}
