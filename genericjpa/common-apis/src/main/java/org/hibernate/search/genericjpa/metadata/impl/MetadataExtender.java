/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.metadata.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * this class converts {@link org.hibernate.search.engine.metadata.impl.TypeMetadata}
 * into {@link ExtendedTypeMetadata} that is in a better format for the async backend
 * to understand.
 *
 * @author Martin Braun
 */
public final class MetadataExtender {

	private static final Log log = LoggerFactory.make();

	public List<ExtendedTypeMetadata> rehash(List<TypeMetadata> originals, Collection<Class<?>> checkHierarchyFor) {
		Map<Class<?>, TypeMetadata> typeMetadataPerClass = new HashMap<>();
		for ( TypeMetadata metadata : originals ) {
			typeMetadataPerClass.put( metadata.getType(), metadata );
		}
		Map<Class<?>, ExtendedTypeMetadata> rehashedPerType = new HashMap<>();

		List<Class<?>> classesToConsiderForHierarchies = new ArrayList<>();
		classesToConsiderForHierarchies.addAll( checkHierarchyFor );

		for ( TypeMetadata orig : originals ) {
			ExtendedTypeMetadata rehashed = this.rehash( orig, checkHierarchyFor );
			rehashedPerType.put( rehashed.getOriginalTypeMetadata().getType(), rehashed );
			classesToConsiderForHierarchies.add( rehashed.getOriginalTypeMetadata().getType() );
		}

		Map<Class<?>, Set<Class<?>>> relevantSubClasses = this.calculateRelevantSubclasses(
				classesToConsiderForHierarchies
		);

		for ( Map.Entry<Class<?>, ExtendedTypeMetadata> entry : rehashedPerType.entrySet() ) {
			Class<?> parent = entry.getKey();
			ExtendedTypeMetadata extendedTypeMetadata = entry.getValue();

			Set<Class<?>> relevantSub = relevantSubClasses.get( parent );
			for ( Class<?> sub : relevantSub ) {
				if ( !sub.equals( parent ) ) {
					ExtendedTypeMetadata subRehashed = rehashedPerType.get( sub );
					if ( subRehashed != null ) {
						//we have some additional stuff here
						//this has to be merged into the parent's info
						//so we have this available during index updating
						Set<String> idFieldNames = extendedTypeMetadata.idFieldNamesForType.computeIfAbsent(
								parent,
								(key) -> new HashSet<>()
						);
						idFieldNames.addAll(
								subRehashed.idFieldNamesForType.get( sub )
						);

						if ( !extendedTypeMetadata.idPropertyNameForType.get( parent )
								.equals( subRehashed.idPropertyNameForType.get( sub ) ) ) {
							throw log.overriddenIdSettings( parent, sub );
						}
						if ( !extendedTypeMetadata.idPropertyAccessorForType.get( parent ).getName()
								.equals( subRehashed.idPropertyAccessorForType.get( sub ).getName() ) ) {
							throw log.overriddenIdSettings( parent, sub );
						}
					}
				}
			}
		}

		List<ExtendedTypeMetadata> ret = new ArrayList<>();
		for ( Map.Entry<Class<?>, ExtendedTypeMetadata> entry : rehashedPerType.entrySet() ) {
			ret.add( entry.getValue() );
		}
		return ret;
	}

	public Map<Class<?>, Set<Class<?>>> calculateRelevantSubclasses(
			Collection<Class<?>> classesToConsider) {
		Map<Class<?>, Set<Class<?>>> ret = new HashMap<>();
		for ( Class<?> parent : classesToConsider ) {
			for ( Class<?> clazz : classesToConsider ) {
				if ( parent.isAssignableFrom( clazz ) ) {
					ret.computeIfAbsent( parent, (_1) -> new HashSet<>() ).add( clazz );
				}
			}
		}
		return ret;
	}

	private ExtendedTypeMetadata rehash(
			TypeMetadata original,
			Collection<Class<?>> checkHierarchyFor) {
		ExtendedTypeMetadata rehashed = new ExtendedTypeMetadata();
		rehashed.originalTypeMetadata = original;

		if ( !this.handlePropertyMetadata(
				original,
				rehashed,
				original.getIdPropertyMetadata(),
				checkHierarchyFor
		) ) {
			throw new IllegalArgumentException(
					"couldn't find any id field for: " + original.getType()
							+ "! This is required in order to use Hibernate Search with JPA!"
			);
		}

		for ( EmbeddedTypeMetadata embedded : original.getEmbeddedTypeMetadata() ) {
			this.rehashRec( embedded, rehashed, checkHierarchyFor );
		}
		return rehashed;
	}

	private void rehashRec(
			EmbeddedTypeMetadata original,
			ExtendedTypeMetadata rehashed,
			Collection<Class<?>> checkHierarchyFor) {
		// handle the current TypeMetadata
		this.handleTypeMetadata( original, rehashed, checkHierarchyFor );
		// recursion
		for ( EmbeddedTypeMetadata embedded : original.getEmbeddedTypeMetadata() ) {
			this.rehashRec( embedded, rehashed, checkHierarchyFor );
		}
	}

	private void handleTypeMetadata(
			EmbeddedTypeMetadata original,
			ExtendedTypeMetadata rehashed,
			Collection<Class<?>> checkHierarchyFor) {
		for ( PropertyMetadata propertyMetadata : original.getAllPropertyMetadata() ) {
			if ( this.handlePropertyMetadata(
					original,
					rehashed,
					propertyMetadata,
					checkHierarchyFor
			) ) {
				return;
			}
		}
		//we didn't find any id, but this should be okay (-> Embeddable)
	}

	private boolean handlePropertyMetadata(
			TypeMetadata original,
			ExtendedTypeMetadata rehashed,
			PropertyMetadata propertyMetadata,
			Collection<Class<?>> checkHierarchyFor) {
		for ( DocumentFieldMetadata documentFieldMetadata : propertyMetadata.getFieldMetadataSet() ) {
			// this must either be id or id of an embedded object
			if ( documentFieldMetadata.isIdInEmbedded() || documentFieldMetadata.isId() ) {
				{
					Set<Class<?>> types = new HashSet<>();
					types.add( original.getType() );
					for ( Class<?> check : checkHierarchyFor ) {
						if ( original.getType().isAssignableFrom( check ) ) {
							types.add( check );
						}
					}
					for ( Class<?> type : types ) {
						rehashed.idFieldNamesForType.computeIfAbsent( type, (key) -> new HashSet<>() ).add(
								documentFieldMetadata.getName()
						);
						rehashed.idPropertyNameForType.put( type, propertyMetadata.getPropertyAccessorName() );
						rehashed.idPropertyAccessorForType.put( type, propertyMetadata.getPropertyAccessor() );
					}
				}
				return true;
			}
		}

		return false;
	}
}
