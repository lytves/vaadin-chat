package com.vaadin.chat.vaadinchat;

import com.github.rjeschke.txtmark.Processor;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

@Route("")
@Push
public class MainView extends VerticalLayout {

    private final Storage storage;
    private Grid<Storage.ChatMessage> grid;
    private Registration registration;
    private VerticalLayout chat;
    private VerticalLayout login;
    private String userName = "";

    public MainView(Storage storage) {
        this.storage = storage;

        buildLogin();
        buildChat();
    }

    private void buildLogin() {
        login = new VerticalLayout() {{
            TextField textfield = new TextField("please, introduce yourself");
            add(
                    textfield,
                    new Button("Login") {{
                        addClickListener(click -> {
                            login.setVisible(false);
                            chat.setVisible(true);
                            userName = textfield.getValue();
                            storage.addRecordNewUserJoined(userName);
                        });
                        addClickShortcut(Key.ENTER);
                    }}
            );
        }};
        add(login);
    }

    private void buildChat() {
        chat = new VerticalLayout();
        add(chat);
        chat.setVisible(false);

        grid = new Grid<>();
        grid.setItems(storage.getMessages());
        grid.addColumn(new ComponentRenderer<>(message -> new Html(renderRow(message))))
                .setAutoWidth(true)
                .setHeader("Let's go to talk!");

        TextField textField = new TextField();

        chat.add(
                new H3("Vaadin chat"),
                grid,
                new HorizontalLayout() {{
                    add(
                            textField,
                            new Button("➜") {{
                                addClickListener(click -> {
                                    storage.addRecord(userName, textField.getValue());
                                    textField.clear();
                                });
                                addClickShortcut(Key.ENTER);
                            }}
                    );
                }}
        );
    }

    public void onMessage(Storage.ChatEvent event) {
        if (getUI().isPresent()) {
            UI ui = getUI().get();
            ui.getSession().lock();
            ui.beforeClientResponse(grid, ctx -> grid.scrollToEnd());
            ui.access(() -> grid.getDataProvider().refreshAll());
            ui.getSession().unlock();
        }
    }

    private String renderRow(Storage.ChatMessage message) {
        if (message.getUsrName().isEmpty()) {
            return Processor.process(String.format("_User **%s** is just joined the chat!_", message.getMessage()));
        } else {
            return Processor.process(String.format("**%s**: %s", message.getUsrName(), message.getMessage()));
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        registration = storage.attachListener(this::onMessage);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        registration.remove();
    }
}
