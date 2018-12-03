/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.impl;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;

/**
 * A bridge builder that upon building retrieves a {@link BridgeBuilder} from the bean provider,
 * then delegates to that bridge builder.
 *
 * @param <B> The type of bridges returned by this builder.
 */
@SuppressWarnings("rawtypes") // Clients cannot provide a level of guarantee stronger than raw types
public final class BeanDelegatingBridgeBuilder<B> implements BridgeBuilder<B> {

	private final BeanReference<? extends BridgeBuilder> delegateReference;
	private final Class<B> expectedBridgeType;

	public BeanDelegatingBridgeBuilder(BeanReference<? extends BridgeBuilder> delegateReference,
			Class<B> expectedBridgeType) {
		this.delegateReference = delegateReference;
		this.expectedBridgeType = expectedBridgeType;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[delegateReference=" + delegateReference + "]";
	}

	@Override
	public B build(BridgeBuildContext buildContext) {
		BridgeBuilder<?> delegate = delegateReference.getBean( buildContext.getBeanProvider() );
		return expectedBridgeType.cast( delegate.build( buildContext ) );
	}

}
