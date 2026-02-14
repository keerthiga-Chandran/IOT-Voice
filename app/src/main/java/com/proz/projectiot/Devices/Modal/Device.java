package com.proz.projectiot.Devices.Modal;

public class Device {
    private String name;
    private String ip;
    private boolean isConnected;
    String custom_name,password;
    public Device(String name, String ip, boolean isConnected,String custom_name,String password) {
        this.name = name;
        this.ip = ip;
        this.isConnected = isConnected;
        this.custom_name = custom_name;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getCustom_name() {
        return custom_name;
    }

    public void setCustom_name(String custom_name) {
        this.custom_name = custom_name;
    }

    public String getName() { return name; }
    public String getIp() { return ip; }
    public boolean isConnected() { return isConnected; }

    public void setConnected(boolean connected) { isConnected = connected; }
    public void setName(String name) { this.name = name; }
}
