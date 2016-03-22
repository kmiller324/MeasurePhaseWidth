import ij.plugin.filter.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.event.*;
import ij.plugin.PlugIn;
import ij.util.*;
import ij.measure.*;
import ij.plugin.Straightener;

/** This plugin generates and displays the width of an object imaged using Phase Microscopy, by analyzing the derivative of pixel intensity across the image. Written by Kyle E. Miller of Michigan State
 University 3_22_16. The underlying idea is that pixel intensity will change most sharply across the edges of objects in phase images. This was then validated by comparing the width of objects using phase and scanning electron microscopy.*/


public class Measure_Phase_Width implements PlugInFilter {

    ImagePlus imp;
    static boolean verticalProfile;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_ALL+NO_UNDO+NO_CHANGES;
    }

    public void run(ImageProcessor ip) {
        boolean averageHorizontally = verticalProfile;
        new ProfilePlotAW(imp, averageHorizontally).createWindow();
    }
    
}

// This is a class based off the ProfilePlot.java source code. It creates a graph, like the one used by the plot profile command, in which the data can be viewed and exported. As this is part of the ImageJ source code, Wayne Rasband is likely to be the original author.

 class ProfilePlotAW {
    
    //setup the window to output the data
    static final int MIN_WIDTH = 350;
    static final double ASPECT_RATIO = 1;
    private double min, max;
    private boolean minAndMaxCalculated;
    private static double fixedMin = Prefs.getDouble("pp.min",0.0);
    private static double fixedMax = Prefs.getDouble("pp.max",0.0);
    protected ImagePlus imp;
    protected double[] profile;
    protected double magnification;
    protected double xInc;
    protected String units;
    protected String yLabel;
    protected float[] xValues;
    
    public ProfilePlotAW(ImagePlus imp, boolean averageHorizontally) {
        this.imp = imp;
        Roi roi = imp.getRoi();
        if (roi==null) {
            IJ.error("Profile Plot", "Selection required.");
            return;
        }
        int roiType = roi.getType();
        if (!(roi.isLine() || roiType==Roi.RECTANGLE)) {
            IJ.error("Line or rectangular selection required.");
            return;
        }
        
        Calibration cal = imp.getCalibration();
        xInc = cal.pixelWidth;
        units = cal.getUnits();
        yLabel = "Width (pixels)";
        
        ImageProcessor ip = imp.getProcessor();
        
        profile = getColumnAverageProfile(roi.getBounds(), ip);
        
        
        ip.setCalibrationTable(null);
        ImageCanvas ic = imp.getCanvas();
        if (ic!=null)
            magnification = ic.getMagnification();
        else
            magnification = 1.0;
    }
    
    
    /** Returns the size of the plot that createWindow() creates. */
    public Dimension getPlotSize() {
        if (profile==null) return null;
        int width = (int)(profile.length*magnification);
        int height = (int)(width*ASPECT_RATIO);
        if (width<MIN_WIDTH) {
            width = MIN_WIDTH;
            height = (int)(width*ASPECT_RATIO);
        }
        Dimension screen = IJ.getScreenSize();
        int maxWidth = Math.min(screen.width-200, 1000);
        if (width>maxWidth) {
            width = maxWidth;
            height = (int)(width*ASPECT_RATIO);
        }
        return new Dimension(width, height);
    }
    
/** Displays the profile of width plot in a window. */
    public void createWindow() {
        if (profile==null)
            return;
        Dimension d = getPlotSize();
        String xLabel = "Distance ("+units+")";
        int n = profile.length;
        if (xValues==null) {
            xValues = new float[n];
            for (int i=0; i<n; i++)
                xValues[i] = (float)(i*xInc);
        }
        float[] yValues = new float[n];
        for (int i=0; i<n; i++)
            yValues[i] = (float)profile[i];
        boolean fixedYScale = fixedMin!=0.0 || fixedMax!=0.0;
        Plot plot = new Plot("Plot of "+getShortTitle(imp), xLabel, yLabel, xValues, yValues);
        if (fixedYScale) {
            double[] a = Tools.getMinMax(xValues);
            plot.setLimits(a[0],a[1],fixedMin,fixedMax);
        }
        plot.show();
    }
    
    String getShortTitle(ImagePlus imp) {
        String title = imp.getTitle();
        int index = title.lastIndexOf('.');
        if (index>0 && (title.length()-index)<=5)
            title = title.substring(0, index);
        return title;
    }
    
    /** Returns the profile plot data. */
    public double[] getProfile() {
        return profile;
    }
    
    
    /** Sets the y-axis min and max. Specify (0,0) to autoscale. */
    public static void setMinAndMax(double min, double max) {
        fixedMin = min;
        fixedMax = max;
        IJ.register(ProfilePlot.class);
    }
    
    /** Returns the profile plot y-axis min. Auto-scaling is used if min=max=0. */
    public static double getFixedMin() {
        return fixedMin;
    }
    
    /** Returns the profile plot y-axis max. Auto-scaling is used if min=max=0. */
    public static double getFixedMax() {
        return fixedMax;
    }
    
/* Here is the part of code that measures width.
 In words it takes a vertical line at each x position, uses a very simple method for finding the derivative, identifies the pixels with maximum and minimum intesities and returns the distance in pixels. A more sophisicated approach would be to take the Gaussian derivative of the image and then find the min and max pixel intensity.
 
 */
     
    double[] getColumnAverageProfile(Rectangle rect, ImageProcessor ip) {
        double[] profile = new double[rect.width];
        double[] aLine = new double[rect.height];
        double[] derivativeLine = new double[rect.height];
        double[] minder = new double[rect.height];
        double[] maxder = new double[rect.height];
        double[] maxpos = new double[rect.width];
        double[] minpos = new double[rect.width];
        double maxderivativeLine;
        double minderivativeLine;
        
        ip.setInterpolate(false);

//
        
        for (int i=0; i<rect.width; i++) {
            
            aLine = ip.getLine(i, rect.y, i, rect.height);
            
            for (int j=0; j<rect.height-1; j++)
                derivativeLine[j] = aLine[j+1] - aLine[j];
            
            maxderivativeLine = derivativeLine[0];
            for (int j=0; j<rect.height-1; j++) {
                if (maxderivativeLine < derivativeLine[j]) maxderivativeLine = derivativeLine[j];
            }
            
            for (int j=0; j<rect.height-1; j++) {
                if (maxderivativeLine == derivativeLine[j]) maxderivativeLine = j;
            }
            
            minderivativeLine = derivativeLine[0];
            for (int j=0; j<rect.height-1; j++) {
                if (minderivativeLine > derivativeLine[j]) minderivativeLine = derivativeLine[j];
            }
            
            for (int j=0; j<rect.height-1; j++) {
                if (minderivativeLine == derivativeLine[j]) minderivativeLine = j;
            }
            
            
            maxpos[i] = maxderivativeLine;
            minpos[i] = minderivativeLine;
            
        }
        
        for (int i=0; i<rect.width; i++) {
            profile[i] = maxpos[i] - minpos[i];
        }
        
        
        return profile;
    }	
    
}



