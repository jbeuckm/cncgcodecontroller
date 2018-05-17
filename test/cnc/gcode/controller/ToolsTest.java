/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.Color;
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
public class ToolsTest {
    
    public ToolsTest() {
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
     * Test of strtod method, of class Tools.
     */
    @Test
    public void testStrtod() throws Exception {
        String s = "5.67";
        Double expResult = 5.67;
        Double result = Tools.strtod(s);
        assertEquals(expResult, result);
    }

    /**
     * Test of strtodsave method, of class Tools.
     */
    @Test
    public void testStrtodsave() {
        assertEquals(Tools.strtodsave("1.23"), 1.23, 0);
        assertEquals(Tools.strtodsave("abc"), 0.0, 0);
    }

    /**
     * Test of dtostr method, of class Tools.
     */
    @Test
    public void testDtostr() {
        assertEquals(Tools.dtostr(1.23), "1.2300");
    }

    /**
     * Test of formatDuration method, of class Tools.
     */
    @Test
    public void testFormatDuration() {
        assertEquals(Tools.formatDuration(45), "0:00:45");
        assertEquals(Tools.formatDuration(60), "0:01:00");
        assertEquals(Tools.formatDuration(120), "0:02:00");
        assertEquals(Tools.formatDuration(3602), "1:00:02");
        assertEquals(Tools.formatDuration(12345), "3:25:45");
    }

    /**
     * Test of convertToMultiline method, of class Tools.
     */
    @Test
    public void testConvertToMultiline() {
        String orig = "hello\nthere";
        String expResult = "<html>hello<br>there";
        String result = Tools.convertToMultiline(orig);
        assertEquals(expResult, result);
    }


    /**
     * Test of getJarName method, of class Tools.
     */
    @Test
    public void testGetJarName() {
        int len = Tools.getJarName().length();
        assertTrue(len > 0);
    }

    /**
     * Test of adjustInt method, of class Tools.
     */
    @Test
    public void testAdjustInt() {
        assertEquals(Tools.adjustInt(1, 0, 10), 1, 0.0);
        assertEquals(Tools.adjustInt(-1, 0, 10), 0, 0.0);
        assertEquals(Tools.adjustInt(100, 0, 10), 10);
    }

    /**
     * Test of adjustDouble method, of class Tools.
     */
    @Test
    public void testAdjustDouble() {
        assertEquals(Tools.adjustDouble(1, 0, 10), 1, 0.0);
        assertEquals(Tools.adjustDouble(-.1, 0, 10), 0, 0.0);
        assertEquals(Tools.adjustDouble(10.1, 0, 10), 10, 0.0);
    }

    /**
     * Test of setAlpha method, of class Tools.
     */
    @Test
    public void testSetAlpha() {
        Color c = new Color(100);

        Color result = Tools.setAlpha(c, .8);
        assertEquals(result.getAlpha(), 204, 0);
        
        Color result2 = Tools.setAlpha(c, 0);
        assertEquals(result2.getAlpha(), 0, 0);
    }
    
}
