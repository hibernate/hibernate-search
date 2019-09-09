/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm;

import java.util.Optional;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContextExtension;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContextExtension;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.mapper.orm.mapping.context.HibernateOrmMappingContext;
import org.hibernate.search.mapper.orm.session.context.HibernateOrmSessionContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContextExtension;

/**
 * An extension for the Hibernate ORM mapper, giving access to Hibernate ORM-specific contexts.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called directly by users.
 * In short, users are only expected to get instances of this type from an API and pass it to another API.
 *
 * @see #get()
 */
public final class HibernateOrmExtension
		implements IdentifierBridgeToDocumentIdentifierContextExtension<HibernateOrmMappingContext>,
		IdentifierBridgeFromDocumentIdentifierContextExtension<HibernateOrmSessionContext>,
		RoutingKeyBridgeToRoutingKeyContextExtension<HibernateOrmSessionContext>,
		TypeBridgeWriteContextExtension<HibernateOrmSessionContext>,
		PropertyBridgeWriteContextExtension<HibernateOrmSessionContext>,
		ValueBridgeToIndexedValueContextExtension<HibernateOrmMappingContext>,
		ValueBridgeFromIndexedValueContextExtension<HibernateOrmSessionContext>,
		ToDocumentFieldValueConvertContextExtension<HibernateOrmMappingContext>,
		FromDocumentFieldValueConvertContextExtension<HibernateOrmSessionContext> {

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
			BackendMappingContext mappingContext) {
		return extendToOrmMappingContext( mappingContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(IdentifierBridgeFromDocumentIdentifierContext original,
			BackendSessionContext sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(RoutingKeyBridgeToRoutingKeyContext original,
			BackendSessionContext sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(TypeBridgeWriteContext original,
			BackendSessionContext sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(PropertyBridgeWriteContext original,
			BackendSessionContext sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmMappingContext> extendOptional(ValueBridgeToIndexedValueContext original,
			BackendMappingContext mappingContext) {
		return extendToOrmMappingContext( mappingContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(ValueBridgeFromIndexedValueContext original,
			BackendSessionContext sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmMappingContext> extendOptional(ToDocumentFieldValueConvertContext original,
			BackendMappingContext mappingContext) {
		return extendToOrmMappingContext( mappingContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(FromDocumentFieldValueConvertContext original,
			BackendSessionContext sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	private Optional<HibernateOrmMappingContext> extendToOrmMappingContext(BackendMappingContext mappingContext) {
		if ( mappingContext instanceof HibernateOrmMappingContext ) {
			return Optional.of( (HibernateOrmMappingContext) mappingContext );
		}
		else {
			return Optional.empty();
		}
	}

	private Optional<HibernateOrmSessionContext> extendToOrmSessionContext(BackendSessionContext sessionContext) {
		if ( sessionContext instanceof HibernateOrmSessionContext ) {
			return Optional.of( (HibernateOrmSessionContext) sessionContext );
		}
		else {
			return Optional.empty();
		}
	}
}
