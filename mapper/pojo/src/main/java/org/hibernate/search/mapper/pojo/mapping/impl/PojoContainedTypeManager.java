/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSessionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.impl.common.ToStringTreeAppendable;
import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

public class PojoContainedTypeManager<E> implements AutoCloseable, ToStringTreeAppendable {

	private final Class<E> javaClass;
	private final PojoCaster<E> caster;
	private final PojoImplicitReindexingResolver<E> reindexingResolver;

	public PojoContainedTypeManager(Class<E> javaClass,
			PojoCaster<E> caster,
			PojoImplicitReindexingResolver<E> reindexingResolver) {
		this.javaClass = javaClass;
		this.caster = caster;
		this.reindexingResolver = reindexingResolver;
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "javaClass", javaClass )
				.attribute( "reindexingResolver", reindexingResolver );
	}

	Supplier<E> toEntitySupplier(PojoSessionContext sessionContext, Object entity) {
		PojoRuntimeIntrospector proxyIntrospector = sessionContext.getRuntimeIntrospector();
		return new CachingCastingEntitySupplier<>( caster, proxyIntrospector, entity );
	}

	void resolveEntitiesToReindex(PojoReindexingCollector collector, PojoRuntimeIntrospector runtimeIntrospector,
			Supplier<E> entitySupplier) {
		reindexingResolver.resolveEntitiesToReindex( collector, runtimeIntrospector, entitySupplier.get() );
	}

	ChangesetPojoContainedTypeWorker<E> createWorker(PojoSessionContext sessionContext) {
		return new ChangesetPojoContainedTypeWorker<>(
				this, sessionContext
		);
	}
}
