package org.hibernate.search.test.backends.jgroups;

import org.hibernate.search.backend.impl.jgroups.AutoNodeSelector;
import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.View;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JGroups' Auto Node Selector Test
 * <p/>
 * Test if the selector choose a valid member
 *
 * @author Pedro Ruivo
 * @since 4.3
 */
public class AutoNodeSelectorTest {

   private static final String NEGATIVE_HASH_CODE_INDEX_NAME = "test.Book";
   private static final String POSITIVE_HASH_CODE_INDEX_NAME = "test.Author";
   private static final String ZERO_HASH_CODE_INDEX_NAME = "";
   private static final AtomicInteger NEXT_ADDRESS_ID = new AtomicInteger(0);
   private static final AtomicLong NEXT_VIEW_ID = new AtomicLong(0);

   @Test
   public void testIndexNameUsed() {
      assert NEGATIVE_HASH_CODE_INDEX_NAME.hashCode() < 0;
      assert ZERO_HASH_CODE_INDEX_NAME.hashCode() == 0;
      assert POSITIVE_HASH_CODE_INDEX_NAME.hashCode() > 0;
   }

   @Test
   public void testNegativeHashCodeIndex() {
      performTest(NEGATIVE_HASH_CODE_INDEX_NAME);
   }

   @Test
   public void testPositiveHashCodeIndex() {
      performTest(POSITIVE_HASH_CODE_INDEX_NAME);
   }

   @Test
   public void testZeroHashCodeIndex() {
      performTest(ZERO_HASH_CODE_INDEX_NAME);
   }

   private void performTest(String index) {
      AutoNodeSelector selector = new AutoNodeSelector(index);

      /*
      there is 4 main cases:
      1) when the view has 1 member;
      2) when the view has 2 members;
      3) when the view has 3 member
      4) when the view has > 3 members;

      some view size can trigger the bug and other don't. test with a wide range
      */

      for (int viewSize = 1; viewSize <= 50; ++viewSize) {
         View view = createView(viewSize);
         assert view.getMembers().size() == viewSize;
         selector.viewAccepted(view);
      }
   }

   private View createView(int size) {
      List<Address> addressList = new LinkedList<Address>();
      while (size-- > 0) {
         addressList.add(new TestAddress(NEXT_ADDRESS_ID.incrementAndGet()));
      }
      return new View(addressList.get(0), NEXT_VIEW_ID.incrementAndGet(), addressList);
   }

   private final class TestAddress implements Address {

      private int addressId;

      public TestAddress(int addressId) {
         this.addressId = addressId;
      }

      @SuppressWarnings("UnusedDeclaration")
      public TestAddress() {
      }

      @Override
      public int size() {
         return Global.INT_SIZE;
      }

      @Override
      public int compareTo(Address o) {
         if (o == null || !(o instanceof TestAddress)) {
            return -1;
         }

         return Integer.valueOf(addressId).compareTo(((TestAddress) o).addressId);
      }

      @Override
      public void writeExternal(ObjectOutput out) throws IOException {
         out.writeInt(addressId);
      }

      @Override
      public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
         addressId = in.readInt();
      }

      @Override
      public void writeTo(DataOutput dataOutput) throws Exception {
         dataOutput.writeInt(addressId);
      }

      @Override
      public void readFrom(DataInput dataInput) throws Exception {
         addressId = dataInput.readInt();
      }

      @Override
      public String toString() {
         return "TestAddress{" +
               "addressId=" + addressId +
               '}';
      }
   }
}
