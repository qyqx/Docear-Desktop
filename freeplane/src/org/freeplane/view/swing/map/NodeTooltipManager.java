package org.freeplane.view.swing.map;

/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.resources.ResourceController;

class NodeTooltipManager{
	private static final String TOOL_TIP_MANAGER = "toolTipManager.";
	private static final String TOOL_TIP_MANAGER_INITIAL_DELAY = "toolTipManager.initialDelay";
	private static NodeTooltipManager instance;
	private Timer enterTimer;
	private Timer exitTimer;
	private String toolTipText;
	private Point preferredLocation;
	private JComponent insideComponent;
	private MouseEvent mouseEvent;
	
	private Popup tipPopup;
	/** The Window tip is being displayed in. This will be non-null if
	 * the Window tip is in differs from that of insideComponent's Window.
	 */
	private JToolTip tip;
	final private ComponentMouseListener componentMouseListener;

	static NodeTooltipManager getSharedInstance(){
		if(instance == null){
			instance = new NodeTooltipManager();
			final int maxWidth = ResourceController.getResourceController().getIntProperty(
			    "toolTipManager.max_tooltip_width", Integer.MAX_VALUE);
			NodeTooltip.setMaximumWidth(maxWidth);
			setTooltipDelays();
			ResourceController.getResourceController().addPropertyChangeListener(new IFreeplanePropertyListener() {
				public void propertyChanged(final String propertyName, final String newValue, final String oldValue) {
					if (propertyName.startsWith(TOOL_TIP_MANAGER)) {
						setTooltipDelays();
					}
				}
			});
		}
		return instance;
	}
	private static void setTooltipDelays() {
		final int initialDelay = ResourceController.getResourceController().getIntProperty(
		    TOOL_TIP_MANAGER_INITIAL_DELAY, 0);
		instance.setInitialDelay(initialDelay);
    }
	private NodeTooltipManager() {
		enterTimer = new Timer(750, new insideTimerAction());
		enterTimer.setRepeats(false);
		exitTimer = new Timer(750, new exitTimerAction());
		exitTimer.setRepeats(false);
		componentMouseListener = new ComponentMouseListener();
	}

	/**
	* Specifies the initial delay value.
	*
	* @param milliseconds  the number of milliseconds to delay
	*        (after the cursor has paused) before displaying the
	*        tooltip
	* @see #getInitialDelay
	*/
	public void setInitialDelay(int milliseconds) {
		enterTimer.setInitialDelay(milliseconds);
	}

	/**
	 * Returns the initial delay value.
	 *
	 * @return an integer representing the initial delay value,
	 *		in milliseconds
	 * @see #setInitialDelay
	 */
	public int getInitialDelay() {
		return enterTimer.getInitialDelay();
	}


	private void showTipWindow() {
		if (insideComponent == null || !insideComponent.isShowing())
			return;
		Point screenLocation = insideComponent.getLocationOnScreen();
		tip = insideComponent.createToolTip();
		tip.setTipText(toolTipText);
		int x = screenLocation.x + preferredLocation.x;
		int y = screenLocation.y + preferredLocation.y;
		PopupFactory popupFactory = PopupFactory.getSharedInstance();
		tipPopup = popupFactory.getPopup(insideComponent, tip, x, y);
		tipPopup.show();
	}

	private void hideTipWindow() {
		insideComponent = null;
		toolTipText = null;
		preferredLocation = null;
		mouseEvent = null;
		if (tipPopup != null) {
			tipPopup.hide();
			tipPopup = null;
			tip = null;
			enterTimer.stop();
			exitTimer.stop();
		}
	}

	/**
	 * Registers a component for tooltip management.
	 * <p>
	 * This will register key bindings to show and hide the tooltip text
	 * only if <code>component</code> has focus bindings. This is done
	 * so that components that are not normally focus traversable, such
	 * as <code>JLabel</code>, are not made focus traversable as a result
	 * of invoking this method.
	 *
	 * @param component  a <code>JComponent</code> object to add
	 * @see JComponent#isFocusTraversable
	 */
	public void registerComponent(JComponent component) {
		component.removeMouseListener(componentMouseListener);
		component.removeMouseMotionListener(componentMouseListener);
		component.addMouseListener(componentMouseListener);
		component.addMouseMotionListener(componentMouseListener);
	}

	/**
	 * Removes a component from tooltip control.
	 *
	 * @param component  a <code>JComponent</code> object to remove
	 */
	public void unregisterComponent(JComponent component) {
		component.removeMouseListener(componentMouseListener);
	}


	private class ComponentMouseListener extends MouseAdapter {

		public void mouseEntered(MouseEvent event) {
			initiateToolTip(event);
		}
		public void mouseMoved(MouseEvent event) {
			initiateToolTip(event);
		}
		public void mouseExited(MouseEvent event) {
				exitTimer.start();
		}
	}
	
	private void initiateToolTip(MouseEvent event) {
	JComponent component = (JComponent) event.getSource();
	if(insideComponent == component){
		return;
	}
	hideTipWindow();
	insideComponent = component;
	mouseEvent = event;
	enterTimer.restart();
}

	public boolean isOutside(JComponent component, MouseEvent event) {
		if(component == null){
			return true;
		}
		final Point point = event.getLocationOnScreen();
		SwingUtilities.convertPointFromScreen(point, component);
	    return !(point.x >= 0 && point.y >= 0 && point.x < component.getWidth() && point.y < component.getHeight());
    }


	private class insideTimerAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (insideComponent != null && insideComponent.isShowing()) {
				// Lazy lookup
				if (toolTipText == null && mouseEvent != null) {
					toolTipText = insideComponent.getToolTipText(mouseEvent);
					preferredLocation = insideComponent.getToolTipLocation(mouseEvent);
				}
				if (toolTipText != null) {
					showTipWindow();
				}
				else {
					hideTipWindow();
				}
			}
		}
	}

	private class exitTimerAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if(tip == null || insideComponent == null){
				return;
			}
			if(tip.getMousePosition(true) != null || insideComponent.getMousePosition(true) != null){
				exitTimer.restart();
				return;
			}
			final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			if(focusOwner != null){
				final Window tipWindow = SwingUtilities.getWindowAncestor(tip);
				if(SwingUtilities.isDescendingFrom(focusOwner, tipWindow)){
					exitTimer.restart();
					return;
				}
			}
			hideTipWindow();
		}
	}

}
