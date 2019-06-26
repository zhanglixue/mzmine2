/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package net.sf.mzmine.util.components;

import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;
import net.sf.mzmine.modules.visualization.fx3d.Fx3DDataset;

public class ColorTableCell<T> extends TableCell<T, Color> {

    private final ColorPicker colorPicker;

    private static final Logger LOG = Logger
            .getLogger(ColorTableCell.class.getName());

    public ColorTableCell(TableColumn<T, Color> column,
            ObservableList<Fx3DDataset> datasets) {
        colorPicker = new ColorPicker();
        colorPicker.editableProperty().bind(column.editableProperty());
        colorPicker.disableProperty().bind(column.editableProperty().not());
        colorPicker.setOnShowing(event -> {
            final TableView<T> tableView = getTableView();
            tableView.getSelectionModel().select(getTableRow().getIndex());
            tableView.edit(tableView.getSelectionModel().getSelectedIndex(),
                    column);
        });
        colorPicker.valueProperty()
                .addListener((observable, oldValue, newValue) -> {
                    commitEdit(newValue);
                    datasets.get(getTableRow().getIndex()).colorProperty()
                            .set(newValue);
                });
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    @Override
    protected void updateItem(Color item, boolean empty) {

        super.updateItem(item, empty);

        setText(null);
        if (empty) {
            setGraphic(null);
        } else {
            colorPicker.setValue(item);
            setGraphic(colorPicker);
        }
    }
}
