/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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
public class GeometricsTest {
    
    public GeometricsTest() {
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
     * Test of pointToRectangel method, of class Geometrics.
     */
    @Test
    public void testPointToRectangel_Point_Point() {
        Point p1 = new Point(0, 0);
        Point p2 = new Point(1, 1);
        Rectangle expResult = new Rectangle(0, 0, 1, 1);
        Rectangle result = Geometrics.pointToRectangel(p1, p2);
        assertEquals(expResult, result);
    }

    /**
     * Test of pointToRectangel method, of class Geometrics.
     */
    @Test
    public void testPointToRectangel_Point2D_Point2D() {
        Point2D p1 = new Point(0, 0);
        Point2D p2 = new Point(1, 1);
        Rectangle2D expResult = new Rectangle(0, 0, 1, 1);
        Rectangle2D result = Geometrics.pointToRectangel(p1, p2);
        assertEquals(expResult, result);
    }

    /**
     * Test of pointOnRectangle method, of class Geometrics.
     */
    @Test
    public void testPointOnRectangle() {
        Point point = new Point(1,1);
        Rectangle rectangle = new Rectangle(0, 0, 2, 2);
        int margin = 0;
        Geometrics.EArea expResult = Geometrics.EArea.CENTER;
        Geometrics.EArea result = Geometrics.pointOnRectangle(point, rectangle, margin);
        assertEquals(expResult, result);
    }

    /**
     * Test of pointInRectangle method, of class Geometrics.
     */
    @Test
    public void testPointInRectangle_3args_1() {
        Point point = new Point(1, 1);
        Rectangle rectangle = new Rectangle(0, 0, 2, 2);
        int margin = 0;
        boolean expResult = true;
        boolean result = Geometrics.pointInRectangle(point, rectangle, margin);
        assertEquals(expResult, result);
    }

    /**
     * Test of pointInRectangle method, of class Geometrics.
     */
    @Test
    public void testPointInRectangle_3args_2() {
        Point2D point = new Point(1, 1);
        Rectangle2D rectangle = new Rectangle(0, 0, 2, 2);
        double margin = 0.0;
        boolean expResult = true;
        boolean result = Geometrics.pointInRectangle(point, rectangle, margin);
        assertEquals(expResult, result);
    }

    /**
     * Test of rectangleInRectangle method, of class Geometrics.
     */
    @Test
    public void testRectangleInRectangle() {
        Rectangle r1 = new Rectangle(0, 0, 2, 2);
        Rectangle r2 = new Rectangle(1, 1, 2, 2);
        boolean expResult = true;
        boolean result = Geometrics.rectangleInRectangle(r1, r2);
        assertEquals(expResult, result);
    }

    /**
     * Test of getRatio method, of class Geometrics.
     */
    @Test
    public void testGetRatio_double_double() {
        double w = 1.0;
        double h = 1.0;
        double expResult = 1.0;
        double result = Geometrics.getRatio(w, h);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of getRatio method, of class Geometrics.
     */
    @Test
    public void testGetRatio_int_int() {
        int w = 1;
        int h = 2;
        double expResult = 0.5;
        double result = Geometrics.getRatio(w, h);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of placeRectangle method, of class Geometrics.
     */
    @Test
    public void testPlaceRectangle() {
        int w = 10;
        int h = 10;
        double ratio = 1.0;
        Rectangle expResult = new Rectangle(0, 0, 10, 10);
        Rectangle result = Geometrics.placeRectangle(w, h, ratio);
        assertEquals(expResult, result);        
    }

    /**
     * Test of getScale method, of class Geometrics.
     */
    @Test
    public void testGetScale_4args_1() {
        double w1 = 1.0;
        double h1 = 1.0;
        double w2 = 2.0;
        double h2 = 2.0;
        double expResult = 0.5;
        double result = Geometrics.getScale(w1, h1, w2, h2);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of getScale method, of class Geometrics.
     */
    @Test
    public void testGetScale_4args_2() {
        int w1 = 2;
        int h1 = 2;
        int w2 = 1;
        int h2 = 1;
        double expResult = 2.0;
        double result = Geometrics.getScale(w1, h1, w2, h2);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of manipulateRectangle method, of class Geometrics.
     */
    @Test
    public void testManipulateRectangle() {
        Geometrics.EArea operation = Geometrics.EArea.RIGHT_SIDE;
        Rectangle oldR = new Rectangle(0, 0, 1, 1);
        Point delta = new Point(1, 1);
        Point max = new Point(10, 10);
        double ratio = 1.0;
        Rectangle expResult = new Rectangle(0, 0, 2, 2);
        Rectangle result = Geometrics.manipulateRectangle(operation, oldR, delta, max, ratio);
        assertEquals(expResult, result);
    }

    /**
     * Test of getCursor method, of class Geometrics.
     */
    @Test
    public void testGetCursor() {
        Geometrics.EArea area = Geometrics.EArea.RIGHT_SIDE;
        Cursor expResult = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
        Cursor result = Geometrics.getCursor(area);
        assertEquals(expResult, result);
    }

    /**
     * Test of limitPointInRect method, of class Geometrics.
     */
    @Test
    public void testLimitPointInRect() {
        Point t_pos = new Point(10, 0);
        Rectangle rect = new Rectangle(0, 0, 1, 1);
        Point expResult = new Point(1, 0);
        Point result = Geometrics.limitPointInRect(t_pos, rect);
        assertEquals(expResult, result);
    }


    /**
     * Test of compRectangles method, of class Geometrics.
     */
    @Test
    public void testCompRectangles() {
        Rectangle[] rects = { new Rectangle(0, 0, 1, 1), new Rectangle(3, 3, 1, 1) };
        Rectangle expResult = new Rectangle(0, 0, 4, 4);
        Rectangle result = Geometrics.compRectangles(rects);
        assertEquals(expResult, result);
    }

    /**
     * Test of dimensiontoPoint method, of class Geometrics.
     */
    @Test
    public void testDimensiontoPoint() {
        Dimension d = new Dimension(1, 1);
        Point expResult = new Point(1, 1);
        Point result = Geometrics.dimensiontoPoint(d);
        assertEquals(expResult, result);
    }

    /**
     * Test of scaleRectangleInRectangle method, of class Geometrics.
     */
    @Test
    public void testScaleRectangleInRectangle() {
        Rectangle element = new Rectangle(0, 0, 1, 1);
        Rectangle r_old = new Rectangle(0, 0, 1, 1);
        Rectangle r_new = new Rectangle(2, 2, 4, 4);
        Rectangle expResult = new Rectangle(2, 2, 4, 4);
        Rectangle result = Geometrics.scaleRectangleInRectangle(element, r_old, r_new);
        assertEquals(expResult, result);
    }

    /**
     * Test of scaleRectangleandPos method, of class Geometrics.
     */
    @Test
    public void testScaleRectangleandPos() {
        Rectangle r = new Rectangle(0, 0, 1, 1);
        Point center = new Point(3, 3);
        double scale = 2.0;
        Rectangle expResult = new Rectangle(3, 3, 2, 2);
        Rectangle result = Geometrics.scaleRectangleandPos(r, center, scale);
        assertEquals(expResult, result);
    }

    /**
     * Test of doubleEquals method, of class Geometrics.
     */
    @Test
    public void testDoubleEquals_3args() {
        double a = 0.0;
        double b = 0.01;
        double epsilon = 0.1;
        boolean expResult = true;
        boolean result = Geometrics.doubleEquals(a, b, epsilon);
        assertEquals(expResult, result);
    }

    /**
     * Test of doubleEquals method, of class Geometrics.
     */
    @Test
    public void testDoubleEquals_double_double() {
        double a = 0.0;
        double b = 1.0;
        boolean expResult = false;
        boolean result = Geometrics.doubleEquals(a, b);
        assertEquals(expResult, result);
    }

    /**
     * Test of getDistance method, of class Geometrics.
     */
    @Test
    public void testGetDistance() {
        double x1 = 0.0;
        double y1 = 0.0;
        double x2 = 3.0;
        double y2 = 4.0;
        double expResult = 5.0;
        double result = Geometrics.getDistance(x1, y1, x2, y2);
        assertEquals(expResult, result, 0.0);
    }
    
}
