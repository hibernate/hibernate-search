/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;
import org.hibernate.search.mapper.pojo.model.path.spi.StringSetPojoPathFilter;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A {@link PojoPathFilterFactory} suitable for use with Hibernate ORM,
 * in particular with its event system.
 * <p>
 * Paths passed to this factory are assigned a string representation as follows:
 * <ul>
 *     <li>
 *         If the whole path does not contain any multi-valued {@link Value}
 *         and can be resolved to a Hibernate ORM {@link Value}
 *         that will be reported by ORM as dirty in a {@link PostUpdateEvent}
 *         (i.e. a {@link SimpleValue} but not its subtypes, or a {@link org.hibernate.mapping.ToOne},
 *         or a {@link org.hibernate.mapping.Any}, or a {@link Component}),
 *         then the path will be represented as the "property path"
 *         (a dot-separated sequence of the properties mentioned in the whole path).
 *     </li>
 *     <li>
 *         Otherwise, if the whole path can be resolved to a multi-valued {@link Value}
 *         (i.e. a {@link org.hibernate.mapping.Collection}, which also encompasses maps)
 *         containing only embeddeds (i.e. {@link Component})
 *         or "atomic" values (i.e. {@link SimpleValue} but not its subtypes)
 *         or "association" values (i.e. {@link org.hibernate.mapping.OneToMany},
 *         {@link org.hibernate.mapping.ToOne} or {@link org.hibernate.mapping.Any})
 *         then the path will be represented as either the "property path"
 *         (a dot-separated sequence of the properties mentioned in the whole path).
 *         or alternatively as the collection role.
 *         <p>
 *         Note that either representation may be passed as a parameter to the filter,
 *         so we will add both representations to the set of accepted paths.
 *     </li>
 *     <li>
 *         Otherwise, if a prefix of the path that does not contain any multi-valued {@link Value}
 *         can be resolved to a Hibernate ORM {@link Value}
 *         that will be reported by ORM as dirty in a {@link PostUpdateEvent}
 *         and is not an association
 *         (i.e. {@link SimpleValue} but not its subtypes),
 *         then the path will be represented as the "property path" of that prefix.
 *         <p>
 *         Rationale: such values are considered as atomic when persisting,
 *         i.e. they will be reported by ORM as dirty in a {@link PostUpdateEvent}
 *         whenever any of their parts changed.
 *         <p>
 *         For example, {@code ContainingEntity} below will depend on the path
 *         {@code Property contained => Property field} when indexed,
 *         but any change in the property {@code field} will trigger a {@link PostUpdateEvent}
 *         on entity {@code ContainingEntity}
 *         which will allow us to tell that something in {@code contained} is dirty.
 *         In this example, the prefix leading to a value considered as "atomic when persisting"
 *         is {@code Property contained}.
 *         <pre><code>
 *             &#064;Entity
 *             &#064;Indexed
 *             public class ContainingEntity {
 *                // The user type defines an "equals(x, y)" method allowing ORM to determine whether "contained" changed
 *                &#064;Basic
 *                &#064;Type(type = "myUserType")
 *                &#064;IndexedEmbedded
 *                private Contained contained;
 *             }
 *             public class Contained {
 *                &#064;GenericField
 *                private String field;
 *             }
 *         </code></pre>
 *     </li>
 *     <li>
 *         Otherwise, if a prefix of the path can be resolved to a multi-valued {@link Value}
 *         (i.e. a {@link org.hibernate.mapping.Collection}, which also encompasses maps)
 *         containing only embeddeds (i.e. {@link Component})
 *         or "atomic" values (i.e. {@link SimpleValue} but not its subtypes),
 *         then the path will be represented as either the "property path" of that prefix
 *         or alternatively as the collection role.
 *         <p>
 *         Note that since either representation may be passed as a parameter to the filter,
 *         so we will add both representations to the set of accepted paths.
 *         <p>
 *         Rationale: such values are considered as atomic when persisting,
 *         i.e. they will be reported by ORM as dirty in an {@link org.hibernate.event.spi.AbstractCollectionEvent}
 *         whenever any of their parts changed.
 *         <p>
 *         For example, {@code ContainingEntity} below will depend on the path
 *         {@code Property contained => CollectionElementExtractor => Property field} when indexed,
 *         but any change in the property {@code field} will trigger an {@link org.hibernate.event.spi.AbstractCollectionEvent}
 *         which will allow us to tell that something in {@code contained} is dirty.
 *         In this example, the prefix leading to a value considered as "atomic when persisting"
 *         is {@code Property contained => CollectionElementExtractor}.
 *         <pre><code>
 *             &#064;Entity
 *             &#064;Indexed
 *             public class ContainingEntity {
 *                &#064;ElementCollection
 *                &#064;IndexedEmbedded
 *                private List&lt;Contained&gt; contained;
 *             }
 *             &#064;Embeddable
 *             public class Contained {
 *                &#064;GenericField
 *                private String field;
 *             }
 *         </code></pre>
 *     </li>
 *     <li>
 *         Otherwise, no string representation can be assigned and an exception will be thrown.
 *         This includes in particular all cases when the path points to a {@link javax.persistence.Transient} property,
 *         or when a custom or {@link java.util.Optional} value extractor
 *         is used before we can detect a prefix matching the conditions described above.
 *     </li>
 * </ul>
 */
public class HibernateOrmPathFilterFactory implements PojoPathFilterFactory<Set<String>> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PersistentClass persistentClass;

	public HibernateOrmPathFilterFactory(PersistentClass persistentClass) {
		this.persistentClass = persistentClass;
	}

	@Override
	public PojoPathFilter<Set<String>> create(Set<PojoModelPathValueNode> paths) {
		// Use a LinkedHashSet for deterministic iteration
		Set<String> pathsAsStrings = CollectionHelper.newLinkedHashSet( paths.size() );
		for ( PojoModelPathValueNode path : paths ) {
			addDirtyPathStringRepresentations( pathsAsStrings, path );
		}
		return new StringSetPojoPathFilter( pathsAsStrings );
	}

	private void addDirtyPathStringRepresentations(Set<String> pathsAsStrings, PojoModelPathValueNode path) {
		Optional<Value> valueOptional = addDirtyPathStringRepresentationsRecursively( pathsAsStrings, path, true );
		if ( valueOptional.isPresent() ) {
			Value value = valueOptional.get();
			/*
			 * We were able to resolve the path, but didn't find any Value that could possibly
			 * be reported as dirty by Hibernate ORM.
			 */
			throw log.unreportedPathForDirtyChecking( persistentClass.getMappedClass(), path, value );
		}
		// Else everything is good, the string representation was successfully added to the set.
	}

	private Optional<Value> addDirtyPathStringRepresentationsRecursively(Set<String> pathsAsStrings,
			PojoModelPathValueNode path, boolean isWholePath) {
		PojoModelPathPropertyNode propertyNode = path.getParent();
		PojoModelPathValueNode propertyNodeParent = propertyNode.getParent();

		Property property;
		if ( propertyNodeParent == null ) {
			property = resolveProperty( persistentClass, propertyNode );
		}
		else {
			// Recurse using a prefix of the path
			Optional<Value> parentValueOptional =
					addDirtyPathStringRepresentationsRecursively( pathsAsStrings, propertyNodeParent, false );
			if ( !parentValueOptional.isPresent() ) {
				// The string representation of the path was added by the call above, we can stop here
				return Optional.empty();
			}
			else {
				Value parentValue = parentValueOptional.get();
				if ( !( parentValue instanceof Component ) ) {
					throw log.unknownPathForDirtyChecking( persistentClass.getMappedClass(), propertyNode, null );
				}
				property = resolveProperty( (Component) parentValue, propertyNode );
			}
		}

		Value baseValue = property.getValue();

		ContainerExtractorPath extractorPath = path.getExtractorPath();
		if ( extractorPath.isDefault() ) {
			throw new AssertionFailure(
					"Expected a non-default extractor path as per the "
					+ PojoPathFilterFactory.class.getSimpleName() + " contract"
			);
		}

		Class<? extends Value> valueClass = baseValue.getClass();

		if ( Component.class.isAssignableFrom( valueClass ) ) {
			if ( extractorPath.isEmpty() ) {
				if ( isWholePath ) {
					// The path as a whole (and not just a prefix) was resolved to an embedded
					pathsAsStrings.add( path.getParent().toPropertyString() );
					// The string representation of the path was added, we can stop here
					return Optional.empty();
				}
				else {
					return Optional.of( baseValue );
				}
			}
		}
		else if ( SimpleValue.class.equals( valueClass ) ) { // equals() and not isAssignableFrom(), we mean it.
			pathsAsStrings.add( propertyNode.toPropertyString() );
			// The string representation of the path was added, we can stop here
			return Optional.empty();
		}
		else if ( SimpleValue.class.isAssignableFrom( valueClass ) ) {
			if ( isWholePath && ToOne.class.isAssignableFrom( valueClass )
					|| isWholePath && Any.class.isAssignableFrom( valueClass ) ) {
				// The path as a whole (and not just a prefix) was resolved to an association
				pathsAsStrings.add( path.getParent().toPropertyString() );
				// The string representation of the path was added, we can stop here
				return Optional.empty();
			}
			else {
				return Optional.of( baseValue );
			}
		}
		else if ( org.hibernate.mapping.Collection.class.isAssignableFrom( valueClass ) ) {
			if ( extractorPath.isEmpty() && !isWholePath ) {
				/*
				 * We only allow an empty extractor path for a collection at the very end of the path,
				 * meaning "reindex whenever that collection changes, we don't really care about the values".
				 */
				throw log.unknownPathForDirtyChecking( persistentClass.getMappedClass(), propertyNode, null );
			}

			List<String> extractorNames = extractorPath.getExplicitExtractorNames();
			Iterator<String> extractorNameIterator = extractorNames.iterator();

			Value containedValue = baseValue;
			org.hibernate.mapping.Collection collectionValue;
			do {
				collectionValue = (org.hibernate.mapping.Collection) containedValue;
				try {
					String extractorName = extractorNameIterator.hasNext() ? extractorNameIterator.next() : null;
					containedValue = resolveContainedValue( collectionValue, extractorName );
				}
				catch (SearchException e) {
					throw log.unknownPathForDirtyChecking( persistentClass.getMappedClass(), path, e );
				}
			}
			while ( extractorNameIterator.hasNext() && containedValue instanceof org.hibernate.mapping.Collection );

			if ( !extractorNameIterator.hasNext() ) {
				// We managed to resolve the whole container value extractor list
				Class<? extends Value> containedValueClass = containedValue.getClass();
				if ( SimpleValue.class.equals( containedValueClass ) // equals() and not isAssignableFrom(), we mean it.
						|| Component.class.isAssignableFrom( containedValueClass )
						|| isWholePath && OneToMany.class.isAssignableFrom( containedValueClass )
						|| isWholePath && ToOne.class.isAssignableFrom( containedValueClass )
						|| isWholePath && Any.class.isAssignableFrom( containedValueClass ) ) {
					pathsAsStrings.add( propertyNode.toPropertyString() );
					pathsAsStrings.add( collectionValue.getRole() );
					// The string representation of the path was added, we can stop here
					return Optional.empty();
				}
				else {
					return Optional.of( containedValue );
				}
			}
		}

		throw log.unknownPathForDirtyChecking( persistentClass.getMappedClass(), path, null );
	}

	@SuppressWarnings("rawtypes")
	private Value resolveContainedValue(org.hibernate.mapping.Collection collectionValue, String extractorName) {
		if ( collectionValue instanceof org.hibernate.mapping.Array ) {
			if ( extractorName == null || BuiltinContainerExtractors.ARRAY.equals( extractorName ) ) {
				return collectionValue.getElement();
			}
		}
		else if ( collectionValue instanceof org.hibernate.mapping.Map ) {
			if ( BuiltinContainerExtractors.MAP_KEY.equals( extractorName ) ) {
				/*
				 * Do not let ORM confuse you: getKey() doesn't return the value of the map key,
				 * but the value of the foreign key to the targeted entity...
				 */
				return ( (org.hibernate.mapping.Map) collectionValue ).getIndex();
			}
			else if ( extractorName == null || BuiltinContainerExtractors.MAP_VALUE.equals( extractorName ) ) {
				return collectionValue.getElement();
			}
		}
		else if ( extractorName == null || BuiltinContainerExtractors.COLLECTION.equals( extractorName ) ) {
			return collectionValue.getElement();
		}

		throw log.invalidContainerExtractorForDirtyChecking( collectionValue.getClass(), extractorName );
	}

	private Property resolveProperty(PersistentClass persistentClass, PojoModelPathPropertyNode propertyNode) {
		try {
			return persistentClass.getProperty( propertyNode.getPropertyName() );
		}
		catch (MappingException e) {
			throw log.unknownPathForDirtyChecking( persistentClass.getMappedClass(), propertyNode, e );
		}
	}

	private Property resolveProperty(Component parentValue, PojoModelPathPropertyNode propertyNode) {
		try {
			return parentValue.getProperty( propertyNode.getPropertyName() );
		}
		catch (MappingException e) {
			throw log.unknownPathForDirtyChecking( persistentClass.getMappedClass(), propertyNode, e );
		}
	}
}
