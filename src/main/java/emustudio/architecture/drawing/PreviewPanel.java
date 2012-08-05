/*
 * PreviewPanel.java
 *
 * Created on 9.7.2008, 12:42:32
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2008-2012, Peter Jakubčo
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.architecture.drawing;

import emustudio.main.Main;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class PreviewPanel extends JPanel {

    private final static Logger logger = LoggerFactory.getLogger(PreviewPanel.class);
    private Schema schema;
    private int schemaWidth;
    private int schemaHeight;
    private File lastImageFile;
    /**
     * Left factor is a constant used in panel resizing. It is a distance between panel left and the x position of the
     * nearest point in the schema.
     */
    private int leftFactor;
    /**
     * Top factor is a constant used in panel resizing. It is a distance between panel top and the y position of the
     * nearest point in the schema.
     */
    private int topFactor;
    /* double buffering */
    private Image dbImage;   // second buffer
    private Graphics2D dbg;  // graphics for double buffering
    /**
     * Holds true when this PreviewPanel was resized, false otherwise
     */
    private boolean panelResized;

    /**
     * Creates empty PreviewPanel
     */
    public PreviewPanel() {
        this(null);
    }

    /**
     * Creates PreviewPanel instance representing given schema.
     *
     * @param schema Schema of the virtual computer
     */
    public PreviewPanel(Schema schema) {
        this.schema = schema;
        this.setBackground(Color.WHITE);
        leftFactor = topFactor = 0;
        panelResized = false;
        this.setDoubleBuffered(true);
    }

    /**
     * Override previous update method in order to implement double-buffering. As a second buffer is used Image object.
     *
     * @param g the Graphics object. It is retyped to Graphics2D
     */
    @Override
    public void update(Graphics g) {
        // initialize buffer if needed
        if (dbImage == null) {
            dbImage = createImage(this.getSize().width,
                    this.getSize().height);
            dbg = (Graphics2D) dbImage.getGraphics();
        }
        // clear screen in background
        dbg.setColor(getBackground());
        dbg.fillRect(0, 0, this.getSize().width,
                this.getSize().height);

        // draw elements in background
        dbg.setColor(getForeground());
        paint(dbg);

        // draw image on the screen
        g.drawImage(dbImage, 0, 0, this);
    }

    private void resizePanel(Graphics g) {
        if (schema == null) {
            return;
        }
        // hladanie najvzdialenejsich elementov (alebo bodov lebo ciara
        // nemoze byt dalej ako bod)
        int width = 0, height = 0, minLeft = -1, minTop = -1;

        for (Element elem : schema.getAllElements()) {
            elem.measure(g);
        }

        for (Element elem : schema.getAllElements()) {
            int eX = elem.getX() - elem.getWidth() / 2;
            int eY = elem.getY() - elem.getHeight() / 2;
            int eWidth = elem.getWidth();
            int eHeight = elem.getHeight();

            if (minLeft == -1) {
                minLeft = eX;
            } else if (minLeft > eX) {
                minLeft = eX;
            }

            if (minTop == -1) {
                minTop = eY;
            } else if (minTop > eY) {
                minTop = eY;
            }

            if (eX + eWidth > width) {
                width = eX + eWidth;
            }
            if (eY + eHeight > height) {
                height = eY + eHeight;
            }
        }
        for (int i = schema.getConnectionLines().size() - 1; i >= 0; i--) {
            List<Point> ps = schema.getConnectionLines().get(i).getPoints();
            for (int j = ps.size() - 1; j >= 0; j--) {
                Point p = ps.get(j);

                if (minLeft == -1) {
                    minLeft = p.x;
                } else if (minLeft > p.x) {
                    minLeft = p.x;
                }

                if (minTop == -1) {
                    minTop = p.y;
                } else if (minTop > p.y) {
                    minTop = p.y;
                }

                if (p.x > width) {
                    width = p.x;
                }
                if (p.y > height) {
                    height = p.y;
                }
            }
        }
        leftFactor = minLeft - Schema.MIN_LEFT_MARGIN;
        topFactor = minTop - Schema.MIN_TOP_MARGIN;
        if (width != 0 && height != 0) {
            this.setSize(width - leftFactor + Schema.MIN_LEFT_MARGIN,
                    height - topFactor + Schema.MIN_TOP_MARGIN);
            this.revalidate();
        }
        schemaWidth = width;
        schemaHeight = height;
        panelResized = true;
    }

    /**
     * Get schema real width.
     *
     * @return schema width
     */
    public int getSchemaWidth() {
        return schemaWidth;
    }

    /**
     * Get schema real height.
     *
     * @return schema height
     */
    public int getSchemaHeight() {
        return schemaHeight;
    }

    /**
     * Override panel paint method to draw shapes.
     *
     * @param g the Graphics object
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (schema == null) {
            return;
        }
        boolean moved = panelResized;
        if (panelResized == false) {
            resizePanel(g);
        }
        if (moved == false) {
            schema.selectAll();
            schema.moveSelection(-leftFactor, -topFactor);
            schema.deselectAll();
            for (Element elem : schema.getAllElements()) {
                elem.measure(g);
            }
        }
        for (ConnectionLine line : schema.getConnectionLines()) {
            line.draw((Graphics2D) g, true);
        }
        for (Element element : schema.getAllElements()) {
            element.draw(g);
        }
    }

    /**
     * Assign new schema to this PreviewPanel. If it is null, does nothing
     *
     * @param s new abstract schema
     */
    public void setSchema(Schema s) {
        if (s == null) {
            return;
        }
        this.schema = s;
        panelResized = false;
        this.repaint();
    }

    /**
     * Clears the preview panel.
     */
    public void clearScreen() {
        this.schema = null;
        this.repaint();
    }

    public void saveSchemaImage() {
        JFileChooser f = new JFileChooser();

        f.setDialogTitle("Save schema image");
        f.setAcceptAllFileFilterUsed(false);

        ImageIO.scanForPlugins();
        FileFilter defaultFilter = null;
        String suffixes[] = ImageIO.getWriterFileSuffixes();
        String formatNames[] = ImageIO.getWriterFormatNames();

        class ImageFileFilter extends FileFilter {

            private String formatName;
            private String suffix;

            public ImageFileFilter(String formatName, String suffix) {
                this.formatName = formatName;
                this.suffix = suffix;
            }

            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                return f.getName().toUpperCase().endsWith("." + suffix.toUpperCase());
            }

            @Override
            public String getDescription() {
                return formatName + " image";
            }

            public String getFormatName() {
                return formatName;
            }

            public String getSuffix() {
                return suffix;
            }
        }

        for (int i = 0; i < suffixes.length; i++) {
            FileFilter filter = new ImageFileFilter(suffixes[i], formatNames[i]);
            f.addChoosableFileFilter(filter);
            if (defaultFilter == null) {
                defaultFilter = filter;
            }
        }
        if (defaultFilter == null) {
            String msg = "Could not save schema image - no image writers are available.";
            logger.error(msg);
            Main.tryShowErrorMessage(msg);
        }
        f.setFileFilter(defaultFilter);
        f.setApproveButtonText("Save");
        if (lastImageFile != null) {
            f.setCurrentDirectory(lastImageFile.getParentFile());
        } else {
            f.setCurrentDirectory(new File(System.getProperty("user.dir")));
        }
        f.setSelectedFile(null);

        int returnVal = f.showSaveDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File selectedFile = f.getSelectedFile();
        ImageFileFilter selectedFileFilter = (ImageFileFilter) f.getFileFilter();

        String suffix = selectedFileFilter.getSuffix();
        if (selectedFile.getName().toLowerCase().endsWith("." + suffix.toLowerCase())) {
            lastImageFile = selectedFile;
        } else {
            lastImageFile = new File(selectedFile.getAbsolutePath() + "." + suffix.toLowerCase());
        }

        // Save the image
        BufferedImage bi = new BufferedImage(getSchemaWidth(), getSchemaHeight(),
                BufferedImage.TYPE_INT_RGB);
        paint(bi.createGraphics());
        try {
            ImageIO.write(bi, selectedFileFilter.getFormatName(), lastImageFile);
        } catch (IOException e) {
            logger.error("Could not save schema image.", e);
            Main.tryShowErrorMessage("Could not save schema image. See log file for details.");
        }
    }
}
