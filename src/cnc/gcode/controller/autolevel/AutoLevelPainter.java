/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller.autolevel;

import cnc.gcode.controller.DatabaseV2;
import cnc.gcode.controller.Geometrics;
import cnc.gcode.controller.Tools;
import de.unikassel.ann.util.ColorHelper;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 *
 * @author j.beuckman
 */
public class AutoLevelPainter {
    
    private static Point2D.Double paintHeatmap(AutoLevelSystem al, int paintableWidth, int paintableHeight, AffineTransform trans, Graphics2D g2) {

        double autolevelDistance = DatabaseV2.ALDISTANCE.getsaved();
        double areaWidth    = DatabaseV2.WORKSPACE0.getsaved(); //x
        double areaHeight   = DatabaseV2.WORKSPACE1.getsaved(); //y
        Rectangle rect      = Geometrics.placeRectangle(paintableWidth, paintableHeight, Geometrics.getRatio(areaWidth,areaHeight));
        double scaleX       = rect.width / areaWidth;
        double scaleY       = rect.height / areaHeight;

        double max  = -Double.MAX_VALUE;
        double min  =  Double.MAX_VALUE;
        for(AutoLevelSystem.Point p:al.getPoints())
        {
            double value = p.getValue();
            max = Math.max(max, value);
            min = Math.min(min, value);
        }
        double delta = max - min;
        g2.setTransform(new AffineTransform());
        int cx  = Math.max((int)(areaWidth/autolevelDistance * 10), rect.width);
        int cy  = Math.max((int)(areaHeight/autolevelDistance * 10), rect.height);
        double w    = rect.width / (double)cx;
        double h    = rect.height / (double)cy;
        for (int x = 0; x < cx; x++)
        {
            for (int y = 0; y < cy; y++)
            {
                Point2D p = trans.transform(new Point2D.Double((double)rect.x + ((double)x + 0.5) * w,
                                                               (double)rect.y + ((double)y + 0.5) * h),
                                                                null);
                double z        =   al.getdZ(p);
                double relative =   (z - min)/delta;
                relative = Tools.adjustDouble(relative, 0, 1);

                g2.setColor(ColorHelper.numberToColorPercentage(relative));
                g2.fillRect((int)Math.floor(rect.x + x * w),
                            (int)Math.floor(rect.y + y * h),
                            (int)Math.ceil(w),
                            (int)Math.ceil(h));
            }
        }
        
        return new Point2D.Double(min, max);
    }
    
    private static void paintScale( Graphics2D g2, Rectangle rect, int boardAreaWidth, double min, double max ) {

        double delta = max - min;

        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
        g2.setFont(font);
        int zh      = (int)(font.getStringBounds(Tools.dtostr(100.0), g2.getFontRenderContext()).getHeight()) + 10;                    
        int elements= rect.height / zh;
        int dy      = (rect.height - elements * zh) / 2;
        
        for (int i = 0; i < elements && elements >= 2; i++)
        {
            double z = max - i * (delta / (elements - 1));
            double relative = (z - min) / delta;
            relative = Tools.adjustDouble(relative, 0, 1);

            Color c = ColorHelper.numberToColorPercentage(relative);
            g2.setColor(c);
            g2.fillRect(boardAreaWidth + 5,
                        dy + zh * i,
                        90,
                        zh - 4);
            g2.setColor(((299 * c.getRed() + 587 * c.getGreen() + 114 * c.getBlue())> 128000) ? Color.black:Color.white);
            g2.drawString(Tools.dtostr(z), boardAreaWidth + 10, dy + zh * i + zh - 10);
            g2.setColor(Color.black);
            g2.drawRect(boardAreaWidth + 5,
                        dy + zh * i,
                        90,
                        zh - 4);
        }

    }
    
    private static void paintProgressPoints(AutoLevelSystem.Point[] points, Graphics2D g2) {

        double autolevelDistance = DatabaseV2.ALDISTANCE.getsaved();
        double d = Math.min(autolevelDistance/10.0, 10.0);

        for (AutoLevelSystem.Point p:points)
        {
            if(p.isLeveled())
            {
                g2.setColor(Color.green);
            }
            else
            {
                g2.setColor(Color.black);
            }

            g2.fill(new Ellipse2D.Double((double)(p.getPoint().x) - d / 2.0, (double)(p.getPoint().y) - d / 2.0, d, d));

        }
    }
    
    private static void scaleForStartCorner(int paintableWidth, int paintableHeight, Graphics2D g2) {
        g2.translate(paintableWidth / 2, paintableHeight / 2);
        switch (DatabaseV2.EHoming.get()) 
        {
            case UPPER_LEFT:
            default:
                g2.scale(1,1);                
                break;
            case UPPER_RIGHT:
                g2.scale(-1,1);
                break;
            case LOWER_LEFT:
                g2.scale(1,-1);
                break;
            case LOWER_RIGHT:                
                g2.scale(-1,-1);
                break;
        }
        g2.translate(-paintableWidth / 2, -paintableHeight / 2);
    }
    
    public static BufferedImage paint(AutoLevelSystem al, int paintableWidth, int paintableHeight, AffineTransform trans) {

        paintableWidth = Tools.adjustInt(paintableWidth, 1, Integer.MAX_VALUE);
        paintableHeight = Tools.adjustInt(paintableHeight, 1, Integer.MAX_VALUE);

        BufferedImage image = new BufferedImage(paintableWidth, paintableHeight, BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        //Scalling transforming ...
        if(AutoLevelSystem.leveled())
        {
            paintableWidth -= 100;
        }

        scaleForStartCorner(paintableWidth, paintableHeight, g2);

        //Display Position
        double areaWidth    = DatabaseV2.WORKSPACE0.getsaved(); //x
        double areaHeight   = DatabaseV2.WORKSPACE1.getsaved(); //y
        Rectangle rect      = Geometrics.placeRectangle(paintableWidth, paintableHeight, Geometrics.getRatio(areaWidth, areaHeight));

        double scaleX       = rect.width / areaWidth;
        double scaleY       = rect.height / areaHeight;
        
        g2.translate(rect.x, rect.y);
        g2.scale(scaleX, scaleY);

        try {
            AffineTransform t = g2.getTransform();
            t.invert();
            trans.setTransform(t);
        } catch (NoninvertibleTransformException ex) 
        {
            trans.setTransform(new AffineTransform());
        }

        //Draw base
        g2.setColor(new Color(Integer.parseInt(DatabaseV2.CBACKGROUND.get())));
        g2.fill(new Rectangle2D.Double(0, 0, areaWidth, areaHeight));


        if (AutoLevelSystem.leveled())
        {
            Point2D.Double extent = paintHeatmap(al, paintableWidth, paintableHeight, trans, g2);
            paintScale( g2, rect, paintableWidth, extent.x, extent.y );
            
            g2.translate(rect.x, rect.y);
            g2.scale(scaleX, scaleY);
        }
        else
        {
            paintProgressPoints(al.getPoints(), g2);
        }

        double gridDistance = DatabaseV2.CGRIDDISTANCE.getsaved();

        //Draw coordinate plane
        if (gridDistance > 0) {
            g2.setColor(new Color(Integer.parseInt(DatabaseV2.CGRID.get())));

            g2.setStroke(new BasicStroke((float)(1.0/scaleX)));
            for (int x=1; x<areaWidth/gridDistance; x++) {
                Shape l = new Line2D.Double((double)x * gridDistance, 0.0, (double)x * gridDistance, areaHeight);
                g2.draw(l);
            }

            g2.setStroke(new BasicStroke((float)(1.0/scaleY)));
            for (int y=1; y<areaHeight/gridDistance; y++) {
                Shape l = new Line2D.Double(0.0, ((double)y * gridDistance), areaWidth, ((double)y * gridDistance));
                g2.draw(l);
            }
        }

        return image;        
    }
}
