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
import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;

/**
 * A bridge builder that upon building retrieves an {@link AnnotationBridgeBuilder} from the bean provider,
 * initializes it using a pre-defined annotation, and then delegates to that bridge builder.
 *
 * @param <B> The type of bridges returned by this builder.
 * @param <A> The type of annotations accepted by the delegate {@link AnnotationBridgeBuilder}.
 */
@SuppressWarnings("rawtypes") // Clients cannot provide a level of guarantee stronger than raw types
public final class AnnotationInitializingBeanDelegatingBridgeBuilder<B, A extends Annotation> implements BridgeBuilder<B> {

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
			/*
			 * TODO HSEARCH-3077 make this raw type use safer by checking the generic parameters of delegate.getClass() somehow,
			 * maybe in a similar way to what we do in PojoIndexModelBinderImpl#addValueBridge,
			 * and throwing an exception with a detailed explanation if something is wrong.
			 */
			delegateHolder.get().initialize( annotation );
			bridgeHolder = delegateHolder.get().build( buildContext );
		}

		expectedBridgeType.cast( bridgeHolder.get() );
		@SuppressWarnings( "unchecked" ) // The cast above is enough, since BeanHolder must return the same instance for each call to get()
		BeanHolder<? extends B> castedBridgeHolder = (BeanHolder<? extends B>) bridgeHolder;
		return castedBridgeHolder;
	}

}
