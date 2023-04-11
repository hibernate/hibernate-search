/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoModelPathBinder;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoModelPathWalker;
import org.hibernate.search.mapper.pojo.model.path.spi.BindablePojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinition;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinitionProvider;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathEntityStateRepresentation;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A single-use interpreter of {@link org.hibernate.search.mapper.pojo.model.path.PojoModelPath}
 * as a path in the Hibernate ORM metamodel,
 * in particular with respect to the Hibernate ORM event system.
 * <p>
 * Paths passed to this object are assigned a string representation so as to match the property names
 * and collection roles from Hibernate ORM.
 * <p>
 * Implementation is as follows:
 * <ul>
 *     <li>
 *         If the whole path does not contain any multi-valued {@link Value}
 *         and can be resolved to a Hibernate ORM {@link Value}
 *         that will be reported by ORM as dirty in a {@link PostUpdateEvent}
 *         (i.e. a {@link SimpleValue} but not its subtypes, or a {@link ToOne},
 *         or a {@link Any}, or a {@link Component}),
 *         then the path will be represented as the "property path"
 *         (a dot-separated sequence of the properties mentioned in the whole path).
 *     </li>
 *     <li>
 *         Otherwise, if the whole path can be resolved to a multi-valued {@link Value}
 *         (i.e. a {@link org.hibernate.mapping.Collection}, which also encompasses maps)
 *         containing only embeddeds (i.e. {@link Component})
 *         or "atomic" values (i.e. {@link SimpleValue} but not its subtypes)
 *         or "association" values (i.e. {@link OneToMany},
 *         {@link ToOne} or {@link Any})
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
 *         This includes in particular all cases when the path points to a {@link jakarta.persistence.Transient} property,
 *         or when a custom or {@link Optional} value extractor
 *         is used before we can detect a prefix matching the conditions described above.
 *     </li>
 * </ul>
 */
final class HibernateOrmPathInterpreter
		implements PojoModelPathWalker<HibernateOrmPathInterpreter.Context, Value, Property, Value> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final Set<String> PRIMITIVE_EXTRACTOR_NAMES = CollectionHelper.asImmutableSet(
			BuiltinContainerExtractors.ARRAY_CHAR,
			BuiltinContainerExtractors.ARRAY_BOOLEAN,
			BuiltinContainerExtractors.ARRAY_BYTE,
			BuiltinContainerExtractors.ARRAY_SHORT,
			BuiltinContainerExtractors.ARRAY_INT,
			BuiltinContainerExtractors.ARRAY_LONG,
			BuiltinContainerExtractors.ARRAY_FLOAT,
			BuiltinContainerExtractors.ARRAY_DOUBLE
	);

	static final class Context {
		private final PersistentClass persistentClass;
		private final PojoTypeModel<?> typeModel;
		private final List<String> propertyStringRepresentationByOrdinal;
		private final PojoModelPathValueNode wholePath;

		private boolean root = true;
		private final Set<String> stringRepresentations = new LinkedHashSet<>();
		private String rootComponentPropertyName;
		private PojoPathEntityStateRepresentation entityStateRepresentation = null;
		private boolean found = false;

		private Context(PojoTypeModel<?> typeModel, PersistentClass persistentClass,
				List<String> propertyStringRepresentationByOrdinal,
				PojoModelPathValueNode wholePath) {
			this.typeModel = typeModel;
			this.persistentClass = persistentClass;
			this.propertyStringRepresentationByOrdinal = propertyStringRepresentationByOrdinal;
			this.wholePath = wholePath;
		}

		public void resolvedStringRepresentation(String ... stringRepresentationArray) {
			found = true;
			Collections.addAll( stringRepresentations, stringRepresentationArray );
		}

		void pushComponent(String propertyName) {
			if ( rootComponentPropertyName == null ) {
				rootComponentPropertyName = propertyName;
			}
		}

		public void disableStateRepresentation() {
			this.entityStateRepresentation = null;
		}

		public void resolvedStateRepresentation(String wholePathStringRepresentation) {
			try {
				tryResolveStateRepresentation( wholePathStringRepresentation );
			}
			catch (RuntimeException e) {
				// TODO HSEARCH-4720 when we can afford breaking changes (in the next major), we should probably throw an exception
				//  instead of just logging a warning here?
				log.failedToResolveStateRepresentation(
						wholePathStringRepresentation,
						EventContexts.fromType( typeModel ).append( PojoEventContexts.fromPath( wholePath ) ),
						e.getMessage(), e
				);
				disableStateRepresentation();
			}
		}

		public void tryResolveStateRepresentation(String wholePathStringRepresentation) {
			String propertyStringRepresentationForOrdinal;
			Optional<BindablePojoModelPath> pathFromStateArrayElement;
			if ( rootComponentPropertyName == null ) {
				// When components (@Embedded) are NOT involved,
				// Hibernate ORM will include the "leaf" value
				// (whatever the whole path points to) directly in the state array.
				propertyStringRepresentationForOrdinal = wholePathStringRepresentation;
				pathFromStateArrayElement = Optional.empty();
			}
			else {
				// When components (@Embedded) are involved,
				// Hibernate ORM will include the root component in the state array,
				// not the actual properties individually.
				// So we need use the root component path to determine the ordinal in the array...
				propertyStringRepresentationForOrdinal = rootComponentPropertyName;
				// ... and we need to provide the POJO mapper with a way to extract the "leaf" value
				// (whatever the whole path points to) from the root component.
				PojoModelPathValueNode rootComponentPath = PojoModelPath.ofValue(
						rootComponentPropertyName, ContainerExtractorPath.noExtractors() );
				Optional<PojoModelPathValueNode> unboundPathFromRootComponent = wholePath.relativize( rootComponentPath );
				if ( !unboundPathFromRootComponent.isPresent() ) {
					throw new AssertionFailure( "Cannot relativize '" + rootComponentPath + "' to '" + wholePath + "'." );
				}
				pathFromStateArrayElement = Optional.of( new BindablePojoModelPath(
						typeModel.property( rootComponentPropertyName ).typeModel(),
						unboundPathFromRootComponent.get()
				) );
			}

			int ordinalInStateArray = propertyStringRepresentationByOrdinal.indexOf( propertyStringRepresentationForOrdinal );
			if ( ordinalInStateArray < 0 ) {
				throw new AssertionFailure( "Cannot find ordinal in state array for path '"
						+ propertyStringRepresentationForOrdinal
						+ "'. Available paths are: " + propertyStringRepresentationByOrdinal + "." );
			}

			this.entityStateRepresentation = new PojoPathEntityStateRepresentation( ordinalInStateArray,
					pathFromStateArrayElement );
		}
	}

	public PojoPathDefinition interpretPath(PojoRawTypeModel<?> typeModel, PersistentClass persistentClass,
			List<String> propertyStringRepresentationByOrdinal, PojoModelPathValueNode path) {
		Context context = new Context( typeModel, persistentClass, propertyStringRepresentationByOrdinal, path );
		Value value = PojoModelPathBinder.bind( context, null, path, this );
		if ( !context.found ) {
			/*
			 * We were able to resolve the path, but didn't find any Value that could possibly
			 * be reported as dirty by Hibernate ORM.
			 */
			throw log.unreportedPathForDirtyChecking( path, value );
		}
		// Else everything is good, the string representation was successfully added to the set.
		return new PojoPathDefinition( context.stringRepresentations,
				Optional.ofNullable( context.entityStateRepresentation ) );
	}

	@Override
	public Value type(Context context, Value valueNode) {
		// No-op
		return valueNode;
	}

	@Override
	public Property property(Context context, Value parentValue, PojoModelPathPropertyNode pathNode) {
		if ( context.found ) {
			// We stopped interpreting.
			return null;
		}

		try {
			if ( context.root ) {
				context.root = false;
				return context.persistentClass.getProperty( pathNode.propertyName() );
			}
			else if ( parentValue instanceof Component ) {
				return ( (Component) parentValue ).getProperty( pathNode.propertyName() );
			}
			else {
				throw log.unknownPathForDirtyChecking( pathNode, null );
			}
		}
		catch (MappingException e) {
			throw log.unknownPathForDirtyChecking( pathNode, e );
		}
	}

	@Override
	public Value value(Context context, Property property, PojoModelPathValueNode path) {
		if ( context.found ) {
			// We stopped interpreting.
			return null;
		}

		boolean isWholePath = context.wholePath.equals( path );
		Value baseValue = property.getValue();
		PojoModelPathPropertyNode propertyNode = path.parent();

		ContainerExtractorPath extractorPath = path.extractorPath();
		if ( extractorPath.isDefault() ) {
			throw new AssertionFailure(
					"Expected a non-default extractor path as per the "
					+ PojoPathDefinitionProvider.class.getSimpleName() + " contract"
			);
		}

		Class<? extends Value> valueClass = baseValue.getClass();

		if ( Component.class.isAssignableFrom( valueClass ) ) {
			if ( !extractorPath.isEmpty() ) {
				throw log.unknownPathForDirtyChecking( path, null );
			}
			if ( isWholePath ) {
				// The path as a whole (and not just a prefix) was resolved to an embedded
				context.resolvedStringRepresentation( propertyNode.toPropertyString() );
				// We don't need state extraction in this case
				context.disableStateRepresentation();
				// The string representation of the path was added, we can stop here
				return null;
			}
			else {
				context.pushComponent( propertyNode.propertyName() );
				return baseValue;
			}
		}
		else if ( BasicValue.class.isAssignableFrom( valueClass ) ) {
			// The path as a whole (and not just a prefix) was resolved to a non-component, non-association value
			context.resolvedStringRepresentation( propertyNode.toPropertyString() );
			// We don't need state extraction in this case
			context.disableStateRepresentation();
			// The string representation of the path was added, we can stop here
			return null;
		}
		else if ( SimpleValue.class.isAssignableFrom( valueClass ) ) {
			if ( isWholePath && isSingleValuedAssociation( valueClass ) ) {
				// The path as a whole (and not just a prefix) was resolved to an association
				String stringRepresentationAsProperty = propertyNode.toPropertyString();
				context.resolvedStringRepresentation( stringRepresentationAsProperty );
				resolveStateExtractorIfRelevant( context, stringRepresentationAsProperty, baseValue );
				// The string representation of the path was added, we can stop here
				return null;
			}
			else {
				return baseValue;
			}
		}
		else if ( org.hibernate.mapping.Collection.class.isAssignableFrom( valueClass ) ) {
			if ( extractorPath.isEmpty() && !isWholePath ) {
				/*
				 * We only allow an empty extractor path for a collection at the very end of the path,
				 * meaning "reindex whenever that collection changes, we don't really care about the values".
				 */
				throw log.unknownPathForDirtyChecking( path, null );
			}

			List<String> extractorNames = extractorPath.explicitExtractorNames();
			Iterator<String> extractorNameIterator = extractorNames.iterator();

			return resolveExtractorPath(
					context, path, isWholePath, propertyNode, baseValue, extractorNameIterator
			);
		}
		else {
			throw log.unknownPathForDirtyChecking( path, null );
		}
	}

	private Value resolveExtractorPath(Context context, PojoModelPathValueNode path,
			boolean isWholePath, PojoModelPathPropertyNode propertyNode,
			Value baseValue, Iterator<String> extractorNameIterator) {
		Value containedValue = baseValue;
		org.hibernate.mapping.Collection collectionValue;
		do {
			collectionValue = (org.hibernate.mapping.Collection) containedValue;
			try {
				String extractorName = extractorNameIterator.hasNext() ? extractorNameIterator.next() : null;
				containedValue = resolveExtractor( collectionValue, extractorName );
			}
			catch (SearchException e) {
				throw log.unknownPathForDirtyChecking( path, e );
			}
		}
		while ( extractorNameIterator.hasNext() && containedValue instanceof org.hibernate.mapping.Collection );

		if ( !extractorNameIterator.hasNext() ) {
			// We managed to resolve the whole container value extractor list
			Class<? extends Value> containedValueClass = containedValue.getClass();
			if ( BasicValue.class.isAssignableFrom( containedValueClass )
					|| Component.class.isAssignableFrom( containedValueClass )
					|| isWholePath && isAssociation( containedValueClass ) ) {
				String stringRepresentationAsProperty = propertyNode.toPropertyString();
				context.resolvedStringRepresentation( stringRepresentationAsProperty, collectionValue.getRole() );
				resolveStateExtractorIfRelevant( context, stringRepresentationAsProperty, containedValue );
				// The string representation of the path was added, we can stop here
				return null;
			}
			else {
				return containedValue;
			}
		}

		throw log.unknownPathForDirtyChecking( path, null );
	}

	private Value resolveExtractor(org.hibernate.mapping.Collection collectionValue, String extractorName) {
		if ( collectionValue instanceof org.hibernate.mapping.PrimitiveArray ) {
			if ( extractorName == null || PRIMITIVE_EXTRACTOR_NAMES.contains( extractorName ) ) {
				return collectionValue.getElement();
			}
		}
		else if ( collectionValue instanceof org.hibernate.mapping.Array ) {
			if ( extractorName == null || BuiltinContainerExtractors.ARRAY_OBJECT.equals( extractorName ) ) {
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

	private void resolveStateExtractorIfRelevant(Context context, String stringRepresentationAsProperty, Value value) {
		if ( value instanceof OneToOne
				|| value instanceof ManyToOne && ( (ManyToOne) value ).isLogicalOneToOne() ) {
			String mappedBy = ( (ToOne) value ).getReferencedPropertyName();
			if ( mappedBy == null || mappedBy.isEmpty() ) {
				// This is the owning side of a OneToOne association.
				// We DO need to resolve the association from the entity state upon change,
				// because we may not get any event when to non-owning side changes.
				// See https://hibernate.atlassian.net/browse/HSEARCH-4708

				context.resolvedStateRepresentation( stringRepresentationAsProperty );
			}
			else {
				// This is the non-owning side of a OneToOne association.
				// We do NOT need to resolve the association from the entity state upon change,
				// because the owning side will always get updated (otherwise the change won't be reflected in DB).
				context.disableStateRepresentation();
			}
		}
		else {
			// We do not support resolving this association (ManyToOne, ManyToMany, Any, ...)
			// from the entity state upon change, at least not at the moment.
			// See https://hibernate.atlassian.net/browse/HSEARCH-3567
			context.disableStateRepresentation();
		}
	}

	private static boolean isSingleValuedAssociation(Class<? extends Value> valueClass) {
		return ToOne.class.isAssignableFrom( valueClass )
				|| Any.class.isAssignableFrom( valueClass );
	}

	private static boolean isAssociation(Class<? extends Value> valueClass) {
		return OneToMany.class.isAssignableFrom( valueClass )
				|| ToOne.class.isAssignableFrom( valueClass )
				|| Any.class.isAssignableFrom( valueClass );
	}
}
