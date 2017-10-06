package ru.nsu.ccfit.boltava.model.event;

public interface IEventListener<E extends Event> {

    void act(E event);

}
