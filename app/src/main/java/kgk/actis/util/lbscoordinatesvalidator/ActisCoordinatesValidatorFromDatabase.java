package kgk.actis.util.lbscoordinatesvalidator;

import java.util.Iterator;
import java.util.List;

import de.greenrobot.event.EventBus;
import kgk.actis.actions.ActionCreator;
import kgk.actis.database.ActisDatabaseDao;
import kgk.actis.dispatcher.Dispatcher;
import kgk.actis.model.Signal;
import kgk.actis.networking.event.ValidatedCoordinatesReceivedEvent;
import kgk.actis.util.AppController;

/**
 * Класс для проверки координат местоположения по базовым станциям, в данной реализации предназначен
 * для работы в режиме демона, периодически проверяющего сигналы в локальной базе данных, в настоящее
 * время класс нуждается в доработке
 */
public class ActisCoordinatesValidatorFromDatabase implements LbsCoordinatesValidator {

    private static final String TAG = ActisCoordinatesValidatorFromDatabase.class.getSimpleName();

    private ActionCreator actionCreator;
    private ActisDatabaseDao actisDatabaseDao;

    private List<Signal> signals;

    ////

    public ActisCoordinatesValidatorFromDatabase(List<Signal> signals) {
        EventBus.getDefault().register(this);
        actionCreator = ActionCreator.getInstance(Dispatcher.getInstance(EventBus.getDefault()));
        actisDatabaseDao = ActisDatabaseDao.getInstance(AppController.getInstance().getApplicationContext());
        this.signals = signals;
    }

    ////

    @Override
    public void validate() {
        excludeEarliestDateFromSignals();

        for (Signal signal : signals) {
            try {
                actionCreator.sendActisCoordinatesValidationRequest(signal.getDate(),
                        signal.getMcc(),
                        signal.getMnc(),
                        signal.getCellId(),
                        signal.getLac());
            } catch (Exception e) {
                e.printStackTrace();
                actisDatabaseDao.updateSignalActisDate(signal.getDate());
            }
        }
    }

    ////

    private void excludeEarliestDateFromSignals() {
        long minDate = signals.get(0).getDate();
        for (Signal signal : signals) {
            if (signal.getDate() < minDate) {
                minDate = signal.getDate();
            }
        }

        Iterator<Signal> iterator = signals.iterator();
        while (iterator.hasNext()) {
            Signal signal = iterator.next();
            if (signal.getDate() == minDate) {
                iterator.remove();
            }
        }
    }

    ////

    public void onEvent(ValidatedCoordinatesReceivedEvent event) {
        actisDatabaseDao.updateSignalCoordinatesByServerDate(event.getServerDate(),
                event.getLatitude(),
                event.getLongitude());
        EventBus.getDefault().unregister(this);
    }
}
