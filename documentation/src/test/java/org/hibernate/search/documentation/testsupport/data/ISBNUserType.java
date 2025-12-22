/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.testsupport.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.EnhancedUserType;

/**
 * In some cases, e.g. with {@link jakarta.persistence.Id}, an {@link jakarta.persistence.AttributeConverter attribute converter}
 * cannot be used, then we'll rely on a user type instead.
 */
public class ISBNUserType implements EnhancedUserType<ISBN> {

	@Override
	public int getSqlType() {
		return Types.VARCHAR;
	}

	@Override
	public Class<ISBN> returnedClass() {
		return ISBN.class;
	}

	@Override
	public ISBN deepCopy(ISBN value) {
		return value == null ? null : ISBN.parse( value.getStringValue() );
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public ISBN nullSafeGet(ResultSet rs, int position, WrapperOptions options) throws SQLException {
		String result = rs.getString( position );
		return rs.wasNull() ? null : ISBN.parse( result );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, ISBN value, int position, WrapperOptions options) throws SQLException {
		if ( value == null ) {
			st.setNull( position, getSqlType() );
		}
		else {
			st.setString( position, value.getStringValue() );
		}
	}

	@Override
	public String toSqlLiteral(ISBN value) {
		return value == null ? null : value.getStringValue();
	}

	@Override
	public String toString(ISBN value) throws HibernateException {
		return toSqlLiteral( value );
	}

	@Override
	public ISBN fromStringValue(CharSequence sequence) throws HibernateException {
		return sequence == null ? null : ISBN.parse( sequence.toString() );
	}
}
