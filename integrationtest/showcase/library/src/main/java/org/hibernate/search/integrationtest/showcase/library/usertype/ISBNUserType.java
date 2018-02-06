/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.usertype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.search.integrationtest.showcase.library.model.ISBN;
import org.hibernate.type.StringType;
import org.hibernate.usertype.UserType;

public class ISBNUserType implements UserType {

	public static final String NAME = "org.hibernate.search.integrationtest.showcase.library.usertype.ISBNUserType";

	private final StringType delegateType = new StringType();

	@Override
	public int[] sqlTypes() {
		return new int[]{ delegateType.sqlType() };
	}

	@Override
	public Class<?> returnedClass() {
		return ISBN.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		return Objects.equals( x, y );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return Objects.hashCode( x );
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		String value = (String) delegateType.nullSafeGet( rs, names, session, owner );
		if ( value == null ) {
			return null;
		}
		else {
			return new ISBN( value );
		}
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( value == null ) {
			delegateType.nullSafeSet( st, null, index, session );
		}
		else {
			delegateType.nullSafeSet( st, ( (ISBN) value ).getStringValue(), index, session );
		}
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		return value; // type is immutable
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) value; // type is immutable
	}

	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return cached; // type is immutable
	}

	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original; // type is immutable
	}

}
