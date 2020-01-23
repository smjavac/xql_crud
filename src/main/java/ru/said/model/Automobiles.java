package ru.said.model;

public class Automobiles  {
    private int id;
    private String model;
    private String body;

    public Automobiles() {
    }

    public Automobiles(int id, String model,  String csrbody) {
        this.id = id;
        this.model = model;
        this.body = csrbody;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
