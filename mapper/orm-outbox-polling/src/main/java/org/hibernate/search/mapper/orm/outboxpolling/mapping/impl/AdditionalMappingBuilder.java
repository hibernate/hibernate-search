/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.mapping.impl;

import java.util.Locale;

import jakarta.persistence.AccessType;
import jakarta.persistence.EnumType;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIndexImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTenantIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUuidGeneratorImpl;

public class AdditionalMappingBuilder {

	private final JaxbEntityImpl entity;

	public AdditionalMappingBuilder(Class<?> type, String name) {
		entity = new JaxbEntityImpl();
		entity.setAccess( AccessType.FIELD );
		entity.setClazz( type.getName() );
		entity.setName( name );
		JaxbTableImpl table = new JaxbTableImpl();
		entity.setTable( table );

		entity.setAttributes( new JaxbAttributesContainerImpl() );
	}

	public AdditionalMappingBuilder table(String schema, String catalog, String table) {
		entity.getTable().setSchema( schema );
		entity.getTable().setCatalog( catalog );
		entity.getTable().setName( table );

		return this;
	}

	public AdditionalMappingBuilder index(String name) {
		return index( name, name );
	}

	public AdditionalMappingBuilder index(String name, String columns) {
		JaxbIndexImpl index = new JaxbIndexImpl();
		index.setName( name );
		index.setColumnList( columns );

		entity.getTable()
				.getIndex()
				.add( index );
		return this;
	}

	public AdditionalMappingBuilder attribute(String name, Integer length, Boolean nullable) {
		return attribute( name, length, nullable, null );
	}

	public AdditionalMappingBuilder attribute(String name, Integer length, Boolean nullable, Integer type) {
		entity.getAttributes().getBasicAttributes().add( createAttribute( name, length, nullable, type ) );
		return this;
	}

	public AdditionalMappingBuilder tenantId(String name) {
		entity.setTenantId( new JaxbTenantIdImpl() );
		entity.getTenantId().setName( name );
		return this;
	}

	public AdditionalMappingBuilder enumAttribute(String name, Integer length, Boolean nullable) {
		JaxbBasicImpl attribute = createAttribute( name, length, nullable );
		attribute.setEnumerated( EnumType.STRING );
		entity.getAttributes().getBasicAttributes().add( attribute );
		return this;
	}

	public AdditionalMappingBuilder id(Integer type, String strategy) {
		JaxbIdImpl id = new JaxbIdImpl();
		id.setName( "id" );
		if ( type != null ) {
			id.setJdbcTypeCode( type );
		}
		id.setUuidGenerator( new JaxbUuidGeneratorImpl() );
		id.getUuidGenerator().setStyle( UuidGenerator.Style.valueOf( strategy.toUpperCase( Locale.ROOT ) ) );
		entity.getAttributes().getIdAttributes().add( id );

		return this;
	}

	public JaxbEntityMappingsImpl build() {
		JaxbEntityMappingsImpl mappings = new JaxbEntityMappingsImpl();

		mappings.getEntities().add( entity );

		return mappings;
	}

	private JaxbBasicImpl createAttribute(String name, Integer size, boolean nullable) {
		return createAttribute( name, size, nullable, null );
	}

	private JaxbBasicImpl createAttribute(String name, Integer size, boolean nullable, Integer type) {
		JaxbBasicImpl attribute = new JaxbBasicImpl();
		attribute.setName( name );
		JaxbColumnImpl column = new JaxbColumnImpl();
		attribute.setColumn( column );
		column.setName( name );
		column.setNullable( nullable );
		column.setLength( size );
		if ( type != null ) {
			attribute.setJdbcTypeCode( type );
		}
		return attribute;
	}
}
