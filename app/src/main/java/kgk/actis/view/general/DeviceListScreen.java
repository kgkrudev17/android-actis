package kgk.actis.view.general;

import kgk.actis.model.Device;

/**
 * Интерфейс взаимодействия с экраном списка усторйств
 */
public interface DeviceListScreen {

    void onListItemClick(String deviceInfo, Device chosenDevice);
}
