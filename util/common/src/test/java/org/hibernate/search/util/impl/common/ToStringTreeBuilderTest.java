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
		assertThat( toString( ToStringStyle.inlineDelimiterStructure() ) )
				.isEqualTo(
						"foo=value, children={"
						+ " childrenFoo=23, child1={ child1Foo=customToString, [ foo, 42 ] }, emptyChild={ },"
						+ " appendable={ attr=val, nested={ attr=val2 } },"
						+ " appendableAsObject={ attr=val, nested={ attr=val2 } },"
						+ " nullAppendable=null,"
						+ " list=[ { name=foo }, object={ name=foo, attr=bar }, { nestedList=[ first, second ], name=bar } ]"
						+ " }, bar=value"
				);
		assertThat( new ToStringTreeBuilder().toString() ).isEqualTo( "" );
		assertThat( new ToStringTreeBuilder().startObject( "" ).endObject().toString() )
				.isEqualTo( "{ }" );
	}

	@Test
	public void multiLineStyle() {
		assertThat( toString( ToStringStyle.multilineDelimiterStructure() ) )
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
						+ "\t\tobject={\n"
						+ "\t\t\tname=foo\n"
						+ "\t\t\tattr=bar\n"
						+ "\t\t}\n"
						+ "\t\t{\n"
						+ "\t\t\tnestedList=[\n"
						+ "\t\t\t\tfirst\n"
						+ "\t\t\t\tsecond\n"
						+ "\t\t\t]\n"
						+ "\t\t\tname=bar\n"
						+ "\t\t}\n"
						+ "\t]\n"
						+ "}\n"
						+ "bar=value"
				);
		assertThat( new ToStringTreeBuilder( ToStringStyle.multilineDelimiterStructure() ).toString() ).isEqualTo( "" );
		assertThat( new ToStringTreeBuilder( ToStringStyle.multilineDelimiterStructure() ).startObject( "" ).endObject().toString() )
				.isEqualTo( "{\n}" );
	}

	@Test
	public void multiLineLightStyle() {
		ToStringStyle style = ToStringStyle.multilineIndentStructure(
				": ", "    ", "  - ", "    "
		);
		assertThat( toString( style ) )
				.isEqualTo(
						"foo: value\n"
						+ "children: \n"
						+ "    childrenFoo: 23\n"
						+ "    child1: \n"
						+ "        child1Foo: customToString\n"
						+ "          - foo\n"
						+ "          - 42\n"
						+ "    emptyChild: \n"
						+ "    appendable: \n"
						+ "        attr: val\n"
						+ "        nested: \n"
						+ "            attr: val2\n"
						+ "    appendableAsObject: \n"
						+ "        attr: val\n"
						+ "        nested: \n"
						+ "            attr: val2\n"
						+ "    nullAppendable: null\n"
						+ "    list: \n"
						+ "      - name: foo\n"
						+ "      - object: \n"
						+ "            name: foo\n"
						+ "            attr: bar\n"
						+ "      - nestedList: \n"
						+ "          - first\n"
						+ "          - second\n"
						+ "        name: bar\n"
						+ "bar: value"
				);
		assertThat( new ToStringTreeBuilder( style ).toString() ).isEqualTo( "" );
		assertThat( new ToStringTreeBuilder( style ).startObject( "" ).endObject().toString() )
				.isEqualTo( "" );
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
						.startObject( "object" )
								.attribute( "name", "foo" )
								.attribute( "attr", "bar" )
								.endObject()
						.startObject()
							.startList( "nestedList" )
								.value( "first" )
								.value( "second" )
								.endList()
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
