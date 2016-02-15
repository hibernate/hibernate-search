/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.db.events.jpa;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.exception.AssertionFailure;

/**
 * ONLY USED FOR OLD UNIT TESTS!!! class used to parse the JPA annotations in respect to the Hibernate-Search Index
 * annotations. <br>
 * <br>
 * This class also has means to have accessors to the root entities of a specific class. This could be used to propagate
 * events up <br>
 * <br>
 * This could be used to propagate events up to the top entity, but Hibernate-Search takes care of this via the @ContainedIn
 * events, which is neat :). Either way, we still need this class to check whether the classes are annotated properly
 * (every entity has to know its parent via {@link org.hibernate.search.annotations.ContainedIn}
 *
 * @author Martin Braun
 */
//@Deprecated
public class MetaModelParser {

	private static final Logger LOGGER = Logger.getLogger( MetaModelParser.class.getName() );
	// only contains EntityTypes
	private final Map<Class<?>, ManagedType<?>> managedTypes = new HashMap<>();
	private final Map<Class<?>, Boolean> isRootType = new HashMap<>();
	private final Set<Class<?>> totalVisitedEntities = new HashSet<>();
	private final Map<Class<?>, String> idProperties = new HashMap<>();

	private static Map<Class<? extends Annotation>, Set<Attribute<?, ?>>> buildAttributeForAnnotationType(EntityType<?> entType) {
		Map<Class<? extends Annotation>, Set<Attribute<?, ?>>> attributeForAnnotationType = new HashMap<>();
		entType.getAttributes().forEach(
				(declared) -> {
					String propertyName = declared.getName();
					Field field;
					try {
						field = entType.getJavaType().getDeclaredField( propertyName );
					}
					catch (NoSuchFieldException e) {
						throw new AssertionFailure( "expected to find a Field but didn't: " + propertyName );
					}
					Method method = null;
					try {
						// we shouldn't encounter any Booleans but we
						// are nice
						Class<?> type = getEntityTypeClass( declared );
						StringBuilder methodName;
						if ( type.equals( boolean.class ) || type.equals( Boolean.class ) ) {
							methodName = new StringBuilder( "is" );
						}
						else {
							methodName = new StringBuilder( "get" );
						}
						methodName.append( String.valueOf( propertyName.charAt( 0 ) ).toUpperCase( Locale.ROOT ) );
						if ( propertyName.length() > 1 ) {
							methodName.append( propertyName.substring( 1 ) );
						}
						method = entType.getJavaType().getMethod( methodName.toString() );
					}
					catch (NoSuchMethodException e) {
						LOGGER.warning( "no getter for " + propertyName + " found." );
					}
					maybeAddToAttributeMap( declared, field, attributeForAnnotationType, IndexedEmbedded.class );
					if ( method != null ) {
						maybeAddToAttributeMap( declared, method, attributeForAnnotationType, IndexedEmbedded.class );
					}

					maybeAddToAttributeMap( declared, field, attributeForAnnotationType, ContainedIn.class );
					if ( method != null ) {
						maybeAddToAttributeMap( declared, method, attributeForAnnotationType, ContainedIn.class );
					}
				}
		);
		return attributeForAnnotationType;
	}

	private static void maybeAddToAttributeMap(
			Attribute<?, ?> declared,
			Member member,
			Map<Class<? extends Annotation>, Set<Attribute<?, ?>>> attributeForAnnotationType,
			final Class<? extends Annotation> annotationClass) {
		if ( isAnnotationPresent( member, annotationClass ) ) {
			Set<Attribute<?, ?>> list = attributeForAnnotationType.computeIfAbsent(
					annotationClass, (key) -> {
						return new HashSet<>();
					}
			);
			list.add( declared );
		}
	}

	private static Class<?> getEntityTypeClass(Attribute<?, ?> attribute) {
		Class<?> entityTypeClass;
		if ( attribute instanceof PluralAttribute<?, ?, ?> ) {
			entityTypeClass = (((PluralAttribute<?, ?, ?>) attribute).getElementType().getJavaType());
		}
		else if ( attribute instanceof SingularAttribute<?, ?> ) {
			entityTypeClass = (((SingularAttribute<?, ?>) attribute).getType().getJavaType());
		}
		else {
			throw new AssertionFailure( "attributes have to either be " + "instanceof PluralAttribute or SingularAttribute " + "at this point" );
		}
		return entityTypeClass;
	}

	private static boolean isAnnotationPresent(Member member, Class<? extends Annotation> annotationClass) {
		boolean ret = false;
		if ( member instanceof Method ) {
			Method method = (Method) member;
			ret = method.isAnnotationPresent( annotationClass );
		}
		else if ( member instanceof Field ) {
			Field field = (Field) member;
			ret = field.isAnnotationPresent( annotationClass );
		}
		else {
			throw new AssertionFailure( "member should either be Field or Member" );
		}
		return ret;
	}

	private static Object member(Member member, Object object) {
		try {
			Object ret;
			if ( member instanceof Method ) {
				Method method = (Method) member;
				ret = method.invoke( object );
			}
			else if ( member instanceof Field ) {
				Field field = (Field) member;
				// just to make sure
				field.setAccessible( true );
				ret = field.get( object );
			}
			else {
				throw new AssertionFailure( "member should either be Field or Member" );
			}
			return ret;
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException( e );
		}
	}

	public Set<Class<?>> getIndexRelevantEntites() {
		return Collections.unmodifiableSet( this.totalVisitedEntities );
	}

	public Map<Class<?>, String> getIdProperties() {
		return Collections.unmodifiableMap( this.idProperties );
	}

	public void parse(Metamodel metaModel) {
		// clear the (old) state
		this.isRootType.clear();
		this.managedTypes.clear();
		this.idProperties.clear();
		this.managedTypes.clear();
		this.managedTypes.putAll(
				metaModel.getManagedTypes().stream().filter(
						(meta3) -> {
							return meta3 instanceof EntityType;
						}
				).collect(
						Collectors.toMap(
								(meta) -> {
									return meta.getJavaType();
								}, (meta2) -> {
									return meta2;
								}
						)
				)
		);
		Set<EntityType<?>> emptyVisited = Collections.emptySet();
		this.totalVisitedEntities.clear();
		for ( EntityType<?> curEntType : metaModel.getEntities() ) {
			// we only consider Entities that are @Indexed here
			if ( curEntType.getJavaType().isAnnotationPresent( Indexed.class ) ) {
				this.idProperties.put( curEntType.getJavaType(), this.getIdProperty( metaModel, curEntType ) );
				this.isRootType.put( curEntType.getJavaType(), true );
				Map<Class<? extends Annotation>, Set<Attribute<?, ?>>> attributeForAnnotationType = buildAttributeForAnnotationType(
						curEntType
				);
				// and do the recursion
				this.doRecursion( attributeForAnnotationType, curEntType, emptyVisited );
			}
		}
	}

	private void parse(EntityType<?> curEntType, Class<?> cameFrom, Set<EntityType<?>> visited) {
		// just to make sure we don't handle an entity twice:
		if ( visited.contains( curEntType ) ) {
			return;
		}

		Map<Class<? extends Annotation>, Set<Attribute<?, ?>>> attributeForAnnotationType = buildAttributeForAnnotationType(
				curEntType
		);
		Function<Object, Object> toRoot;
		// first of all, lets build the parentAccessor for this entity
		if ( visited.size() > 0 ) {
			// don't do this for the first entity
			Set<Attribute<?, ?>> cameFromAttributes = attributeForAnnotationType.getOrDefault(
					ContainedIn.class,
					new HashSet<>()
			).stream()
					.filter(
							(attribute) -> {
								Class<?> entityTypeClass = getEntityTypeClass( attribute );
								return entityTypeClass.equals( cameFrom );
							}
					).collect( Collectors.toSet() );
			if ( cameFromAttributes.size() != 1 ) {
				throw new IllegalArgumentException( "entity: " + curEntType.getJavaType() + " has not exactly 1 @ContainedIn for each Index-parent specified" );
			}
			Attribute<?, ?> toParentAttribute = cameFromAttributes.iterator().next();
			toRoot = (object) -> {
				Object parentOfThis = member( toParentAttribute.getJavaMember(), object );
				return parentOfThis;
			};
		}

		// and do the recursion
		this.doRecursion( attributeForAnnotationType, curEntType, visited );
	}

	private void doRecursion(
			Map<Class<? extends Annotation>, Set<Attribute<?, ?>>> attributeForAnnotationType, EntityType<?> entType,
			Set<EntityType<?>> visited) {
		// we don't change the original visited set.
		Set<EntityType<?>> newVisited = new HashSet<>( visited );
		// add the current entityType to the set
		newVisited.add( entType );
		this.totalVisitedEntities.add( entType.getJavaType() );
		// we don't want to visit already visited entities
		// this should be okay to do, as cycles don't matter
		// as long as we start from the original
		attributeForAnnotationType
				.getOrDefault( IndexedEmbedded.class, new HashSet<>() )
				.stream()
				.filter(
						(attribute) -> {
							Class<?> entityTypeClass = getEntityTypeClass( attribute );
							boolean notVisited = !visited.contains( this.managedTypes.get( entityTypeClass ) );
							PersistentAttributeType attrType = attribute.getPersistentAttributeType();
							boolean otherEndIsEntity = attrType != PersistentAttributeType.BASIC && attrType != PersistentAttributeType.EMBEDDED;
							if ( attrType == PersistentAttributeType.ELEMENT_COLLECTION ) {
								throw new UnsupportedOperationException(
										"Element Collections are not allowed as with plain JPA "
												+ "as we haven't reliably proved how to get the " + "events to update our index, yet!"
								);
							}
							// Collections get updated in the owning entity (with
							// EclipseLink) :)
							// TODO: we should still testCustomUpdatedEntity whether MANY_TO_MANY
							// are fine as well, but they should
							// if (attrType == PersistentAttributeType.MANY_TO_MANY) {
							// throw new UnsupportedOperationException(
							// "MANY_TO_MANY is not allowed as with plain JPA "
							// +
							// "we cannot reliably get the events to update our index!"
							// + " Please map the Bridge table itself. "
							// +
							// "Btw.: Map all your Bridge tables when using this class!");
							// }
							return notVisited && otherEndIsEntity;
						}
				).forEach(
				(attribute) -> {
					Class<?> entityTypeClass = getEntityTypeClass( attribute );
					this.parse(
							(EntityType<?>) this.managedTypes.get( entityTypeClass ),
							entType.getJavaType(),
							newVisited
					);
				}
		);
	}

	@SuppressWarnings("unchecked")
	private String getIdProperty(Metamodel metamodel, EntityType<?> entityType) {
		String idProperty = null;
		Set<?> singularAttributes = entityType.getSingularAttributes();
		for ( SingularAttribute<?, ?> singularAttribute : (Set<SingularAttribute<?, ?>>) singularAttributes ) {
			if ( singularAttribute.isId() ) {
				idProperty = singularAttribute.getName();
				break;
			}
		}
		if ( idProperty == null ) {
			throw new RuntimeException( "id field not found" );
		}
		return idProperty;
	}

}
