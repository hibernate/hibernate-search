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
import org.hibernate.boot.jaxb.mapping.JaxbAttributes;
import org.hibernate.boot.jaxb.mapping.JaxbBasic;
import org.hibernate.boot.jaxb.mapping.JaxbColumn;
import org.hibernate.boot.jaxb.mapping.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;
import org.hibernate.boot.jaxb.mapping.JaxbId;
import org.hibernate.boot.jaxb.mapping.JaxbIndex;
import org.hibernate.boot.jaxb.mapping.JaxbTable;
import org.hibernate.boot.jaxb.mapping.JaxbTenantId;
import org.hibernate.boot.jaxb.mapping.JaxbUuidGenerator;

public class AdditionalMappingBuilder {

	private final JaxbEntity entity;

	public AdditionalMappingBuilder(Class<?> type, String name) {
		entity = new JaxbEntity();
		entity.setAccess( AccessType.FIELD );
		entity.setClazz( type.getName() );
		entity.setName( name );
		JaxbTable table = new JaxbTable();
		entity.setTable( table );

		entity.setAttributes( new JaxbAttributes() );
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
		JaxbIndex index = new JaxbIndex();
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
		entity.setTenantId( new JaxbTenantId() );
		entity.getTenantId().setName( name );
		return this;
	}

	public AdditionalMappingBuilder enumAttribute(String name, Integer length, Boolean nullable) {
		JaxbBasic attribute = createAttribute( name, length, nullable );
		attribute.setEnumerated( EnumType.STRING );
		entity.getAttributes().getBasicAttributes().add( attribute );
		return this;
	}

	public AdditionalMappingBuilder id(int type, String strategy) {
		JaxbId id = new JaxbId();
		id.setName( "id" );
		id.setJdbcTypeCode( type );
		id.setUuidGenerator( new JaxbUuidGenerator() );
		id.getUuidGenerator().setStyle( UuidGenerator.Style.valueOf( strategy.toUpperCase( Locale.ROOT ) ) );
		entity.getAttributes().getId().add( id );

		return this;
	}

	public JaxbEntityMappings build() {
		JaxbEntityMappings mappings = new JaxbEntityMappings();

		mappings.getEntities().add( entity );

		return mappings;
	}

	private JaxbBasic createAttribute(String name, Integer size, boolean nullable) {
		return createAttribute( name, size, nullable, null );
	}

	private JaxbBasic createAttribute(String name, Integer size, boolean nullable, Integer type) {
		JaxbBasic attribute = new JaxbBasic();
		attribute.setName( name );
		JaxbColumn column = new JaxbColumn();
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
