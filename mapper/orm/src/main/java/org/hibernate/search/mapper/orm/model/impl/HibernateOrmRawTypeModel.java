/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.hcann.spi.PojoCommonsAnnotationsHelper;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.JavaClassPojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class HibernateOrmRawTypeModel<T> implements PojoRawTypeModel<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmBootstrapIntrospector introspector;
	private final PojoRawTypeIdentifier<T> typeIdentifier;
	private final XClass xClass;
	private final HibernateOrmBasicTypeMetadata ormTypeMetadata;
	private final RawTypeDeclaringContext<T> rawTypeDeclaringContext;
	private final PojoCaster<T> caster;

	private final Map<String, HibernateOrmPropertyModel<?>> propertyModelCache = new HashMap<>();

	private List<PojoPropertyModel<?>> declaredProperties;

	private Map<String, XProperty> declaredFieldAccessXPropertiesByName;
	private Map<String, XProperty> declaredMethodAccessXPropertiesByName;

	HibernateOrmRawTypeModel(HibernateOrmBootstrapIntrospector introspector,
			PojoRawTypeIdentifier<T> typeIdentifier,
			HibernateOrmBasicTypeMetadata ormTypeMetadata, RawTypeDeclaringContext<T> rawTypeDeclaringContext) {
		this.introspector = introspector;
		this.typeIdentifier = typeIdentifier;
		this.xClass = introspector.toXClass( typeIdentifier.getJavaClass() );
		this.ormTypeMetadata = ormTypeMetadata;
		this.rawTypeDeclaringContext = rawTypeDeclaringContext;
		this.caster = new JavaClassPojoCaster<>( typeIdentifier.getJavaClass() );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		HibernateOrmRawTypeModel<?> that = (HibernateOrmRawTypeModel<?>) o;
		/*
		 * We need to take the introspector into account, so that the engine does not confuse
		 * type models from different mappers during bootstrap.
		 */
		return Objects.equals( introspector, that.introspector ) &&
				Objects.equals( typeIdentifier, that.typeIdentifier );
	}

	@Override
	public int hashCode() {
		return Objects.hash( introspector, typeIdentifier );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + typeIdentifier + "]";
	}

	@Override
	public PojoRawTypeIdentifier<T> getTypeIdentifier() {
		return typeIdentifier;
	}

	@Override
	public String getName() {
		return typeIdentifier.toString();
	}

	@Override
	public boolean isAbstract() {
		return xClass.isAbstract();
	}

	@Override
	public boolean isSubTypeOf(MappableTypeModel other) {
		return other instanceof HibernateOrmRawTypeModel
				&& ( (HibernateOrmRawTypeModel<?>) other ).xClass.isAssignableFrom( xClass );
	}

	@Override
	public PojoRawTypeModel<? super T> getRawType() {
		return this;
	}

	@Override
	public Stream<HibernateOrmRawTypeModel<? super T>> getAscendingSuperTypes() {
		return introspector.getAscendingSuperTypes( xClass );
	}

	@Override
	public Stream<HibernateOrmRawTypeModel<? super T>> getDescendingSuperTypes() {
		return introspector.getDescendingSuperTypes( xClass );
	}

	@Override
	public Stream<Annotation> getAnnotations() {
		return introspector.getAnnotations( xClass );
	}

	@Override
	public final PojoPropertyModel<?> getProperty(String propertyName) {
		PojoPropertyModel<?> propertyModel = getPropertyOrNull( propertyName );
		if ( propertyModel == null ) {
			throw log.cannotFindReadableProperty( this, propertyName );
		}
		return propertyModel;
	}

	@Override
	public Stream<PojoPropertyModel<?>> getDeclaredProperties() {
		if ( declaredProperties == null ) {
			// TODO HSEARCH-3056 remove lambdas if possible
			declaredProperties = Stream.concat(
					getDeclaredFieldAccessXPropertiesByName().keySet().stream(),
					getDeclaredMethodAccessXPropertiesByName().keySet().stream()
			)
					.distinct()
					.map( this::getPropertyOrNull )
					.filter( Objects::nonNull )
					.collect( Collectors.toList() );
		}
		return declaredProperties.stream();
	}

	@Override
	public PojoCaster<T> getCaster() {
		return caster;
	}

	RawTypeDeclaringContext<T> getRawTypeDeclaringContext() {
		return rawTypeDeclaringContext;
	}

	private Map<String, XProperty> getDeclaredFieldAccessXPropertiesByName() {
		if ( declaredFieldAccessXPropertiesByName == null ) {
			declaredFieldAccessXPropertiesByName =
					introspector.getDeclaredFieldAccessXPropertiesByName( xClass );
		}
		return declaredFieldAccessXPropertiesByName;
	}

	private Map<String, XProperty> getDeclaredMethodAccessXPropertiesByName() {
		if ( declaredMethodAccessXPropertiesByName == null ) {
			declaredMethodAccessXPropertiesByName =
					introspector.getDeclaredMethodAccessXPropertiesByName( xClass );
		}
		return declaredMethodAccessXPropertiesByName;
	}

	private HibernateOrmPropertyModel<?> getPropertyOrNull(String propertyName) {
		return propertyModelCache.computeIfAbsent( propertyName, this::createPropertyModel );
	}

	private HibernateOrmPropertyModel<?> createPropertyModel(String propertyName) {
		List<XProperty> declaredXProperties = new ArrayList<>( 2 );
		// Add the method first on purpose: the first XProperty may be used as a default to create the value accessor handle
		XProperty methodAccessXProperty = getDeclaredMethodAccessXPropertiesByName().get( propertyName );
		if ( methodAccessXProperty != null ) {
			declaredXProperties.add( methodAccessXProperty );
		}
		XProperty fieldAccessXProperty = getDeclaredFieldAccessXPropertiesByName().get( propertyName );
		if ( fieldAccessXProperty != null ) {
			declaredXProperties.add( fieldAccessXProperty );
		}

		HibernateOrmBasicPropertyMetadata ormPropertyMetadata;
		if ( ormTypeMetadata == null ) {
			// There isn't any Hibernate ORM metadata for this type
			ormPropertyMetadata = null;
		}
		else {
			ormPropertyMetadata = ormTypeMetadata.getPropertyMetadataOrNull( propertyName );
		}

		Member member = findPropertyMember(
				propertyName, methodAccessXProperty, fieldAccessXProperty, ormPropertyMetadata
		);

		if ( member == null ) {
			return null;
		}

		return new HibernateOrmPropertyModel<>(
				introspector, this, propertyName,
				declaredXProperties, ormPropertyMetadata, member
		);
	}

	private Member findPropertyMember(String propertyName,
			XProperty methodAccessXProperty, XProperty fieldAccessXProperty,
			HibernateOrmBasicPropertyMetadata propertyMetadataFromHibernateOrmMetamodel) {
		if ( propertyMetadataFromHibernateOrmMetamodel != null ) {
			Member memberFromHibernateOrmMetamodel = propertyMetadataFromHibernateOrmMetamodel.getMember();
			/*
			 * Hibernate ORM has metadata for this property,
			 * which means this property is persisted.
			 *
			 * Hibernate ORM might return us the member as declared in a supertype,
			 * in which case the type of that member will not be up-to-date.
			 * Thus we try to get the overridden member declared in the current type,
			 * and failing that we look for the member in supertypes.
			 *
			 * We still try to comply with JPA's configured access type,
			 * which explains the two if/else branches below.
			 */
			if ( memberFromHibernateOrmMetamodel instanceof Method ) {
				return methodAccessXProperty == null ? memberFromHibernateOrmMetamodel : PojoCommonsAnnotationsHelper.getUnderlyingMember( methodAccessXProperty );
			}
			else if ( memberFromHibernateOrmMetamodel instanceof Field ) {
				return fieldAccessXProperty == null ? memberFromHibernateOrmMetamodel : PojoCommonsAnnotationsHelper.getUnderlyingMember( fieldAccessXProperty );
			}
			else {
				/*
				 * We don't have a declared XProperty for this member in the current type.
				 * Try to find the member used to access the same property in the closest supertype.
				 */
				return getPropertyMemberFromParentTypes( propertyName );
			}
		}
		else {
			/*
			 * Hibernate ORM doesn't have any metadata for this property,
			 * which means this property is transient.
			 * We don't need to worry about JPA's access type.
			 */
			if ( methodAccessXProperty != null ) {
				// We managed to find a declared, method-access XProperty on the current type. Use it.
				return PojoCommonsAnnotationsHelper.getUnderlyingMember( methodAccessXProperty );
			}
			else if ( fieldAccessXProperty != null ) {
				// We managed to find a declared, field-access XProperty on the current type. Use it.
				return PojoCommonsAnnotationsHelper.getUnderlyingMember( fieldAccessXProperty );
			}
			else {
				/*
				 * We did not manage to find a declared XProperty on the current type.
				 * Either the property is declared in a supertype, or it does not exist.
				 * Try to find the member used to access the same property in the closest supertype.
				 */
				return getPropertyMemberFromParentTypes( propertyName );
			}
		}
	}

	private Member getPropertyMemberFromParentTypes(String propertyName) {
		// TODO HSEARCH-3056 remove lambdas if possible
		return getAscendingSuperTypes()
				.skip( 1 ) // Ignore self
				.map( type -> type.getPropertyOrNull( propertyName ) )
				.filter( Objects::nonNull )
				.findFirst()
				.map( HibernateOrmPropertyModel::getMember )
				.orElse( null );
	}

}
