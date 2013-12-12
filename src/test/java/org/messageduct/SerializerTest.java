package org.messageduct;

import static org.junit.Assert.*;

import org.junit.Test;
import org.messageduct.utils.serializer.ConcurrentSerializerWrapper;
import org.messageduct.utils.serializer.KryoSerializer;
import org.messageduct.utils.serializer.Serializer;
import org.messageduct.utils.serializer.SerializerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SerializerTest {


    @Test
    public void testSerializer() throws Exception {

        // Normal single threaded case
        assertSerializerWorks(createKryoSerializerWithRegisteredClasses());

        // Check that concurrent serializer can delegate class registration
        final ConcurrentSerializerWrapper concurrentSerializer = new ConcurrentSerializerWrapper(KryoSerializer.class);
        registerAllowedClasses(concurrentSerializer);
        assertSerializerWorksConcurrently(concurrentSerializer);

        // Test concurrent serializer with factory
        assertSerializerWorksConcurrently(new ConcurrentSerializerWrapper(new SerializerFactory() {
            @Override public Serializer createSerializer() {
                return createKryoSerializerWithRegisteredClasses();
            }
        }));

    }

    private KryoSerializer createKryoSerializerWithRegisteredClasses() {
        final KryoSerializer serializer = new KryoSerializer();
        registerAllowedClasses(serializer);
        return serializer;
    }

    private void registerAllowedClasses(Serializer serializer) {
        serializer.registerAllowedClass(DummyObj.class);
        serializer.registerCommonCollectionClasses();
    }

    private void assertSerializerWorksConcurrently(final Serializer serializer) {

        final AtomicBoolean failed = new AtomicBoolean(false);

        // Run the serializer test with the specified serializer in many threads at the same time
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override public void run() {
                    for (int j = 0; j < 10000; j++) {
                        assertSerializerWorks(serializer);
                    }
                }
            });
            threads.add(thread);
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override public void uncaughtException(Thread t, Throwable e) {
                    failed.set(true);
                    e.printStackTrace();
                }
            });
            thread.start();
        }

        // Wait for all threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (failed.get()) {
            fail("Exception when serializing in multiple threads at the same time");
        }

    }

    private void assertSerializerWorks(Serializer serializer) {

        Map<String, Integer> testMap = new HashMap<String, Integer>();
        testMap.put("foo", 1);
        testMap.put("bar", 3);
        testMap.put("baz", 4);

        Set<String> testSet = new HashSet<String>();
        testSet.add("Foo");
        testSet.add("Bar");
        testSet.add("Baz");

        assertSerializes(serializer, "foobar");
        assertSerializes(serializer, 5);
        assertSerializes(serializer, new ArrayList<Object>(Arrays.asList("foo", "Bar", 3.0, 'a')));
        assertSerializes(serializer, testMap);
        assertSerializes(serializer, testSet);
        assertSerializes(serializer, new DummyObj("bar", 5, 3, new DummyObj("baz"), null));
        assertDoesNotSerialize(serializer,
                               new DummyObj("bar", 5, 3, new DummyObj("baz"), new OtherDummyObj("hidden payload"))
        );
        assertDoesNotSerialize(serializer, new OtherDummyObj("dummy"));
    }

    private void assertSerializes(final Serializer serializer, Object original) {
        final byte[] data = serializer.serialize(original);
        final Object serializedObj = serializer.deserialize(data);
        assertEquals(original, serializedObj);
    }

    private void assertDoesNotSerialize(final Serializer serializer, Object original) {
        try {
            serializer.serialize(original);
            fail("Should not serialize");
        }
        catch (Throwable e) {
            // Ok.
        }
    }


    private static class DummyObj {
        public String name;
        public int i;
        public Integer integer;
        public DummyObj nextDummy;
        public OtherDummyObj otherDummy;

        private DummyObj() {
        }

        private DummyObj(String name) {
            this.name = name;
        }

        private DummyObj(String name,
                         int i,
                         Integer integer,
                         DummyObj nextDummy,
                         OtherDummyObj otherDummy) {
            this.name = name;
            this.i = i;
            this.integer = integer;
            this.nextDummy = nextDummy;
            this.otherDummy = otherDummy;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DummyObj dummyObj = (DummyObj) o;

            if (i != dummyObj.i) return false;
            if (integer != null ? !integer.equals(dummyObj.integer) : dummyObj.integer != null) return false;
            if (name != null ? !name.equals(dummyObj.name) : dummyObj.name != null) return false;
            if (nextDummy != null ? !nextDummy.equals(dummyObj.nextDummy) : dummyObj.nextDummy != null) return false;
            if (otherDummy != null ? !otherDummy.equals(dummyObj.otherDummy) : dummyObj.otherDummy != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + i;
            result = 31 * result + (integer != null ? integer.hashCode() : 0);
            result = 31 * result + (nextDummy != null ? nextDummy.hashCode() : 0);
            result = 31 * result + (otherDummy != null ? otherDummy.hashCode() : 0);
            return result;
        }
    }

    private static class OtherDummyObj {
        public String name;

        private OtherDummyObj() {
        }

        private OtherDummyObj(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OtherDummyObj that = (OtherDummyObj) o;

            if (name != null ? !name.equals(that.name) : that.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }
}
