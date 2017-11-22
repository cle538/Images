
//Student information for assignemnt.
//Student 1 Name: Catherine Evans
//Student 1 UTEID: cle538

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.util.Arrays;
import java.util.TreeMap;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

// A program to display the result of various image filters
public class ImagesMain {
    public static void main(String[] args) { 
        try{
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(Exception e){
            System.out.println("Unable to change look and feel");
        }
        ImagesFrame f = new ImagesFrame(); 
        f.start(); 
    } 
} // end of ImagesMain class

class ImagesFrame extends JFrame{
    private ImagesPanel panel;
    private JFileChooser fileChooser;
    
    public ImagesFrame() {
        super("Image Filtering");
        setSize(800, 700);
        setLocation(100, 100);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        panel = new ImagesPanel();
        add(panel);
        createFileChooser();
        add(createButton(), "North");
        
        addMenu();
        addMouseListener(new ColorPicker());
    }
    
    private class ColorPicker extends MouseAdapter {
        private Robot r;
        
        ColorPicker() {
            try {
                r = new Robot();
            }
            catch(AWTException e) {
                System.out.println(e);
            }
        }
        
        public void mouseClicked(MouseEvent e) {
            
            // robot uses absolute coordinates
            // mouse event gives relative coordiantes
            // so need to adjust to get correct location
            Color pixelColor = r.getPixelColor(e.getX() + getX(), 
                    e.getY() + getY());
            panel.setColor(pixelColor);
            panel.repaint();
            // debugging code
             System.out.println(e);
             System.out.println("pixel color: " + pixelColor);
        }
        
    }
    
    // for choosing files
    // when the new file button is pressed the user
    // browsers their system for a new image
    private void createFileChooser(){
        fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Image File to Process");
        
        FileNameExtensionFilter imageFiles 
            = new FileNameExtensionFilter("Image Files", "jpg", "JPG", "GIF", "gif", "PNG", "png", "jpeg", "JPEG");
        fileChooser.setFileFilter(imageFiles);
    }
    
    // when this button is pressed the user will
    // browse their system for a new image
    private JButton createButton(){
        JButton result = new JButton("New Picture");
        result.addActionListener(new ActionListener(){ 
            public void actionPerformed(ActionEvent e){
                int retval = fileChooser.showOpenDialog(ImagesFrame.this);
                if (retval == JFileChooser.APPROVE_OPTION) {
                    panel.readImage(fileChooser.getSelectedFile());
                    panel.repaint();
                }
            }
        });
        return result;
    }
    
    // The menu contains the various filters.
    // The Transforms menu contains filters that are
    // implementations of BufferedImageOp.
    // The Filters menu contains filters that are 
    // implemetations of the custom FilterOps class 
    // which is shown below.
    private void addMenu(){
        JMenuBar mb = new JMenuBar();
        setJMenuBar(mb);
        JMenu menu = new JMenu("Transforms");
        mb.add(menu);
        
        for(String s : ImagesPanel.IMAGE_OPS){
            JMenuItem mi = new JMenuItem(s);
            mi.addActionListener(getActionListener());
            menu.add(mi);
        }
        
        JMenu menu2 = new JMenu("Filters");
        mb.add(menu2);
        for(String s : ImagesPanel.FILTER_OPS){
            JMenuItem mi = new JMenuItem(s);
            mi.addActionListener(getActionListener());
            menu2.add(mi);            
        }
    }
    
    private ActionListener getActionListener(){
        return new ActionListener(){
            public void actionPerformed(ActionEvent event){
                panel.setImageOp(event.getActionCommand());
                panel.repaint();
            }
        };
    }
    
    
    public void start(){
        setVisible(true);
    }
} // end of ImagesFrame class


class ImagesPanel extends JPanel{

    public static final String[] IMAGE_OPS = 
            {"Rescale", "Lookup", "Blur", "Sharpen", "Edge", "Emboss", 
             "Invert", "Threshold", "Mirror", "Gaussian", "Posterize", 
             "Red Only"};

    public static final String[] FILTER_OPS = {"Grayscale", 
        "Grayscale Naive", "Simplify", 
        "Black and White", "Hot Metal",
        "Censor", "Color Pop", "Sepia Tone",
        "Edge Blur"};
    
    // so menus are in sorted order
    static {
        Arrays.sort(IMAGE_OPS);
        Arrays.sort(FILTER_OPS);
        
    }

    private TreeMap<String, BufferedImageOp> imageOps;
    private TreeMap<String, FilterOp> filterOps;
    private BufferedImage originalImage;
    private BufferedImage filteredImage;
    private String currentOp;
    
    

    public ImagesPanel(){
        currentOp = "Sharpen";
        createImageOps();
        createFilterOps();
        
        
    }

    public void setColor(Color pixelColor) {
        if(currentOp.equals("Color Pop")) {
            ColorPop cp = (ColorPop) filterOps.get("Color Pop");
            cp.setColor(pixelColor.getRGB());
            filteredImage = cp.filter(originalImage);
        }
    }

    public void setImageOp(String opName){
        currentOp = opName;
        BufferedImageOp bio = imageOps.get(opName);
        if(bio != null) {
            if(opName.equals("Mirror")) 
                // special case because translation depends on image size!
                filteredImage = mirrorImage();
            else
                filteredImage = bio.filter(originalImage, null);
        }
        else{
            // user picked a filter op
            FilterOp fo = filterOps.get(opName);
            if(fo != null)
                filteredImage = fo.filter(originalImage);
        }
    }

    private BufferedImage mirrorImage() {
        AffineTransform at = new AffineTransform();
        at.translate(originalImage.getWidth(), 0);
        at.scale(-1, 1);
        AffineTransformOp result 
            = new AffineTransformOp(at, 
                    AffineTransformOp.TYPE_BICUBIC);
        return result.filter(originalImage, null);
    }

    public void readImage(File f){
        Toolkit tk = Toolkit.getDefaultToolkit();
        Image img = tk.getImage(f.getPath());
        MediaTracker tracker = new MediaTracker(new Component() {});
        tracker.addImage(img, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException ex) {}
        originalImage = new BufferedImage(img.getWidth(this), img.getHeight(this), BufferedImage.TYPE_INT_RGB);
        originalImage.getGraphics().drawImage(img, 0, 0, this);
        filteredImage = imageOps.get("Sharpen").filter(originalImage, null);
    }

    private void createFilterOps(){
        filterOps = new TreeMap<String, FilterOp>();
        filterOps.put("Grayscale", new Grayscale());
        filterOps.put("Grayscale Naive", new GrayscaleNaive());
        filterOps.put("Simplify", new Simplify());
        filterOps.put("Black and White", new BlackAndWhite());
        filterOps.put("Hot Metal", new HotMetal());
        
        // comment back is as you complete
        filterOps.put("Sepia Tone", new SepiaTone());
        filterOps.put("Censor", new Censor());
        filterOps.put("Color Pop", new ColorPop(65));
        filterOps.put("Edge Blur", new EdgeBlur());
    }

    private void createImageOps(){
        imageOps = new TreeMap<String, BufferedImageOp>();
        imageOps.put("Rescale", getRescaleOp());
        imageOps.put("Lookup", createLookupOp());
        imageOps.put("Blur", getBlur());
        imageOps.put("Sharpen", getSharpen());
        imageOps.put("Edge", getEdge());
        imageOps.put("Emboss", getEmboss());
        imageOps.put("Invert", new RescaleOp(-1, 255, null));
        imageOps.put("Threshold", createThresholdLookup());
        
        // put dummy value in map, becuase we actually crete
        // new BufferedImageOp based on current image 
        imageOps.put("Mirror", getEmboss());
        
        // comment back in as you complete
        imageOps.put("Gaussian", createGaussian());
        imageOps.put("Posterize", createPosterize());
        imageOps.put("Red Only", createRedOnly());
    }
    
    private BufferedImageOp getEmboss() {
        float[] kernelValues = new float[]{-2, -1, 0, -1, 1, 1, 0, 1, 2};
        return new ConvolveOp(new Kernel(3, 3, kernelValues));
    }
    
    private BufferedImageOp getEdge() {
        float[] kernelValues = new float[]{-1, 0, -1, 0, 4, 0, -1, 0, -1};
        return new ConvolveOp(new Kernel(3, 3, kernelValues));
    }
    
    private BufferedImageOp getSharpen() {
        float[] kernelValues = new float[]{0, -1, 0, -1, 5, -1, 0, -1, 0};
        return new ConvolveOp(new Kernel(3, 3, kernelValues));
    }
    
    private BufferedImageOp getBlur() {
        // for convolve ops
        float[] kernelValues = new float[9];
        Arrays.fill(kernelValues, 1 / 9.0f); 
        return new ConvolveOp(new Kernel(3, 3, kernelValues));
    }
    
    private BufferedImageOp createGaussian() {
        // for convolve ops
        float[] kernelValues = new float[49];
        float[] wholes = new float[] {1, 34, 285, 579, 285, 34, 1,
        34, 1174, 9791, 19856, 9791, 1174, 34,
        285, 9791, 81674, 165644, 81674, 9791, 285,
        579, 19856, 165644, 335946, 165644, 19856, 579,
        285, 9791, 81674, 165644, 81674, 9791, 285,
        34, 1174, 9791, 19856, 9791, 1174, 34,
        1, 34, 285, 579, 285, 34, 1};
        for(int i = 0; i < 49; i++)
        {
            Arrays.fill(kernelValues, i, i + 1, wholes[i]/1492538.0f);
        }
        return new ConvolveOp(new Kernel(7, 7, kernelValues));
    }
    
    private BufferedImageOp createRedOnly() {
        float[] scales = {1.0f, 0.0f, 0.0f};
        float[] offsets = {0, 0, 0};     
        return new RescaleOp(scales, offsets, null);
    }
    private BufferedImageOp getRescaleOp() {
        float[] scales = {0.5f, 1.7f, 0.5f};
        float[] offsets = {0, 0, 0};     
        return new RescaleOp(scales, offsets, null);
    }
    
    private LookupOp createThresholdLookup() {
        short[] values = new short[256];
        int THRESHOLD = 70;
        for(int i = THRESHOLD; i < values.length; i++)
            values[i] = 255;
        return new LookupOp(new ShortLookupTable(0, values), null);
    }
    private LookupOp createPosterize() {
        short[] values = new short[256];
        for(int i = 0; i < values.length; i++)
        {
        	if (i < 32)
            	values[i] = 0;
        	else if(i < 96)
        		values[i] = 64;
        	else if(i < 160)
        		values[i] = 128;
        	else if(i < 224)
        		values[i] = 192;
        	else
        		values[i] = 255;
        }	
        return new LookupOp(new ShortLookupTable(0, values), null);
    }
    private LookupOp createLookupOp(){
        final int SIZE = 256;
        final int THRESHOLD = 150;
        short[] redValues = new short[SIZE];
        short[] greenValues = new short[SIZE];
        short[] blueValues = new short[SIZE];
        for(int i = 0; i < SIZE; i++){
            if(i < THRESHOLD){
                greenValues[i] = (short)(Math.sqrt(i));
                blueValues[i] = (short)(Math.sqrt(i));
            }
            else{
                greenValues[i] = (short)(Math.min(Math.pow(i, 1.05), SIZE - 1));
                blueValues[i] = (short)(Math.min(Math.pow(i, 1.05), SIZE - 1));
            }
            redValues[i] = (short)(Math.min(Math.pow(i, 1.05), SIZE - 1));
        }
        short[][] table = {redValues, greenValues, blueValues};
        ShortLookupTable colorTable = new ShortLookupTable(0, table);
        LookupOp lookup = new LookupOp(colorTable, null);
        return lookup;  
    }


    public void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        
        if(originalImage != null) {            
            double scale = getScale();
            int drawWidth = (int)(originalImage.getWidth() * scale);
            int drawHeight = (int)(originalImage.getHeight() * scale);
            // draw original image on left and filtered image on right
            g2.drawImage(originalImage, 0, 0, drawWidth, drawHeight, null);
            g2.drawImage(filteredImage, drawWidth, 0, drawWidth, drawHeight, null);
        }
    }
    
    // method to determine how to scale image. Retain
    // proportion, but shrink or grow image as necessary
    private double getScale() {
        final int MAX_WIDTH = getWidth() / 2;
        final int MAX_HEIGHT = getHeight();
        double xScale = 1.0 * MAX_WIDTH / originalImage.getWidth();
        double yScale = 1.0 * MAX_HEIGHT / originalImage.getHeight();
        
        return Math.min(xScale, yScale);
    }
} // end of ImagesPanel class


abstract class FilterOp {
    
    public abstract int filterOp(int pixel, BufferedImage src);
    
    public BufferedImage filter(BufferedImage src){
        BufferedImage result = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        for(int x = 0; x < src.getWidth(); x++)
            for(int y = 0; y < src.getHeight(); y++)
                result.setRGB(x, y, filterOp(src.getRGB(x, y), src));
        return result;
    }
    
    /**
     * Extracts the red component of a pixel.
     * @param pixel an integer pixel
     * @return  the red component [0-255] of the pixel.
     */
    public static int getRed(int pixel) {
        return pixel >> 16 & 0xff;
    }

    /**
     * Extracts the green component of a pixel.
     * @param pixel an integer pixel
     * @return  the green component [0-255] of the pixel.
     */
    public static int getGreen(int pixel) {
        return pixel >> 8 & 0xff;
    }

    /**
     * Extracts the blue component of a pixel.
     * @param pixel an integer pixel
     * @return  the blue component [0-255] of the pixel.
     */
    public static int getBlue(int pixel) {
        return pixel & 0xff;
    }
    
    /**
     * Extracts all components of a pixel.
     * @param pixel an integer pixel
     * @return  an array with r, g, b values. r is at index 0
     * g is at index 1, and b is at index 2.
     */    
    public static int[] getRGB(int pixel) {
        return new int[]{getRed(pixel), getGreen(pixel), getBlue(pixel)};
    }
    
    /**
     * Constructs a pixel from RGB components.
     * @param red   the red component [0-255]
     * @param green the green component [0-255]
     * @param blue  the blue component [0-255]
     * @return  the packed integer pixel.
     */
    public static int makePixel(int red, int green, int blue) {
        return (red & 0xff) << 16 | (green & 0xff) << 8 | (blue & 0xff);
    }
} // end of FilterOp class


class Simplify extends FilterOp {
    
    public static final int BLACK = makePixel(0, 0, 0);
    public static final int WHITE = makePixel(255, 255, 255);
    public static final int RED = makePixel(255, 0, 0);
    public static final int GREEN = makePixel(0, 255, 0);
    public static final int BLUE = makePixel(0, 0, 255);
    
    public int filterOp(int pixel, BufferedImage src){
        final int LOW_THRESHOLD = 50;
        final int HIGH_THRESHOLD = 200;
        
        int result = BLUE;
        int r = getRed(pixel);
        int g = getGreen(pixel);
        int b = getBlue(pixel);
        if( r < LOW_THRESHOLD && g < LOW_THRESHOLD && b < LOW_THRESHOLD)
            result = BLACK;
        else if (r > HIGH_THRESHOLD && g > HIGH_THRESHOLD && b > HIGH_THRESHOLD)
            result = WHITE;
        else if(r > g && r > b)
            result = RED;
        else if( g > r && g > b)
            result = GREEN;
        return result;
    }
} // end of Simplify class

class GrayscaleNaive extends FilterOp {

    public int filterOp(int pixel, BufferedImage src){
        int r = getRed(pixel);
        int g = getGreen(pixel);
        int b = getBlue(pixel);
        int gray = (int)(0.333 * r + 0.333 * g + 0.333 * b);
        return makePixel(gray, gray, gray);
    }
}

class Grayscale extends FilterOp {

    public int filterOp(int pixel, BufferedImage src){
        int r = getRed(pixel);
        int g = getGreen(pixel);
        int b = getBlue(pixel);
        int gray = (int)(0.3 * r + 0.59 * g + 0.11 * b);
        return makePixel(gray, gray, gray);
    }
} // end of Grayscale class


class BlackAndWhite extends FilterOp {

    public int filterOp(int pixel, BufferedImage src){
        int r = getRed(pixel);
        int g = getGreen(pixel);
        int b = getBlue(pixel);
        int min = 127;
        boolean aboveThreshold = r > min || g > min || b > min;
        return aboveThreshold ? makePixel(255, 255, 255) : makePixel(0, 0, 0);
    }
} // end of BlackAndWhite class


class HotMetal extends FilterOp {
    
    private int[] values;
    
    public HotMetal() {
        values = new int[256];
        final int MAX_RED = 170;
        for(int i = 0; i < values.length; i++) {
            int red = (int) Math.min(1.0 * i / MAX_RED * 255, 255);
            int green = i - MAX_RED;
            green = green <= 0 ? 0 : (short) Math.min(1.0 * green / 85 * 255, 255);
            values[i] = makePixel(red, green, 0);
        }
    }
    
    public int filterOp(int pixel, BufferedImage src){
        int r = getRed(pixel);
        int g = getGreen(pixel);
        int b = getBlue(pixel);
        int gray = (int)(0.3 * r + 0.59 * g + 0.11 * b);
        return values[gray];
    }    
} // end of HotMetal class

class ColorPop extends FilterOp {


    public ColorPop(int d) {
        distance = d;
    }
    
    private int[] rgb = {0,0,0};
    private int distance;
    
    public void setColor(int color) {
        rgb = getRGB(color);
    }
    
    @Override
    public int filterOp(int pixel, BufferedImage src) {
    	int[] rgb2 = getRGB(pixel);
        int gray = (int)(0.3 * rgb2[0] + 0.59 * rgb2[1] + 0.11 * rgb2[2]);
        double dist = Math.sqrt(((rgb[0]-rgb2[0])*(rgb[0]-rgb2[0])) +
        		((rgb[1]-rgb2[1])*(rgb[1]-rgb2[1])) + ((rgb[2]-rgb2[2])*(rgb[2]-rgb2[2])));
        if (dist <= distance)
        		return pixel;
        else
        	return makePixel(gray, gray, gray);
        				
    }
}

class SepiaTone extends FilterOp {  
    public int filterOp(int pixel, BufferedImage src){
        int r = getRed(pixel);
        int g = getGreen(pixel);
        int b = getBlue(pixel);
        int r2 = (int)((r * .393) + (g *.769) + (b * .189));
        int g2 = (int)((r * .349) + (g *.686) + (b * .168));
        int b2 = (int)((r * .272) + (g *.534) + (b * .131));
        if (r2 > 255)
        	r2 = 255;
        if (g2 > 255)
        	g2 = 255;
        if (b2 > 255)
        	b2 = 255;
        return makePixel(r2,g2,b2);
    }

}

class Censor extends FilterOp {
	int superSize = 20;
    public BufferedImage filter(BufferedImage src){
        

        int height = src.getHeight()/superSize*superSize;
        int width = src.getWidth()/superSize*superSize;
        BufferedImage result = new BufferedImage(width, height, src.getType());
        int pixel = 0;
        for (int x = 0; x < width -1; x++)
        {
            for (int y = 0; y < height-1; y++)
	        {
            	if(x < width - superSize && y < height - superSize)
            		pixel = getAverage(x, y, src);
                result.setRGB(x, y, pixel);
	        }
        }
        return result;
     
    }
    
    public int getAverage(int x, int y, BufferedImage src)
    {
        int r[][] = new int[superSize][superSize];
        int g[][] = new int[superSize][superSize];
        int b[][] = new int[superSize][superSize];
        int r2 = 0;
        int g2 = 0;
        int b2 = 0;
        int rgb;
    	for(int i = x; i < (x + superSize-2); i++)
    	{
    		for(int j = y; j < (y + superSize-2); j++)
    		{
    			rgb = src.getRGB(i,j);
	    		r2 += getRed(rgb);
	    		b2 += getBlue(rgb);
	    		g2 += getGreen(rgb);
    		}
    	}

    	return makePixel(r2/(superSize*superSize), g2/(superSize*superSize), b2/(superSize*superSize));
    }
    
    public int filterOp(int pixel, BufferedImage src)
    {
    	
        int r = getRed(pixel);
        int g = getGreen(pixel);
        int b = getBlue(pixel);
        int gray = (int)(0.3 * r + 0.59 * g + 0.11 * b);
        return makePixel(gray, gray, gray);
    }
}

class EdgeBlur extends FilterOp {
    
    private int[] values;
    
    public BufferedImage filter(BufferedImage src){
        BufferedImage result = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        for(int x = 0; x < src.getWidth(); x++)
            for(int y = 0; y < src.getHeight(); y++)
                result.setRGB(x, y, filterOp(src.getRGB(x, y), src));
        return result;
    }
    
    public int filterOp(int pixel, BufferedImage src){
    	int cx = src.getWidth()/2;
    	int cy = src.getHeight()/2;
        int r = getRed(pixel);
        int g = getGreen(pixel);
        int b = getBlue(pixel);
        int gray = (int)(0.3 * r + 0.59 * g + 0.11 * b);
        return values[gray];
    }    
} 


