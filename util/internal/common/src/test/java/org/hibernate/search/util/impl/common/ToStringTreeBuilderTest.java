/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ToStringTreeBuilderTest {

	@Test
	public void inlineStyle() {
		assertThat( new ToStringTreeBuilder().toString() ).isEqualTo( "" );
		assertThat( new ToStringTreeBuilder().startObject( "" ).endObject().toString() )
				.isEqualTo( "{ }" );
		assertThat( toString( ToStringStyle.INLINE ) )
				.isEqualTo(
						"foo=value, children={"
						+ " childrenFoo=23, child1={ child1Foo=customToString, [ foo, 42 ] }, emptyChild={ },"
						+ " appendable={ attr=val, nested={ attr=val2 } },"
						+ " appendableAsObject={ attr=val, nested={ attr=val2 } },"
						+ " nullAppendable=null,"
						+ " list=[ { name=foo }, { name=bar } ]"
						+ " }, bar=value"
				);
	}

	@Test
	public void multiLineStyle() {
		assertThat( new ToStringTreeBuilder( ToStringStyle.MULTILINE ).toString() ).isEqualTo( "" );
		assertThat( new ToStringTreeBuilder( ToStringStyle.MULTILINE ).startObject( "" ).endObject().toString() )
				.isEqualTo( "{\n}" );
		assertThat( toString( ToStringStyle.MULTILINE ) )
				.isEqualTo(
						"foo=value\n"
						+ "children={\n"
						+ "\tchildrenFoo=23\n"
						+ "\tchild1={\n"
						+ "\t\tchild1Foo=customToString\n"
						+ "\t\t[\n"
						+ "\t\t\tfoo\n"
						+ "\t\t\t42\n"
						+ "\t\t]\n"
						+ "\t}\n"
						+ "\temptyChild={\n"
						+ "\t}\n"
						+ "\tappendable={\n"
						+ "\t\tattr=val\n"
						+ "\t\tnested={\n"
						+ "\t\t\tattr=val2\n"
						+ "\t\t}\n"
						+ "\t}\n"
						+ "\tappendableAsObject={\n"
						+ "\t\tattr=val\n"
						+ "\t\tnested={\n"
						+ "\t\t\tattr=val2\n"
						+ "\t\t}\n"
						+ "\t}\n"
						+ "\tnullAppendable=null\n"
						+ "\tlist=[\n"
						+ "\t\t{\n"
						+ "\t\t\tname=foo\n"
						+ "\t\t}\n"
						+ "\t\t{\n"
						+ "\t\t\tname=bar\n"
						+ "\t\t}\n"
						+ "\t]\n"
						+ "}\n"
						+ "bar=value"
				);
	}

	private static String toString(ToStringStyle style) {
		ToStringTreeBuilder builder = new ToStringTreeBuilder( style );
		return builder.attribute( "foo", "value" )
				.startObject( "children" )
					.attribute( "childrenFoo", 23 )
					.startObject( "child1" )
						.attribute( "child1Foo", new Object() {
							@Override
							public String toString() {
								return "customToString";
							}
						} )
						.startList()
							.value( "foo" )
							.value( 42 )
							.endList()
						.endObject()
					.startObject( "emptyChild" )
						.endObject()
					.attribute( "appendable", new Appendable() )
					.attribute( "appendableAsObject", (Object) new Appendable() )
					.attribute( "nullAppendable", (Appendable) null )
					.startList( "list" )
						.startObject()
							.attribute( "name", "foo" )
							.endObject()
						.startObject()
							.attribute( "name", "bar" )
							.endObject()
						.endList()
					.endObject()
				.attribute( "bar", "value" )
				.toString();
	}

	private static class Appendable implements ToStringTreeAppendable {
		@Override
		public void appendTo(ToStringTreeBuilder builder) {
			builder.attribute( "attr", "val" );
			builder.startObject( "nested" );
			builder.attribute( "attr", "val2" );
			builder.endObject();
		}
	}

}
