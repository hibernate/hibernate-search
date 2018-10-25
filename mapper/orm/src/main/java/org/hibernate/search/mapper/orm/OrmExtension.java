/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm;

import java.util.Optional;

import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContextExtension;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.mapper.orm.session.context.HibernateOrmSessionContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContextExtension;

public final class OrmExtension
		implements IdentifierBridgeFromDocumentIdentifierContextExtension<HibernateOrmSessionContext>,
		RoutingKeyBridgeToRoutingKeyContextExtension<HibernateOrmSessionContext>,
		TypeBridgeWriteContextExtension<HibernateOrmSessionContext>,
		PropertyBridgeWriteContextExtension<HibernateOrmSessionContext>,
		FromIndexFieldValueConvertContextExtension<HibernateOrmSessionContext> {

	private static final OrmExtension INSTANCE = new OrmExtension();

	public static OrmExtension get() {
		return INSTANCE;
	}

	private OrmExtension() {
		// Private constructor, use get() instead.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(IdentifierBridgeFromDocumentIdentifierContext original,
			SessionContextImplementor sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(RoutingKeyBridgeToRoutingKeyContext original,
			SessionContextImplementor sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(TypeBridgeWriteContext original,
			SessionContextImplementor sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(PropertyBridgeWriteContext original,
			SessionContextImplementor sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<HibernateOrmSessionContext> extendOptional(FromIndexFieldValueConvertContext original,
			SessionContextImplementor sessionContext) {
		return extendToOrmSessionContext( sessionContext );
	}

	private Optional<HibernateOrmSessionContext> extendToOrmSessionContext(SessionContextImplementor sessionContext) {
		if ( sessionContext instanceof HibernateOrmSessionContext ) {
			return Optional.of( (HibernateOrmSessionContext) sessionContext );
		}
		else {
			return Optional.empty();
		}
	}
}
