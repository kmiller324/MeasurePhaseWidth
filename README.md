# MeasurePhaseWidth
kmiller324 released this on Mar 22

An ImageJ plugin for measuring the width of objects in the y dimension aquired by phase microscopy.

To install, download the plugin and put the java file into the ImageJ plugins folder. 
Then use the Plugins > Compile and Run function to generate the class files needed to run the plugin. 
Quit and restart ImageJ to make the plugin appear in the dropdown menu.

To test the plugin, download the zipped example image files which contain a raw image of an axon and a stretched image.

Select all of the image and run the MeasurePhaseWidth plugin. This will generate a window that shows a graph of width 
in units of pixels. On the right hand side the data is noisy because there is no phase image.

To convert from a raw image to a stretched image.
1. Convert the image from 16-bits to 32-bits.
2. Stretch by a factor of 8 on the y-axis using the TransformJ > TJ scale > Interpolation scheme – quintic B-spline.
3. Remove high frequency noise by running a 2 pixel Gaussian blur using the Process > Filters > Gaussian blur function in ImageJ.

Notes:
1. This only measures width along the y-axis, thus raw images may need to be rotated. 
2. An error message occurs if a subregion of the image (that does not span the top to bottom) is selected. 
   A work around is to crop the image before running the plugin. 
