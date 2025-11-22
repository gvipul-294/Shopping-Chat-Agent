package com.example.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Phone {
    private String name;
    private String brand;
    private Integer price;
    private String camera;
    private String battery;
    private List<String> features;
    private String display;
    private String processor;
    private Integer storage;
    private Integer ram;

    public Phone() {}

    public Phone(String name, String brand, Integer price, String camera, String battery, List<String> features) {
        this.name = name;
        this.brand = brand;
        this.price = price;
        this.camera = camera;
        this.battery = battery;
        this.features = features;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public String getCamera() {
        return camera;
    }

    public void setCamera(String camera) {
        this.camera = camera;
    }

    public String getBattery() {
        return battery;
    }

    public void setBattery(String battery) {
        this.battery = battery;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getProcessor() {
        return processor;
    }

    public void setProcessor(String processor) {
        this.processor = processor;
    }

    public Integer getStorage() {
        return storage;
    }

    public void setStorage(Integer storage) {
        this.storage = storage;
    }

    public Integer getRam() {
        return ram;
    }

    public void setRam(Integer ram) {
        this.ram = ram;
    }
}

