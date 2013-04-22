/**
 * Created by drogoul, 19 janv. 2012
 * 
 */
package msi.gama.gui.swt.controls;

import msi.gama.gui.swt.SwtGui;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

/**
 * The class Popup.
 * 
 * @author drogoul
 * @since 19 janv. 2012
 * 
 */
public class Popup {

	private static final Shell popup = new Shell(SwtGui.getDisplay(), SWT.ON_TOP);
	private static final Label popupText = new Label(popup, SWT.NONE);
	private static final Listener hide = new Listener() {

		@Override
		public void handleEvent(final Event event) {
			hide();
		}
	};

	static {
		popup.setLayout(new FillLayout());
		popupText.setForeground(SwtGui.getDisplay().getSystemColor(SWT.COLOR_WHITE));
	}

	private final MouseTrackListener mtl = new MouseTrackListener() {

		@Override
		public void mouseEnter(final MouseEvent e) {
			display();
			isVisible = true;
		}

		@Override
		public void mouseExit(final MouseEvent e) {
			hide();
			isVisible = false;
		}

		@Override
		public void mouseHover(final MouseEvent e) {
			display();
			isVisible = true;
		}

	};

	private final IPopupProvider provider;
	private boolean isVisible;

	/*
	 * 
	 */
	public Popup(final IPopupProvider provider, final Control ... controls) {
		this.provider = provider;
		Control parent = provider.getPositionControl();
		parent.getShell().addListener(SWT.Move, hide);
		parent.getShell().addListener(SWT.Resize, hide);
		parent.getShell().addListener(SWT.Close, hide);
		parent.getShell().addListener(SWT.Deactivate, hide);
		parent.getShell().addListener(SWT.Hide, hide);
		for ( Control c : controls ) {
			c.addMouseTrackListener(mtl);
		}
	}

	public void display() {
		String s = provider.getPopupText();
		if ( s == null || s.isEmpty() ) {
			hide();
			return;
		}
		popupText.setBackground(provider.getPopupBackground());

		popupText.setText(s);
		Control c = provider.getPositionControl();
		if ( c == null || c.isDisposed() ) {
			hide();
			return;
		}
		final Point point = c.toDisplay(c.getLocation().x, c.getSize().y);
		// popup.pack();
		popup.setLocation(point.x, point.y);

		// Compute the new window size.
		final Point newWindowSize = popup.computeSize(SWT.DEFAULT, SWT.DEFAULT);

		// Crop new window size to screen.

		final Rectangle screenArea = popup.getDisplay().getClientArea();
		if ( newWindowSize.y > screenArea.height - (point.y - screenArea.y) ) {
			newWindowSize.y = screenArea.height - (point.y - screenArea.y);
		}
		if ( newWindowSize.x > screenArea.width - (point.x - screenArea.x) ) {
			newWindowSize.x = screenArea.width - (point.x - screenArea.x);
		}
		popup.setSize(newWindowSize);
		popup.layout();
		popup.setVisible(true);
	}

	public boolean isVisible() {
		return isVisible;
	}

	public static void hide() {
		if ( !popup.isDisposed() ) {
			popup.setVisible(false);
		}
	}
}
