/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.hcann.spi;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

public abstract class AbstractPojoHCAnnPropertyModel<T, I extends AbstractPojoHCAnnBootstrapIntrospector>
		implements PojoPropertyModel<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final I introspector;
	protected final AbstractPojoHCAnnRawTypeModel<?, I> holderTypeModel;

	protected final String name;
	/**
	 * The declared XProperties for this property in the holder type.
	 * May be empty if this property is declared in a supertype of the holder type
	 * and not overridden in the holder type.
	 */
	protected final List<XProperty> declaredXProperties;
	private final List<Member> members;

	private ValueReadHandle<T> handleCache;
	private PojoTypeModel<T> typeModelCache;
	private Member memberCache;

	public AbstractPojoHCAnnPropertyModel(I introspector, AbstractPojoHCAnnRawTypeModel<?, I> holderTypeModel,
			String name, List<XProperty> declaredXProperties, List<Member> members) {
		this.introspector = introspector;
		this.holderTypeModel = holderTypeModel;
		this.name = name;
		this.declaredXProperties = declaredXProperties;
		this.members = members;
		Contracts.assertNotNullNorEmpty( members, "members" );
	}

	@Override
	public final String name() {
		return name;
	}

	@Override
	public final Stream<Annotation> annotations() {
		return declaredXProperties.stream().flatMap( introspector::annotations );
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
				throw log.errorRetrievingPropertyTypeModel( name(), holderTypeModel, e );
			}
		}
		return typeModelCache;
	}

	@Override
	public final ValueReadHandle<T> handle() {
		if ( handleCache == null ) {
			try {
				handleCache = createHandle( member() );
			}
			catch (ReflectiveOperationException | RuntimeException e) {
				throw log.errorRetrievingPropertyTypeModel( name(), holderTypeModel, e );
			}
		}
		return handleCache;
	}

	protected final Member member() {
		if ( memberCache == null ) {
			memberCache = members.get( 0 );
			if ( members.size() > 1 ) {
				log.arbitraryMemberSelection( holderTypeModel, name, memberCache, members.subList( 1, members.size() ) );
			}
		}
		return memberCache;
	}

	protected abstract ValueReadHandle<T> createHandle(Member member) throws ReflectiveOperationException;

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
