package org.messageduct;

import static org.junit.Assert.*;

import org.junit.Test;
import org.messageduct.utils.service.ServiceBase;

public class ServiceTest {

    @Test
    public void testService() throws Exception {
        DummyService dummyService = new DummyService();

        assertEquals(0, dummyService.doInitCallCount);
        assertEquals(0, dummyService.doShutdownCallCount);
        assertEquals(false, dummyService.isInitialized());
        assertEquals(false, dummyService.isActive());
        assertEquals(false, dummyService.isShutdown());

        dummyService.init();

        assertEquals(1, dummyService.doInitCallCount);
        assertEquals(0, dummyService.doShutdownCallCount);
        assertEquals(true, dummyService.isInitialized());
        assertEquals(true, dummyService.isActive());
        assertEquals(false, dummyService.isShutdown());

        dummyService.shutdown();

        assertEquals(1, dummyService.doInitCallCount);
        assertEquals(1, dummyService.doShutdownCallCount);
        assertEquals(true, dummyService.isInitialized());
        assertEquals(false, dummyService.isActive());
        assertEquals(true, dummyService.isShutdown());

        try {
            dummyService.shutdown();
            fail("Should throw exception");
        }
        catch(IllegalStateException e) {
            // Ok
        }

        assertEquals(1, dummyService.doInitCallCount);
        assertEquals(1, dummyService.doShutdownCallCount);
        assertEquals(true, dummyService.isInitialized());
        assertEquals(false, dummyService.isActive());
        assertEquals(true, dummyService.isShutdown());

    }


    @Test
    public void testImmediateShutdown() throws Exception {
        DummyService dummyService = new DummyService();

        assertEquals(0, dummyService.doInitCallCount);
        assertEquals(0, dummyService.doShutdownCallCount);
        assertEquals(false, dummyService.isInitialized());
        assertEquals(false, dummyService.isActive());
        assertEquals(false, dummyService.isShutdown());

        dummyService.shutdown();

        assertEquals(0, dummyService.doInitCallCount);
        assertEquals(0, dummyService.doShutdownCallCount);
        assertEquals(false, dummyService.isInitialized());
        assertEquals(false, dummyService.isActive());
        assertEquals(true, dummyService.isShutdown());

    }

    @Test
    public void testMultipleInit() throws Exception {
        DummyService dummyService = new DummyService();

        assertEquals(0, dummyService.doInitCallCount);
        assertEquals(0, dummyService.doShutdownCallCount);
        assertEquals(false, dummyService.isInitialized());
        assertEquals(false, dummyService.isActive());
        assertEquals(false, dummyService.isShutdown());

        dummyService.init();

        assertEquals(1, dummyService.doInitCallCount);
        assertEquals(0, dummyService.doShutdownCallCount);
        assertEquals(true, dummyService.isInitialized());
        assertEquals(true, dummyService.isActive());
        assertEquals(false, dummyService.isShutdown());

        try {
            dummyService.init();
            fail("Should throw exception");
        }
        catch(IllegalStateException e) {
            // Ok
        }

        assertEquals(1, dummyService.doInitCallCount);
        assertEquals(0, dummyService.doShutdownCallCount);
        assertEquals(true, dummyService.isInitialized());
        assertEquals(true, dummyService.isActive());
        assertEquals(false, dummyService.isShutdown());

    }


    private static class DummyService extends ServiceBase {
        public int doInitCallCount = 0;
        public int doShutdownCallCount = 0;

        @Override protected void doInit() {
            doInitCallCount++;
        }

        @Override protected void doShutdown() {
            doShutdownCallCount++;
        }
    }
}
