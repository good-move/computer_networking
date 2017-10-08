package ru.nsu.ccfit.boltava.model.event;

import java.util.HashMap;
import java.util.HashSet;


/**
 * EventDispatcher keeps track of user defined events and can store various
 * event listeners, which will be activated, once an event they've subscribed to
 * is published.
 *
 * @param <E> - class, which extends Event class.
 */
public class EventDispatcher<E extends Event> {

    private HashMap<E, HashSet<IEventListener<E>>> records = new HashMap<>();

    /**
     * Adds a listener to the list of subscribers to the specified event.
     * If the (event, listener) pair has been previously registered, nothing happens.
     *
     * @param event - event to subscribe to
     * @param listener - listener to add
     */
    public synchronized void subscribe(E event, IEventListener<E> listener) {
        HashSet<IEventListener<E>> listeners;
        if (!records.containsKey(event)) {
            listeners = new HashSet<>();
        } else {
            listeners = records.get(event);
        }
        listeners.add(listener);
        records.put(event, listeners);
    }


    /**
     * Removes a listener from subscribers list for a specific event.
     * If the event/listener passed has not been registered yet, nothing happens.
     * If there are no subscribers to `event` left after removal, the dispatcher
     * removes `event` from the list of registered events.
     *
     * @param event - event to unsubscribe from
     * @param listener - listener to remove
     */
    public synchronized void unsubscribe(final E event, final IEventListener<E> listener) {
        HashSet<IEventListener<E>> listeners = records.get(event);

        // if such event hasn't been registered, return
        if (listeners == null) {
            return;
        }

        listeners.remove(listener);

        // unregister event if no listeners left
        if (listeners.isEmpty()) {
            records.remove(event);
        }
    }

    /**
     * Registers supplied event as active to notify all listeners to this event.
     * If there are no listeners to the event, nothing happens.
     *
     * @param event - event to publish
     */
    public void publish(E event) {
        HashSet<IEventListener<E>> listeners = records.get(event);
        if (listeners == null) {
            return;
        }

        // !!! bad for heavy tasks
        for (IEventListener<E> listener : listeners) {
            listener.act(event);
        }

    }

}
