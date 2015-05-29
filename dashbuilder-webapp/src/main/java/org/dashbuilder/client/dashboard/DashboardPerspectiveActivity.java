/**
 * Copyright (C) 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dashbuilder.client.dashboard;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.enterprise.event.Event;

import org.dashbuilder.client.perspective.editor.MultiListWorkbenchPanelPresenterExt;
import org.dashbuilder.client.perspective.editor.PerspectiveEditorSettings;
import org.dashbuilder.client.perspective.editor.events.PerspectiveEditOffEvent;
import org.dashbuilder.client.perspective.editor.events.PerspectiveEditOnEvent;
import org.dashbuilder.client.resources.i18n.AppConstants;
import org.dashbuilder.displayer.DisplayerSettings;
import org.dashbuilder.displayer.client.PerspectiveCoordinator;
import org.dashbuilder.displayer.client.json.DisplayerSettingsJSONMarshaller;
import org.dashbuilder.displayer.client.widgets.DisplayerEditor;
import org.dashbuilder.displayer.client.widgets.DisplayerEditorPopup;
import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.ioc.client.container.IOCBeanDef;
import org.jboss.errai.ioc.client.container.SyncBeanManagerImpl;
import org.uberfire.client.mvp.PerspectiveActivity;
import org.uberfire.client.mvp.PerspectiveManager;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.workbench.panels.impl.MultiListWorkbenchPanelPresenter;
import org.uberfire.ext.widgets.common.client.common.popups.YesNoCancelPopup;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.workbench.model.PerspectiveDefinition;
import org.uberfire.workbench.model.impl.PerspectiveDefinitionImpl;
import org.uberfire.workbench.model.menu.MenuFactory;
import org.uberfire.workbench.model.menu.Menus;
import org.uberfire.workbench.model.toolbar.ToolBar;

/**
 * The dashboard composer perspective.
 */
public class DashboardPerspectiveActivity  implements PerspectiveActivity {

    protected DashboardManager dashboardManager;
    protected PerspectiveManager perspectiveManager;
    protected PlaceManager placeManager;
    protected DisplayerSettingsJSONMarshaller jsonMarshaller;
    protected PerspectiveCoordinator perspectiveCoordinator;
    protected PerspectiveEditorSettings perspectiveEditorSettings;
    protected Event<PerspectiveEditOnEvent> perspectiveEditOnEvent;
    protected Event<PerspectiveEditOffEvent> perspectiveEditOffEvent;

    protected PlaceRequest place;
    protected String id;
    protected boolean persistent;

    public DashboardPerspectiveActivity() {
    }

    public DashboardPerspectiveActivity(String id,
            DashboardManager dashboardManager,
            PerspectiveManager perspectiveManager,
            PlaceManager placeManager,
            PerspectiveCoordinator perspectiveCoordinator,
            DisplayerSettingsJSONMarshaller jsonMarshaller,
            PerspectiveEditorSettings perspectiveEditorSettings,
            Event<PerspectiveEditOnEvent> perspectiveEditOnEvent,
            Event<PerspectiveEditOffEvent> perspectiveEditOffEvent) {

        this.id = id;
        this.persistent = true;
        this.dashboardManager = dashboardManager;
        this.perspectiveManager = perspectiveManager;
        this.placeManager = placeManager;
        this.perspectiveCoordinator = perspectiveCoordinator;
        this.jsonMarshaller = jsonMarshaller;
        this.perspectiveEditorSettings = perspectiveEditorSettings;
        this.perspectiveEditOnEvent = perspectiveEditOnEvent;
        this.perspectiveEditOffEvent = perspectiveEditOffEvent;
    }

    public String getDisplayName() {
        return id.substring(10);
    }

    @Override
    public PlaceRequest getPlace() {
        return place;
    }

    @Override
    public void onStartup(final PlaceRequest place) {
        this.place = place;
    }

    @Override
    public void onOpen() {
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onShutdown() {
    }

    @Override
    public PerspectiveDefinition getDefaultPerspectiveLayout() {
        PerspectiveDefinition perspective = new PerspectiveDefinitionImpl(MultiListWorkbenchPanelPresenterExt.class.getName());
        perspective.setName(id);
        return perspective;
    }

    @Override
    public String getIdentifier() {
        return id;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public boolean isTransient() {
        return !persistent;
    }

    @Override
    public Menus getMenus() {

        String editStatus = "Edit " + (perspectiveEditorSettings.isEditOn() ? "off" : "on");
        return MenuFactory
                .newTopLevelMenu(editStatus)
                .respondsWith(getChangeEditStatusCommand())
                .endMenu()
                .newTopLevelMenu(AppConstants.INSTANCE.dashboard_new_displayer())
                .respondsWith(getNewDisplayerCommand())
                .endMenu()
                .newTopLevelMenu(AppConstants.INSTANCE.dashboard_delete_dashboard())
                .respondsWith(getShowDeletePopupCommand())
                .endMenu().build();
    }

    @Override
    public ToolBar getToolBar() {
        return null;
    }

    @Override
    public String getSignatureId() {
        return id;
    }

    @Override
    public Collection<String> getRoles() {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getTraits() {
        return Collections.emptyList();
    }

    // Internal stuff

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    protected YesNoCancelPopup deleteDashboardPopup;

    private Command getChangeEditStatusCommand() {
        return new Command() {
            public void execute() {
                PerspectiveActivity currentPerspective = perspectiveManager.getCurrentPerspective();
                if (perspectiveEditorSettings.isEditOn()) {
                    perspectiveEditorSettings.setEditOn(false);
                    perspectiveEditOffEvent.fire(new PerspectiveEditOffEvent(currentPerspective));
                } else {
                    perspectiveEditorSettings.setEditOn(true);
                    perspectiveEditOnEvent.fire(new PerspectiveEditOnEvent(currentPerspective));
                }
            }
        };
    }

    private Command getShowDeletePopupCommand() {
        return new Command() {
            public void execute() {
                deleteDashboardPopup = YesNoCancelPopup.newYesNoCancelPopup(
                        AppConstants.INSTANCE.dashboard_delete_popup_title(),
                        AppConstants.INSTANCE.dashboard_delete_popup_content(),
                        getDoDeleteCommand(),
                        getCancelDeleteCommand(),
                        null);
                deleteDashboardPopup.show();
            }
        };
    }

    private Command getCancelDeleteCommand() {
        return new Command() {
            public void execute() {
                deleteDashboardPopup.hide();
            }
        };
    }

    private Command getDoDeleteCommand() {
        return new Command() {
            public void execute() {
                perspectiveManager.removePerspectiveState(id, new Command() {
                    public void execute() {
                        dashboardManager.removeDashboard(id);
                        placeManager.goTo(getDefaultPerspectiveActivity().getIdentifier());
                    }
                });
            }
        };
    }

    private Command getNewDisplayerCommand() {
        return new Command() {
            public void execute() {
                /* Displayer settings == null => Create a brand new displayer */
                perspectiveCoordinator.editOn();
                DisplayerEditorPopup displayerEditor = new DisplayerEditorPopup();
                displayerEditor.init(null, new DisplayerEditor.Listener() {

                    public void onClose(final DisplayerEditor editor) {
                        perspectiveCoordinator.editOff();
                    }

                    public void onSave(final DisplayerEditor editor) {
                        perspectiveCoordinator.editOff();
                        placeManager.goTo(createPlaceRequest(editor.getDisplayerSettings()));
                        perspectiveManager.savePerspectiveState(new Command() {public void execute() {}});
                    }
                });
            }
        };
    }

    private PlaceRequest createPlaceRequest(DisplayerSettings displayerSettings) {
        String json = jsonMarshaller.toJsonString(displayerSettings);
        Map<String,String> params = new HashMap<String,String>();
        params.put("json", json);
        return new DefaultPlaceRequest("DisplayerScreen", params);
    }

    private PerspectiveActivity getDefaultPerspectiveActivity() {
        PerspectiveActivity first = null;
        SyncBeanManagerImpl beanManager = (SyncBeanManagerImpl) IOC.getBeanManager();
        Collection<IOCBeanDef<PerspectiveActivity>> perspectives = beanManager.lookupBeans(PerspectiveActivity.class);
        Iterator<IOCBeanDef<PerspectiveActivity>> perspectivesIterator = perspectives.iterator();
        while (perspectivesIterator.hasNext() ) {

            IOCBeanDef<PerspectiveActivity> perspective = perspectivesIterator.next();
            PerspectiveActivity instance = perspective.getInstance();

            if (instance.isDefault()) {
                return instance;
            }
            if (first == null) {
                first = instance;
            }
        }
        return first;
    }
}
