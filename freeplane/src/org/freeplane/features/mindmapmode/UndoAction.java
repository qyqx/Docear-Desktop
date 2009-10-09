/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Dimitry Polivaev
 *
 *  This file author is Dimitry Polivaev
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.mindmapmode;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.freeplane.core.controller.Controller;
import org.freeplane.core.frame.IMapSelectionListener;
import org.freeplane.core.model.MapModel;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.undo.IUndoHandler;

class UndoAction extends AFreeplaneAction implements IMapSelectionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Action redo;

	public UndoAction(final Controller controller) {
		super("UndoAction", controller);
		getController().getMapViewManager().addMapSelectionListener(this);
		setEnabled(false);
	}

	public void actionPerformed(final ActionEvent e) {
		final MapModel map = getController().getMap();
		final IUndoHandler undoHandler = (IUndoHandler)map.getExtension(IUndoHandler.class);
		undoHandler.getUndoAction().actionPerformed(e);
		setEnabled(undoHandler.canUndo());
		redo.setEnabled(undoHandler.canRedo());
	}

	public void afterMapChange(final MapModel oldMap, final MapModel newMap) {
		if (newMap == null) {
			return;
		}
		final IUndoHandler undoHandler = (IUndoHandler)( newMap.getExtension(IUndoHandler.class));
		if (undoHandler  != null) {
			setEnabled(undoHandler.canUndo());
			redo.setEnabled(undoHandler.canRedo());
		}
	}

	public void afterMapClose(final MapModel oldMap) {
	}

	public void beforeMapChange(final MapModel oldMap, final MapModel newMap) {
	}

	public void setRedo(final Action redo) {
		this.redo = redo;
	}
}
