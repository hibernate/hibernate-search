/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.metadata.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.IndexDescriptor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.metadata.PropertyDescriptor;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Hardy Ferentschik
 */
public class IndexedTypeDescriptorImpl implements IndexedTypeDescriptor {
	private static final Log log = LoggerFactory.make();

	private final Class<?> indexedType;
	private final float classBoost;
	private final BoostStrategy boostStrategy;
	private final Map<String, PropertyDescriptor> keyedPropertyDescriptors;
	private final Set<PropertyDescriptor> propertyDescriptors;
	private final Set<FieldDescriptor> classBridgeFieldDescriptors;
	private final Set<IndexDescriptor> indexDescriptors;
	private final Map<String, FieldDescriptor> allFieldDescriptors;
	private final boolean sharded;

	public IndexedTypeDescriptorImpl(TypeMetadata typeMetadata, IndexManager[] indexManagers) {
		this.indexedType = typeMetadata.getType();
		this.classBoost = typeMetadata.getStaticBoost();
		this.boostStrategy = typeMetadata.getDynamicBoost();
		this.sharded = indexManagers.length > 1;

		// create the class bridge fields
		Set<FieldDescriptor> fieldDescriptorTmp = new HashSet<FieldDescriptor>();
		for ( DocumentFieldMetadata documentFieldMetadata : typeMetadata.getClassBridgeMetadata() ) {
			FieldDescriptor fieldDescriptor;
			if ( documentFieldMetadata.isNumeric() ) {
				fieldDescriptor = new NumericFieldDescriptorImpl( documentFieldMetadata );
			}
			else {
				fieldDescriptor = new FieldDescriptorImpl( documentFieldMetadata );
			}
			fieldDescriptorTmp.add( fieldDescriptor );
		}
		this.classBridgeFieldDescriptors = Collections.unmodifiableSet( fieldDescriptorTmp );

		// handle the property descriptor and their fields
		this.keyedPropertyDescriptors = Collections.unmodifiableMap( createPropertyDescriptors( typeMetadata ) );
		this.propertyDescriptors = Collections.unmodifiableSet(
				new HashSet<PropertyDescriptor>(
						createPropertyDescriptors( typeMetadata ).values()
				)
		);
		this.allFieldDescriptors = Collections.unmodifiableMap( createAllFieldDescriptors() );

		// create the index descriptors
		Set<IndexDescriptor> indexDescriptorTmp = new HashSet<IndexDescriptor>();
		for ( IndexManager indexManager : indexManagers ) {
			indexDescriptorTmp.add( new IndexDescriptorImpl( indexManager ) );
		}
		this.indexDescriptors = Collections.unmodifiableSet( indexDescriptorTmp );

	}

	@Override
	public Class<?> getType() {
		return indexedType;
	}

	@Override
	public boolean isIndexed() {
		return true;
	}

	@Override
	public boolean isSharded() {
		return sharded;
	}

	@Override
	public float getStaticBoost() {
		return classBoost;
	}

	@Override
	public BoostStrategy getDynamicBoost() {
		return boostStrategy;
	}

	@Override
	public Set<IndexDescriptor> getIndexDescriptors() {
		return indexDescriptors;
	}

	@Override
	public Set<PropertyDescriptor> getIndexedProperties() {
		return propertyDescriptors;
	}

	@Override
	public PropertyDescriptor getProperty(String propertyName) {
		if ( propertyName == null ) {
			throw log.getPropertyNameCannotBeNullException();
		}
		for ( PropertyDescriptor propertyDescriptor : propertyDescriptors ) {
			if ( propertyDescriptor.getName().equals( propertyName ) ) {
				return propertyDescriptor;
			}
		}
		return null;
	}

	@Override
	public Set<FieldDescriptor> getIndexedFields() {
		return classBridgeFieldDescriptors;
	}

	@Override
	public FieldDescriptor getIndexedField(String fieldName) {
		if ( fieldName == null ) {
			throw log.getFieldNameCannotBeNullException();
		}
		return allFieldDescriptors.get( fieldName );
	}

	@Override
	public Set<FieldDescriptor> getFieldsForProperty(String propertyName) {
		if ( propertyName == null ) {
			throw log.getPropertyNameCannotBeNullException();
		}
		if ( keyedPropertyDescriptors.containsKey( propertyName ) ) {
			return keyedPropertyDescriptors.get( propertyName ).getIndexedFields();
		}
		else {
			return Collections.emptySet();
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "IndexedTypeDescriptorImpl{" );
		sb.append( "indexedType=" ).append( indexedType );
		sb.append( ", classBoost=" ).append( classBoost );
		sb.append( ", boostStrategy=" ).append( boostStrategy );
		sb.append( ", keyedPropertyDescriptors=" ).append( keyedPropertyDescriptors );
		sb.append( ", propertyDescriptors=" ).append( propertyDescriptors );
		sb.append( ", classBridgeFieldDescriptors=" ).append( classBridgeFieldDescriptors );
		sb.append( ", indexDescriptors=" ).append( indexDescriptors );
		sb.append( ", allFieldDescriptors=" ).append( allFieldDescriptors );
		sb.append( ", sharded=" ).append( sharded );
		sb.append( '}' );
		return sb.toString();
	}

	private Map<String, PropertyDescriptor> createPropertyDescriptors(TypeMetadata typeMetadata) {
		Map<String, PropertyDescriptor> propertyDescriptorsTmp = new HashMap<String, PropertyDescriptor>();
		for ( PropertyMetadata propertyMetadata : typeMetadata.getAllPropertyMetadata() ) {
			createOrMergePropertyDescriptor( propertyDescriptorsTmp, propertyMetadata );
		}
		createOrMergePropertyDescriptor( propertyDescriptorsTmp, typeMetadata.getIdPropertyMetadata() );
		return propertyDescriptorsTmp;
	}

	private void createOrMergePropertyDescriptor(Map<String, PropertyDescriptor> propertyDescriptorsTmp,
			PropertyMetadata propertyMetadata) {
		String propertyName = propertyMetadata.getPropertyAccessorName();
		Set<FieldDescriptor> tmpSet = new HashSet<FieldDescriptor>();
		if ( propertyDescriptorsTmp.containsKey( propertyName ) ) {
			tmpSet.addAll( propertyDescriptorsTmp.get( propertyName ).getIndexedFields() );
		}

		boolean id = false;
		for ( DocumentFieldMetadata documentFieldMetadata : propertyMetadata.getFieldMetadataSet() ) {
			if ( documentFieldMetadata.isId() ) {
				id = true;
			}
			FieldDescriptor fieldDescriptor;
			if ( documentFieldMetadata.isNumeric() ) {
				fieldDescriptor = new NumericFieldDescriptorImpl( documentFieldMetadata );
			}
			else {
				fieldDescriptor = new FieldDescriptorImpl( documentFieldMetadata );
			}
			tmpSet.add( fieldDescriptor );
		}
		PropertyDescriptor propertyDescriptor = new PropertyDescriptorImpl(
				propertyMetadata.getPropertyAccessorName(),
				id,
				tmpSet
		);

		propertyDescriptorsTmp.put( propertyDescriptor.getName(), propertyDescriptor );
	}

	private Map<String, FieldDescriptor> createAllFieldDescriptors() {
		Map<String, FieldDescriptor> fieldDescriptorMap = new HashMap<String, FieldDescriptor>();
		for ( FieldDescriptor fieldDescriptor : classBridgeFieldDescriptors ) {
			fieldDescriptorMap.put( fieldDescriptor.getName(), fieldDescriptor );
		}
		for ( PropertyDescriptor propertyDescriptor : propertyDescriptors ) {
			for ( FieldDescriptor fieldDescriptor : propertyDescriptor.getIndexedFields() ) {
				fieldDescriptorMap.put( fieldDescriptor.getName(), fieldDescriptor );
			}
		}
		return fieldDescriptorMap;
	}
}


