/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm;

import java.util.Optional;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContextExtension;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContextExtension;
import org.hibernate.search.mapper.orm.mapping.context.HibernateOrmMappingContext;
import org.hibernate.search.mapper.orm.session.context.HibernateOrmSessionContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;

/**
 * An extension for the Hibernate ORM mapper, giving access to Hibernate ORM-specific contexts.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called directly by users.
 * In short, users are only expected to get instances of this type from an API and pass it to another API.
 *
 * @see #get()
 */
@SuppressWarnings("deprecation")
public final class HibernateOrmExtension
		implements IdentifierBridgeToDocumentIdentifierContextExtension<HibernateOrmMappingContext>,
		IdentifierBridgeFromDocumentIdentifierContextExtension<HibernateOrmSessionContext>,
		RoutingBridgeRouteContextExtension<HibernateOrmSessionContext>,
		TypeBridgeWriteContextExtension<HibernateOrmSessionContext>,
		PropertyBridgeWriteContextExtension<HibernateOrmSessionContext>,
		ValueBridgeToIndexedValueContextExtension<HibernateOrmMappingContext>,
		ValueBridgeFromIndexedValueContextExtension<HibernateOrmSessionContext>,
		org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContextExtension<
				HibernateOrmMappingContext>,
		ToDocumentValueConvertContextExtension<HibernateOrmMappingContext>,
		org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContextExtension<
				HibernateOrmSessionContext>,
		FromDocumentValueConvertContextExtension<HibernateOrmSessionContext> {

	private static final HibernateOrmExtension INSTANCE = new HibernateOrmExtension();

	public static HibernateOrmExtension get() {
		return INSTANCE;
	}

	private HibernateOrmExtension() {
		// Private constructor, use get() instead.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmMappingContext> extendOptional(IdentifierBridgeToDocumentIdentifierContext original,
			BridgeMappingContext mappingContext) {
		return extendToOrmMappingContext( mappingContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(IdentifierBridgeFromDocumentIdentifierContext original,
			BridgeSessionContext sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(RoutingBridgeRouteContext original,
			BridgeSessionContext sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(TypeBridgeWriteContext original,
			BridgeSessionContext sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(PropertyBridgeWriteContext original,
			BridgeSessionContext sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmMappingContext> extendOptional(ValueBridgeToIndexedValueContext original,
			BridgeMappingContext mappingContext) {
		return extendToOrmMappingContext( mappingContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(ValueBridgeFromIndexedValueContext original,
			BridgeSessionContext sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @deprecated Use {@link org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter}
	 * and {@link org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext} instead.
	 */
	@Override
	@Deprecated
	public Optional<HibernateOrmMappingContext> extendOptional(
			org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext original,
			BackendMappingContext mappingContext) {
		return extendToOrmMappingContext( mappingContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmMappingContext> extendOptional(ToDocumentValueConvertContext original,
			BackendMappingContext mappingContext) {
		return extendToOrmMappingContext( mappingContext );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @deprecated Use {@link org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter}
	 * and {@link org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext} instead.
	 */
	@Override
	@Deprecated
	public Optional<HibernateOrmSessionContext> extendOptional(
			org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext original,
			BackendSessionContext sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(FromDocumentValueConvertContext original,
			BackendSessionContext sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	private Optional<HibernateOrmMappingContext> extendToOrmMappingContext(Object mappingContext) {
		if ( mappingContext instanceof HibernateOrmMappingContext ) {
			return Optional.of( (HibernateOrmMappingContext) mappingContext );
		}
		else {
			return Optional.empty();
		}
	}

	private Optional<HibernateOrmSessionContext> extendToOrmSessionContext(Object sessionContext) {
		if ( sessionContext instanceof HibernateOrmSessionContext ) {
			return Optional.of( (HibernateOrmSessionContext) sessionContext );
		}
		else {
			return Optional.empty();
		}
	}
}
