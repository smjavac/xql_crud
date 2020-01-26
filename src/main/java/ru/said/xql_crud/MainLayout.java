package ru.said.xql_crud;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.Binder;
import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.*;
import org.apache.log4j.Logger;
import ru.idmt.documino.client.DmConfigureException;
import ru.idmt.documino.client.DocuminoClient;
import ru.idmt.documino.client.api.session.IDmSession;
import ru.idmt.documino.client.api.util.DmException;
import ru.idmt.documino.client.commons.operation.GetMapCollection;
import ru.idmt.documino.client.commons.operation.GetString;
import ru.idmt.documino.client.commons.session.DmLoginInfo;
import ru.said.model.Automobiles;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


@Theme("mytheme")
class MyUI extends UI {
    private ListDataProvider<Automobiles> dataProvider;
    private List<Automobiles> autoList;

    private Binder<Automobiles> binder = new Binder<>();

    private Grid<Automobiles> grid = new Grid<>();
    private TextField name1;
    private TextField name2;
    private TextField search;
    private static final Logger logger = Logger.getLogger(MyUI.class);

    private void onNameFilterTextChange(HasValue.ValueChangeEvent<String> event) {
        dataProvider.setFilter(Automobiles::getModel, s -> caseInsensitiveContains(s, event.getValue()));
    }

    private Boolean caseInsensitiveContains(String where, String what) {
        return where.toLowerCase().contains(what.toLowerCase());
    }

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        try {
            IDmSession session = DocuminoClient.get().getDmAdminClient().newSession(new DmLoginInfo(null, null));
            //  Window subWindow = new Window("sub window");
            final VerticalLayout verticalLayout = new VerticalLayout();
            final HorizontalLayout horizontaLayout = new HorizontalLayout();

            //       addWindow(subWindow);
            Button add = new Button("Добавить");
            Button delete = new Button("Удалить");
            Button edit = new Button("Изменить");

            // здесь логика ИЗМЕНИТЬ
            edit.addClickListener(clickEvent -> {
                //   logger.info("button edit");
                MySub subEditRow = new MySub(session);

                subEditRow.editRow();
                UI.getCurrent().addWindow(subEditRow);
            });
            // здесь логика кнопки DELETE
            delete.addClickListener(clickEvent -> {
                // logger.info("button delete");
                MySub subDeleteRow = new MySub(session);
                try {
                    subDeleteRow.deleteRow();
                } catch (DmException e) {
                    e.printStackTrace();
                }
            });

            // здесь кнопака ДОБАВИТЬ
            add.addClickListener(clickEvent -> {
                //   logger.info("button add");
                MySub subAddRow = new MySub(session);
                subAddRow.addAuto();
                UI.getCurrent().addWindow(subAddRow);
            });


            initDataProvider(session);
            initGrid();

            search = new TextField();
            search.setPlaceholder("Filter by model...");
            search.setWidth("157");
            search.addValueChangeListener(this::onNameFilterTextChange);

            //Automobiles auto2 = new Automobiles();
            // dataProvider.addFilter(auto -> auto.getId() > 3);

            horizontaLayout.addComponents(add, delete, edit, search);
            verticalLayout.addComponents(horizontaLayout, grid);
            //  subWindow.setContent(layout);
            //  subWindow.center();

            setContent(verticalLayout);
            //   setContent(hLayout);
        } catch (DmException | IOException | DmConfigureException e) {
            e.printStackTrace();
        }
    }


    private void initGrid() {
        grid.addColumn(Automobiles::getId).setCaption("Id");
        grid.addColumn(Automobiles::getModel).setCaption("Model");
        grid.addColumn(Automobiles::getBody).setCaption("Body");
    }

    private void initDataProvider(IDmSession session) throws DmException {

        String xql = "SELECT * FROM ddt_automobile";
        Collection<Map<String, Object>> autos = new GetMapCollection(xql, new ArrayList<>()).execute(session);

        logger.debug(xql);
        autoList = new ArrayList<>();

        for (Map<String, Object> autoMap : autos) {

            Automobiles automobile = new Automobiles();
            automobile.setId((String) autoMap.get("r_object_id"));
            automobile.setModel((String) autoMap.get("dss_model"));
            automobile.setBody((String) autoMap.get("dss_body"));
            autoList.add(automobile);
        }

        dataProvider = new ListDataProvider<>(autoList) {
            @Override
            public Object getId(Automobiles item) {
                return item.getId();
            }
        };
        grid.setDataProvider(dataProvider);
    }


    class MySub extends Window {
        IDmSession session;

        MySub(IDmSession session) {
            this.session = session;
        }

        public void addAuto() {
            // super("новый автомобиль"); // Set window caption
            center();

            // Disable the close button
            // setClosable(false);
            final VerticalLayout layout2 = new VerticalLayout();
            final TextField modelTipeTxt = new TextField();
            final TextField modelTxt = new TextField();
            modelTipeTxt.setCaption("Марка");
            modelTxt.setCaption("Модель");
            Button button2 = new Button("Сохранить");
            button2.addClickListener(clickEvent -> {
                binder.forField(modelTipeTxt)
                        .withValidator(value -> value.length() > 0, "Поле не должно быть пустым")
                        .bind(Automobiles::getModel, Automobiles::setModel);

                binder.forField(modelTxt)
                        .withValidator(value -> value.length() > 0, "Поле не должно быть пустым")
                        .bind(Automobiles::getBody, Automobiles::setBody);

                String xql = String.format("CREATE ddt_automobile OBJECT SET dss_model = '%s' SET dss_body = '%s'", modelTipeTxt.getValue(), modelTxt.getValue());
                try {
                    new GetString(xql, null).execute(session);

                } catch (DmException ex) {
                    throw new RuntimeException(ex);
                }

                BinderValidationStatus<Automobiles> status = binder.validate();

                if (!status.hasErrors()) {
                    logger.debug(xql);
                    close();
                }

                try {
                    initDataProvider(session);

                } catch (DmException ex) {
                    throw new RuntimeException(ex);
                }

            });

            layout2.addComponents(modelTipeTxt, modelTxt, button2);
            setContent(layout2);
        }

        public void editRow() {
            center();
            final VerticalLayout layout3 = new VerticalLayout();
            name1 = new TextField();
            name2 = new TextField();
            name1.setCaption("Model");
            name2.setCaption("Body");
            name1.setValue(grid.getSelectedItems().iterator().next().getModel());
            name2.setValue(grid.getSelectedItems().iterator().next().getBody());

            Button save = new Button("Сохранить");

            save.addClickListener(clickEvent -> {

                String model = name1.getValue();
                String body = name2.getValue();

                String xql = String.format(
                        "UPDATE ddt_automobile OBJECTS SET dss_model = '%s' SET dss_body = '%s' WHERE r_object_id = '%s'",
                        model,
                        body,
                        grid.getSelectionModel().getFirstSelectedItem().get().getId());
                try {
                    new GetString(xql, null).execute(session);

                } catch (DmException ex) {
                    throw new RuntimeException(ex);
                }
                logger.debug(xql);

              //  grid.getDataProvider().refreshAll();

                try {
                    initDataProvider(session);
                } catch (DmException ex) {
                    throw new RuntimeException(ex);
                }

                close();
            });

            layout3.addComponents(name1, name2, save);
            setContent(layout3);
        }

        public void deleteRow() throws DmException {
            center();

            String xql = String.format("DELETE ddt_automobile OBJECTS WHERE r_object_id = '%s'",
                    grid.getSelectionModel().getFirstSelectedItem().get().getId());

            new GetString(xql, null).execute(session);

            initDataProvider(session);
        }

        public MySub() {
            super("новый автомобиль"); // Set window caption
        }
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {

    }
}




