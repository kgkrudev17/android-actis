package kgk.actis.map.event;

import kgk.actis.map.Map;

/**
 * Абстракция события готовности объекта карты для взаимодействия
 */
public class MapReadyForUseEvent {

    private Map map;

    public MapReadyForUseEvent(Map map) {
        this.map = map;
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }
}
