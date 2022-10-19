/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorHolder;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathOriginalTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.impl.ToStringTreeAppendable;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * A resolver of the inverse side of an association based on an "association state".
 */
public abstract class PojoImplicitReindexingAssociationInverseSideResolverNode<T>
		implements AutoCloseable, ToStringTreeAppendable {

	@SuppressWarnings("unchecked")
	public static <P, V> PojoImplicitReindexingAssociationInverseSideResolverNode<Object> bind(
			ContainerExtractorBinder extractorBinder, BoundPojoModelPathValueNode<?, P, V> path,
			PojoImplicitReindexingAssociationInverseSideResolverNode<? super V> nested) {
		ContainerExtractorHolder<? super P, V> boundExtractor = null;
		try {
			if ( !path.getExtractorPath().isEmpty() ) {
				boundExtractor = extractorBinder.create( path.getBoundExtractorPath() );
			}
			return bind( extractorBinder, path.getParent(),
					boundExtractor == null
							? (PojoImplicitReindexingAssociationInverseSideResolverNode<? super P>) nested
							: new PojoImplicitReindexingAssociationInverseSideResolverContainerElementNode<>(
									boundExtractor, nested ) );

		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( ContainerExtractorHolder::close, boundExtractor )
					.push( PojoImplicitReindexingAssociationInverseSideResolverNode::close, nested );
			throw e;
		}
	}

	private static <T, P> PojoImplicitReindexingAssociationInverseSideResolverNode<Object> bind(ContainerExtractorBinder extractorBinder,
			BoundPojoModelPathPropertyNode<T, P> path, PojoImplicitReindexingAssociationInverseSideResolverNode<? super P> nested) {
		try {
			return bind( extractorBinder, path.getParent(),
					new PojoImplicitReindexingAssociationInverseSideResolverPropertyNode<>(
							path.getPropertyModel().handle(), nested, path.toUnboundPath() ) );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( PojoImplicitReindexingAssociationInverseSideResolverNode::close, nested );
			throw e;
		}
	}

	private static <T> PojoImplicitReindexingAssociationInverseSideResolverNode<Object> bind(ContainerExtractorBinder extractorBinder,
			BoundPojoModelPathTypeNode<T> path,
			PojoImplicitReindexingAssociationInverseSideResolverNode<? super T> nested) {
		try {
			// Casted type nodes are not supported
			BoundPojoModelPathValueNode<?, ?, T> parent = ( (BoundPojoModelPathOriginalTypeNode<T>) path ).getParent();
			if ( parent == null ) {
				return new PojoImplicitReindexingAssociationInverseSideResolverCastedTypeNode<>(
						path.getTypeModel().rawType().caster(), nested );
			}
			else {
				return bind( extractorBinder, parent, nested );
			}
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( PojoImplicitReindexingAssociationInverseSideResolverNode::close, nested );
			throw e;
		}
	}

	@Override
	public final String toString() {
		return new ToStringTreeBuilder().value( this ).toString();
	}

	@Override
	public abstract void close();

	abstract void resolveEntitiesToReindex(PojoReindexingAssociationInverseSideCollector collector, T state,
			PojoImplicitReindexingAssociationInverseSideResolverRootContext context);

}
