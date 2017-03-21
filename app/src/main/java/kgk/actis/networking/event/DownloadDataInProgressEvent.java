package kgk.actis.networking.event;

import kgk.actis.networking.DownloadDataStatus;

/**
 * Событие, характеризующее текущее состояние обработки http-запроса
 */
public class DownloadDataInProgressEvent {

    private DownloadDataStatus status;

    public DownloadDataStatus getStatus() {
        return status;
    }

    public void setStatus(DownloadDataStatus status) {
        this.status = status;
    }
}
