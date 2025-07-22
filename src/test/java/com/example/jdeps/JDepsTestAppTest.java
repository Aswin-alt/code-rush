package com.example.jdeps;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Map;

/**
 * Test class for JDepsTestApp
 */
public class JDepsTestAppTest {

    public JDepsTestAppTest() {
        super();
    }

    private JDepsTestApp app;

    @Before
    public void setUp() {
        app = new JDepsTestApp();
    }

    @Test
    public void testApplicationInitialization() {
        assertNotNull("App should be initialized", app);
        assertNotNull("ObjectMapper should be initialized", app.getObjectMapper());
        assertNotNull("DataStore should be initialized", app.getDataStore());
    }

    @Test
    public void testDemonstrateDependencies() {
        // This test verifies that the method runs without exceptions
        try {
            app.demonstrateDependencies();
            
            // Check if data was stored during the demonstration
            Map<String, Object> dataStore = app.getDataStore();
            assertTrue("DataStore should contain data after demonstration", 
                      dataStore.size() > 0);
            
        } catch (Exception e) {
            fail("demonstrateDependencies should not throw exceptions: " + e.getMessage());
        }
    }

    @Test
    public void testDataStoreOperations() {
        Map<String, Object> initialStore = app.getDataStore();
        int initialSize = initialStore.size();
        
        // Run the demonstration which should populate the data store
        app.demonstrateDependencies();
        
        Map<String, Object> finalStore = app.getDataStore();
        assertTrue("DataStore should have more entries after demonstration",
                  finalStore.size() > initialSize);
    }

    @Test
    public void testUtilityMethods() {
        // Test the utility class methods
        try {
            DependencyTestUtil.testFileOperations();
            DependencyTestUtil.testCommonsUtilities();
            DependencyTestUtil.testCollectionOperations();
            DependencyTestUtil.testRegexOperations();
        } catch (Exception e) {
            fail("Utility methods should not throw exceptions: " + e.getMessage());
        }
    }
}
