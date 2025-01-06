package com.exadel.etoolbox.tracker.service;

public interface HookManager {

    RegistrationStatus getStatus();

    void registerServices();

    void unregisterServices();

}
