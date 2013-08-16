/*
 * GAMA - V1.4 http://gama-platform.googlecode.com
 * 
 * (c) 2007-2011 UMI 209 UMMISCO IRD/UPMC & Partners (see below)
 * 
 * Developers :
 * 
 * - Alexis Drogoul, UMI 209 UMMISCO, IRD/UPMC (Kernel, Metamodel, GAML), 2007-2012
 * - Vo Duc An, UMI 209 UMMISCO, IRD/UPMC (SWT, multi-level architecture), 2008-2012
 * - Patrick Taillandier, UMR 6228 IDEES, CNRS/Univ. Rouen (Batch, GeoTools & JTS), 2009-2012
 * - Beno�t Gaudou, UMR 5505 IRIT, CNRS/Univ. Toulouse 1 (Documentation, Tests), 2010-2012
 * - Phan Huy Cuong, DREAM team, Univ. Can Tho (XText-based GAML), 2012
 * - Pierrick Koch, UMI 209 UMMISCO, IRD/UPMC (XText-based GAML), 2010-2011
 * - Romain Lavaud, UMI 209 UMMISCO, IRD/UPMC (RCP environment), 2010
 * - Francois Sempe, UMI 209 UMMISCO, IRD/UPMC (EMF model, Batch), 2007-2009
 * - Edouard Amouroux, UMI 209 UMMISCO, IRD/UPMC (C++ initial porting), 2007-2008
 * - Chu Thanh Quang, UMI 209 UMMISCO, IRD/UPMC (OpenMap integration), 2007-2008
 */
package msi.gama.gui.displays.awt;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.SwingUtilities;
import msi.gama.common.interfaces.*;
import msi.gama.common.util.ImageUtils;
import msi.gama.gui.displays.layers.LayerManager;
import msi.gama.metamodel.shape.*;
import msi.gama.outputs.LayeredDisplayOutput;
import msi.gama.outputs.layers.ILayerStatement;
import msi.gama.precompiler.GamlAnnotations.display;
import msi.gama.runtime.*;
import msi.gaml.compilation.ISymbol;

@display("java2D")
public final class AWTDisplaySurface extends AbstractAWTDisplaySurface {

	private Point snapshotDimension, mousePosition;
	protected BufferedImage buffImage;
	private AWTDisplaySurfaceMenu menuManager;

	private class DisplayMouseListener extends MouseAdapter {

		boolean dragging;

		@Override
		public void mouseDragged(final MouseEvent e) {
			if ( SwingUtilities.isLeftMouseButton(e) ) {
				dragging = true;
				canBeUpdated(false);
				final Point p = e.getPoint();
				if ( mousePosition == null ) {
					mousePosition = new Point(getWidth() / 2, getHeight() / 2);
				}
				setOrigin(origin.x + p.x - mousePosition.x, origin.y + p.y - mousePosition.y);
				mousePosition = p;
				repaint();
			}
		}

		@Override
		public void mouseMoved(final MouseEvent e) {
			// we need the mouse position so that after zooming
			// that position of the image is maintained
			mousePosition = e.getPoint();
		}

		@Override
		public void mouseWheelMoved(final MouseWheelEvent e) {
			final boolean zoomIn = e.getWheelRotation() < 0;
			mousePosition = e.getPoint();
			applyZoom(zoomIn ? 1.0 + zoomIncrement : 1.0 - zoomIncrement, mousePosition);
			updateDisplay();
		}

		@Override
		public void mouseClicked(final MouseEvent evt) {
			if ( evt.getClickCount() == 2 ) {
				zoomFit();
			} else if ( evt.isControlDown() || evt.isMetaDown() || evt.isPopupTrigger() ) {
				selectAgents(evt.getX(), evt.getY());
			}
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			if ( dragging ) {
				updateDisplay();
				dragging = false;
			}
			canBeUpdated(true);
		}

	}

	public AWTDisplaySurface(final Object ... args) {
		displayBlock = new Runnable() {

			@Override
			public void run() {
				canBeUpdated(false);
				drawDisplaysWithoutRepainting();
				repaint();
				canBeUpdated(true);
			}
		};
	}

	@Override
	protected void createIGraphics() {
		iGraphics = new AWTDisplayGraphics(this, (Graphics2D) buffImage.getGraphics());
	}

	@Override
	protected void createNewImage(final int width, final int height) {
		super.createNewImage(width, height);
		final BufferedImage newImage = ImageUtils.createCompatibleImage(width, height);
		if ( buffImage != null ) {
			newImage.getGraphics().drawImage(buffImage, 0, 0, width, height, null);
			buffImage.flush();
		}
		buffImage = newImage;

	}

	@Override
	public void initialize(final double env_width, final double env_height,
		final LayeredDisplayOutput layerDisplayOutput) {
		super.initialize(env_width, env_height, layerDisplayOutput);
		setCursor(createCursor());
		menuManager = new AWTDisplaySurfaceMenu(this);
		final DisplayMouseListener d = new DisplayMouseListener();
		addMouseListener(d);
		addMouseMotionListener(d);
		addMouseWheelListener(d);
		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(final ComponentEvent e) {
				if ( buffImage == null || zoomFit ) {
					zoomFit();
				} else {
					if ( isFullImageInPanel() ) {
						centerImage();
					} else if ( isImageEdgeInPanel() ) {
						scaleOrigin();
					}
					updateDisplay();
				}

				final double newZoom =
					Math.min(getWidth() / (double) getDisplayWidth(), getHeight() / (double) getDisplayHeight());
				setZoomLevel(1 / newZoom);
				previousPanelSize = getSize();
			}
		});
	}

	@Override
	public void outputChanged(final double env_width, final double env_height, final LayeredDisplayOutput output) {
		setEnvWidth(env_width);
		setEnvHeight(env_height);
		bgColor = output.getBackgroundColor();
		this.setBackground(bgColor);
		widthHeightConstraint = env_height / env_width;
		if ( manager == null ) {
			manager = new LayerManager(this);
			final List<? extends ISymbol> layers = output.getChildren();
			for ( final ISymbol layer : layers ) {
				manager.addLayer(LayerManager.createLayer((ILayerStatement) layer, env_width, env_height, iGraphics));
			}
		} else {
			manager.outputChanged();
		}
		repaint();
	}

	@Override
	public int[] computeBoundsFrom(final int vwidth, final int vheight) {
		// GuiUtils.debug("AWTDisplaySurface.computeBoundsFrom " + vwidth + " " + vheight);
		if ( !manager.stayProportional() ) { return new int[] { vwidth, vheight }; }
		final int[] dim = new int[2];
		if ( widthHeightConstraint < 1 ) {
			dim[1] = Math.min(vheight, (int) Math.round(vwidth * widthHeightConstraint));
			dim[0] = Math.min(vwidth, (int) Math.round(dim[1] / widthHeightConstraint));
		} else {
			dim[0] = Math.min(vwidth, (int) Math.round(vheight / widthHeightConstraint));
			dim[1] = Math.min(vheight, (int) Math.round(dim[0] * widthHeightConstraint));
		}
		return dim;
	}

	public void selectAgents(final int mousex, final int mousey) {
		final int xc = mousex - origin.x;
		final int yc = mousey - origin.y;
		final List<ILayer> displays = manager.getLayersIntersecting(xc, yc);
		menuManager.selectAgents(mousex, mousey, xc, yc, displays);
	}

	@Override
	public void forceUpdateDisplay() {
		final boolean old = synchronous;
		setSynchronized(false);
		canBeUpdated(true);
		updateDisplay();
		setSynchronized(old);
	}

	public void drawDisplaysWithoutRepainting() {
		if ( iGraphics == null ) { return; }
		// ex[0] = null;
		iGraphics.fillBackground(bgColor, 1);
		manager.drawLayersOn(iGraphics);
	}

	@Override
	public void paintComponent(final Graphics g) {
		super.paintComponent(g);
		((Graphics2D) g).drawRenderedImage(buffImage, translation);
		if ( autosave ) {
			snapshot();
		}
		redrawNavigator();
	}

	@Override
	public void dispose() {
		if ( manager != null ) {
			manager.dispose();
		}
		if ( buffImage != null ) {
			buffImage.flush();
		}
		if ( navigator == null || navigator.isDisposed() ) { return; }
		navigator.dispose();
	}

	@Override
	public BufferedImage getImage() {
		return buffImage;
	}

	@Override
	public void zoomIn() {
		mousePosition = new Point(origin.x + getDisplayWidth() / 2, origin.y + getDisplayHeight() / 2);
		applyZoom(1.0 + zoomIncrement, mousePosition);

	}

	@Override
	public void zoomOut() {
		if ( zoomLevel < 0.02 ) { return; }
		mousePosition = new Point(origin.x + getDisplayWidth() / 2, origin.y + getDisplayHeight() / 2);;
		applyZoom(1.0 - zoomIncrement, mousePosition);

	}

	public void applyZoom(final double factor, final Point c) {
		if ( resizeImage(Math.max(1, (int) Math.round(getDisplayWidth() * factor)),
			Math.max(1, (int) Math.round(getDisplayHeight() * factor))) ) {
			zoomFit = false;
			if ( widthHeightConstraint < 1 ) {
				setZoomLevel(getDisplayWidth() / (double) getWidth());
			} else {
				setZoomLevel(getDisplayHeight() / (double) getHeight());
			}
			// setZoomLevel(zoomLevel * factor);
			final int imagePX =
				c.x < origin.x ? 0 : c.x >= getDisplayWidth() + origin.x ? getDisplayWidth() - 1 : c.x - origin.x;
			final int imagePY =
				c.y < origin.y ? 0 : c.y >= getDisplayHeight() + origin.y ? getDisplayHeight() - 1 : c.y - origin.y;
			setOrigin(c.x - (int) Math.round(imagePX * factor), c.y - (int) Math.round(imagePY * factor));
			updateDisplay();
		}
	}

	@Override
	public void zoomFit() {
		mousePosition = new Point(getWidth() / 2, getHeight() / 2);
		if ( resizeImage(getWidth(), getHeight()) ) {
			super.zoomFit();
			centerImage();
			updateDisplay();
		}
	}

	@Override
	public void focusOn(final IShape geometry, final ILayer display) {
		// FIXME TO BE ENTIRELY REDEFINED
		// Envelope env = geometry.getEnvelope();
		// double minX = env.getMinX();
		// double minY = env.getMinY();
		// double maxX = env.getMaxX();
		// double maxY = env.getMaxY();
		//
		// int leftX = display.getPosition().x + (int) (display.getXScale() * minX + 0.5);
		// int leftY = display.getPosition().y + (int) (display.getYScale() * minY + 0.5);
		// int rightX = display.getPosition().x + (int) (display.getXScale() * maxX + 0.5);
		// int rightY = display.getPosition().y + (int) (display.getYScale() * maxY + 0.5);
		// Rectangle envelop = new Rectangle(leftX + origin.x, leftY + origin.y, rightX - leftX, rightY - leftY);
		// double xScale = (double) getWidth() / (rightX - leftX);
		// double yScale = (double) getHeight() / (rightY - leftY);
		// double zoomFactor = Math.min(xScale, yScale);
		// if ( bWidth * zoomFactor > MAX_SIZE ) {
		// zoomFactor = (double) MAX_SIZE / bWidth;
		// }
		// setZoom(zoomFactor, new Point((int) envelop.getCenterX(), (int) envelop.getCenterY()));
	}

	/**
	 * @see msi.gama.common.interfaces.IDisplaySurface#setAutoSave(boolean)
	 */
	@Override
	public void setAutoSave(final boolean autosave, final int x, final int y) {
		super.setAutoSave(autosave, x, y);
		snapshotDimension = new Point(x, y);
	}

	/**
	 * @see msi.gama.common.interfaces.IDisplaySurface#snapshot()
	 */
	@Override
	public void snapshot() {
		final IScope scope = GAMA.obtainNewScope();
		try {
			if ( snapshotDimension.x == -1 && snapshotDimension.y == -1 ) {
				save(scope, buffImage);
				return;
			}
			final BufferedImage newImage = ImageUtils.createCompatibleImage(snapshotDimension.x, snapshotDimension.y);
			final IGraphics tempGraphics = new AWTDisplayGraphics(this, (Graphics2D) newImage.getGraphics());
			tempGraphics.fillBackground(bgColor, 1);
			manager.drawLayersOn(tempGraphics);
			save(scope, newImage);
			newImage.flush();
		} finally {
			GAMA.releaseScope(scope);
		}

	}

	/**
	 * These methods do nothing yet for JAVA2D display
	 */
	@Override
	public void addShapeFile() {}

	@Override
	public void initOutput3D(final boolean output3d, final ILocation output3dNbCycles) {}

}
