/*
Copyright 2008-2010 Gephi
Authors : Mathieu Bastian <mathieu.bastian@gephi.org>
Website : http://www.gephi.org

This file is part of Gephi.

DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

Copyright 2011 Gephi Consortium. All rights reserved.

The contents of this file are subject to the terms of either the GNU
General Public License Version 3 only ("GPL") or the Common
Development and Distribution License("CDDL") (collectively, the
"License"). You may not use this file except in compliance with the
License. You can obtain a copy of the License at
http://gephi.org/about/legal/license-notice/
or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
specific language governing permissions and limitations under the
License.  When distributing the software, include this License Header
Notice in each file and include the License files at
/cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
License Header, with the fields enclosed by brackets [] replaced by
your own identifying information:
"Portions Copyrighted [year] [name of copyright owner]"

If you wish your version of this file to be governed by only the CDDL
or only the GPL Version 3, indicate your decision by adding
"[Contributor] elects to include this software in this distribution
under the [CDDL or GPL Version 3] license." If you do not indicate a
single choice of license, a recipient has the option to distribute
your version of this file under either the CDDL, the GPL Version 3 or
to extend the choice of license to its licensees as provided above.
However, if you add GPL Version 3 code and therefore, elected the GPL
Version 3 license, then the option applies only if the new code is
made subject to such option by the copyright holder.

Contributor(s):

Portions Copyrighted 2011 Gephi Consortium.
 */

package org.gephi.desktop.banner.workspace;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.project.api.WorkspaceInformation;
import org.gephi.project.api.WorkspaceListener;
import org.gephi.project.api.WorkspaceProvider;
import org.gephi.ui.utils.UIUtils;
import org.netbeans.swing.tabcontrol.DefaultTabDataModel;
import org.netbeans.swing.tabcontrol.TabData;
import org.netbeans.swing.tabcontrol.TabDisplayer;
import org.netbeans.swing.tabcontrol.TabbedContainer;
import org.netbeans.swing.tabcontrol.WinsysInfoForTabbedContainer;
import org.netbeans.swing.tabcontrol.event.TabActionEvent;
import org.openide.awt.Actions;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;

public class WorkspacePanel extends javax.swing.JPanel implements WorkspaceListener, PropertyChangeListener {

    private transient final DefaultTabDataModel tabDataModel;
    private transient final TabDisplayer tabbedContainer;

    /**
     * Creates new form WorkspacePanel
     */
    public WorkspacePanel() {
        // Make the background the same as the parent component
        if (UIUtils.isFlatLafLightLookAndFeel()) {
            UIManager.put("EditorTab.background", Color.WHITE);
        }

        initComponents();

        // Init component
        tabDataModel = new DefaultTabDataModel();

        WinsysInfoForTabbedContainer ws = new WinsysInfoForTabbedContainer() {

            @Override
            public Object getOrientation(Component cmpnt) {
                return TabDisplayer.ORIENTATION_CENTER;
            }

            @Override
            public boolean inMaximizedMode(Component cmpnt) {
                return false;
            }

            @Override
            public boolean isTopComponentMaximizationEnabled() {
                return false;
            }
        };

        tabbedContainer = new TabDisplayer(tabDataModel, TabbedContainer.TYPE_EDITOR, ws);

        // Only needed because of the popup switcher (which doesn't go through the action system)
        tabbedContainer.getSelectionModel().addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (tabbedContainer.getSelectionModel().getSelectedIndex() != -1) {
                    TabData tabData = tabDataModel.getTab(tabbedContainer.getSelectionModel().getSelectedIndex());
                    Workspace workspace = (Workspace) tabData.getUserObject();
                    ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
                    if (pc.getCurrentWorkspace() != null && pc.getCurrentWorkspace() != workspace) {
                        pc.openWorkspace(workspace);
                    }
                }
            }
        });

        tabbedContainer.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                TabActionEvent tabActionEvent = (TabActionEvent) e;
                if (tabActionEvent.getActionCommand().equals(TabbedContainer.COMMAND_CLOSE)) {
                    TabData tabData = tabDataModel.getTab(tabActionEvent.getTabIndex());
                    Actions.forID("Workspace", "org.gephi.desktop.project.actions.DeleteWorkspace").actionPerformed(
                        new ActionEvent(tabData.getUserObject(), 0, null));

                    tabActionEvent.consume();
                } else if (tabActionEvent.getActionCommand().equals(TabbedContainer.COMMAND_CLOSE_ALL)) {
                    Actions.forID("File", "org.gephi.desktop.project.actions.CloseProject").actionPerformed(null);
                    tabActionEvent.consume();
                } else if (tabActionEvent.getActionCommand().equals(TabbedContainer.COMMAND_CLOSE_ALL_BUT_THIS)) {
                    TabData tabData = tabDataModel.getTab(tabActionEvent.getTabIndex());

                    Actions.forID("Workspace", "org.gephi.desktop.project.actions.DeleteOtherWorkspaces").actionPerformed(
                        new ActionEvent(tabData.getUserObject(), 0, null));

                    tabActionEvent.consume();
                } else if (tabActionEvent.getActionCommand().equals(TabbedContainer.COMMAND_SELECT)) {
                    TabData tabData = tabDataModel.getTab(tabActionEvent.getTabIndex());
                    ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
                    pc.openWorkspace((Workspace) tabData.getUserObject());
                    tabActionEvent.consume();
                }
            }
        });

        // Init listener
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {

            @Override
            public void run() {
                ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
                pc.addWorkspaceListener(WorkspacePanel.this);
                refreshModel();
            }
        });
    }

    private synchronized void refreshModel() {
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        if (pc.getCurrentProject() != null) {
            WorkspaceProvider workspaceProvider = pc.getCurrentProject().getLookup().lookup(WorkspaceProvider.class);
            Workspace[] workspaces = workspaceProvider.getWorkspaces();
            if (workspaces.length > 0) {
                for (Workspace workspace : workspaces) {
                    int index = tabDataModel.size();
                    WorkspaceInformation workspaceInformation =
                        workspace.getLookup().lookup(WorkspaceInformation.class);
                    tabDataModel.addTab(index,
                        new TabData(workspace, null, workspaceInformation.getName(),
                            workspaceInformation.getSource()));
                    if (workspaceProvider.getCurrentWorkspace() == workspace) {
                        tabbedContainer.getSelectionModel().setSelectedIndex(index);
                        workspace.getLookup().lookup(WorkspaceInformation.class).addChangeListener(this);
                    }
                }

                add(tabbedContainer, BorderLayout.CENTER);
                getParent().revalidate();
                return;
            }
        }

        // Clear
        tabbedContainer.getSelectionModel().clearSelection();
        if (tabDataModel.size() > 0) {
            tabDataModel.removeTabs(0, tabDataModel.size() - 1);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    @Override
    public void initialize(final Workspace workspace) {
        final WorkspaceInformation workspaceInformation = workspace.getLookup().lookup(WorkspaceInformation.class);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                tabDataModel.addTab(tabDataModel.size(), new TabData(workspace, null,
                    workspaceInformation.getName(),
                    workspaceInformation.getSource()));
                if (tabDataModel.size() == 1) {
                    tabbedContainer.getSelectionModel().setSelectedIndex(0);

                    add(tabbedContainer, BorderLayout.CENTER);
                    getParent().revalidate();
                }
            }
        });
    }

    @Override
    public void select(final Workspace workspace) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                for (int i = 0; i < tabDataModel.size(); i++) {
                    TabData tabData = tabDataModel.getTab(i);
                    if (tabData.getUserObject() == workspace) {
                        if (tabbedContainer.getSelectionModel().getSelectedIndex() != i) {
                            tabbedContainer.getSelectionModel().setSelectedIndex(i);
                        }
                        tabDataModel.setText(i, workspace.getName());
                        workspace.getLookup().lookup(WorkspaceInformation.class).addChangeListener(WorkspacePanel.this);
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void unselect(Workspace workspace) {
        workspace.getLookup().lookup(WorkspaceInformation.class).removeChangeListener(this);
    }

    @Override
    public void close(final Workspace workspace) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                for (int i = 0; i < tabDataModel.size(); i++) {
                    TabData tabData = tabDataModel.getTab(i);
                    if (tabData.getUserObject() == workspace) {
                        tabDataModel.removeTab(i);
                        break;
                    }
                }
                if (tabDataModel.size() == 0) {
                    tabbedContainer.getSelectionModel().clearSelection();

                    remove(tabbedContainer);
                    getParent().revalidate();
                }
            }
        });
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(WorkspaceInformation.EVENT_RENAME)) {

            final WorkspaceInformation workspaceInformation = (WorkspaceInformation) evt.getSource();
            final int index = tabbedContainer.getSelectionModel().getSelectedIndex();
            if (!tabDataModel.getTab(index).getText().equals(workspaceInformation.getName())) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        tabDataModel.setText(index, workspaceInformation.getName());
                    }
                });
            }
        } else if (evt.getPropertyName().equals(WorkspaceInformation.EVENT_SET_SOURCE)) {

            final WorkspaceInformation workspaceInformation = (WorkspaceInformation) evt.getSource();
            final int index = tabbedContainer.getSelectionModel().getSelectedIndex();
            if (tabDataModel.getTab(index).getTooltip() == null
                || !tabDataModel.getTab(index).getTooltip().equals(workspaceInformation.getSource())) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        tabDataModel.setToolTipTextAt(index, workspaceInformation.getSource());
                    }
                });
            }
        }
    }
}
