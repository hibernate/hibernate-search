/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.models.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.models.spi.MemberDetails;
import org.hibernate.search.mapper.pojo.logging.impl.MappingLog;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

public abstract class AbstractPojoModelsPropertyModel<
		T,
		I extends AbstractPojoModelsBootstrapIntrospector,
		H extends ValueReadHandle<T>>
		implements PojoPropertyModel<T> {

	protected final I introspector;
	protected final AbstractPojoModelsRawTypeModel<?, I, ?> holderTypeModel;

	protected final String name;
	/**
	 * The declared properties for this property in the holder type.
	 * May be empty if this property is declared in a supertype of the holder type
	 * and not overridden in the holder type.
	 */
	protected final List<MemberDetails> declaredProperties;
	private final List<Member> members;

	private H handleCache;
	private PojoTypeModel<T> typeModelCache;
	private Member memberCache;

	public AbstractPojoModelsPropertyModel(I introspector, AbstractPojoModelsRawTypeModel<?, I, ?> holderTypeModel,
			String name, List<MemberDetails> declaredProperties, List<Member> members) {
		Contracts.assertNotNullNorEmpty( members, "members" );
		this.introspector = introspector;
		this.holderTypeModel = holderTypeModel;
		this.name = name;
		this.declaredProperties = declaredProperties;
		this.members = members;
	}

	@Override
	public final String name() {
		return name;
	}

	@Override
	public final Stream<Annotation> annotations() {
		return declaredProperties.stream().flatMap( introspector::annotations );
	}

	@Override
	/*
	 * The cast is safe as long as both type parameter T and getterGenericReturnType
	 * match the actual type for this property.
	 */
	@SuppressWarnings("unchecked")
	public final PojoTypeModel<T> typeModel() {
		if ( typeModelCache == null ) {
			try {
				typeModelCache = (PojoTypeModel<T>) holderTypeModel.rawTypeDeclaringContext
						.memberTypeReference( getterGenericReturnType() );
			}
			catch (RuntimeException e) {
				throw MappingLog.INSTANCE.errorRetrievingPropertyTypeModel( name(), holderTypeModel, e );
			}
		}
		return typeModelCache;
	}

	@Override
	public final H handle() {
		if ( handleCache == null ) {
			try {
				handleCache = createHandle( member() );
			}
			catch (ReflectiveOperationException | RuntimeException e) {
				throw MappingLog.INSTANCE.errorRetrievingPropertyTypeModel( name(), holderTypeModel, e );
			}
		}
		return handleCache;
	}

	protected final Member member() {
		if ( memberCache == null ) {
			memberCache = members.get( 0 );
			if ( members.size() > 1 ) {
				MappingLog.INSTANCE.arbitraryMemberSelection( holderTypeModel, name, memberCache,
						members.subList( 1, members.size() ) );
			}
		}
		return memberCache;
	}

	protected abstract H createHandle(Member member) throws ReflectiveOperationException;

	final Type getterGenericReturnType() {
		Member member = member();
		// Try to preserve generics information if possible
		if ( member instanceof Method ) {
			return ( (Method) member ).getGenericReturnType();
		}
		else if ( member instanceof Field ) {
			return ( (Field) member ).getGenericType();
		}
		else {
			throw new AssertionFailure(
					"Unexpected type for a " + Member.class.getName() + ": " + member
							+ " has type " + ( member == null ? null : member.getClass() )
			);
		}
	}

}
