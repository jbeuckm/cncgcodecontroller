/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.geom.Point2D;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author j.beuckman
 */
public class AutoLevelSystemTest {
    
    public AutoLevelSystemTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getPoints method, of class AutoLevelSystem.
     */
    @Test
    public void testGetPoints() {
        AutoLevelSystem instance = new AutoLevelSystem(0, 0, 100, 100);
        AutoLevelSystem.Point[] expResult = new AutoLevelSystem.Point[100];
        AutoLevelSystem.Point[] result = instance.getPoints();
        assertEquals(result.length, 11*11);
    }

    /**
     * Test of isLeveled method, of class AutoLevelSystem.
     */
    @Test
    public void testIsLeveled() {
        AutoLevelSystem instance = new AutoLevelSystem();
        boolean expResult = false;
        boolean result = instance.isLeveled();
        assertEquals(expResult, result);
    }

    /**
     * Test of getdZ method, of class AutoLevelSystem.
     */
    @Test
    public void testGetdZ() {
        Point2D p = null;
        AutoLevelSystem instance = new AutoLevelSystem();
        double expResult = 0.0;
        double result = instance.getdZ(p);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of publish method, of class AutoLevelSystem.
     */
    @Test
    public void testPublish() {
        AutoLevelSystem al = null;
        AutoLevelSystem.publish(al);
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * Test of correctZ method, of class AutoLevelSystem.
     */
    @Test
    public void testCorrectZ() {
        AutoLevelSystem instance = new AutoLevelSystem();
        AutoLevelSystem.publish(instance);

        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        double expResult = 0.0;
        
        double result = AutoLevelSystem.correctZ(x, y, z);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of leveled method, of class AutoLevelSystem.
     */
    @Test
    public void testLeveled() {
        boolean expResult = false;
        boolean result = AutoLevelSystem.leveled();
        assertEquals(expResult, result);
    }
    
}
