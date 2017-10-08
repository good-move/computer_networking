package ru.nsu.ccfit.boltava.model.event;


import org.junit.jupiter.api.Test;

public class EventDispatcherTest {

    @Test
    public void checkPublishReaction() {
        HelloWorldListener listener = new HelloWorldListener();
        EventDispatcher<HelloWorldEvent> dispatcher =
                new EventDispatcher<>();
        HelloWorldEvent event = new HelloWorldEvent();
        dispatcher.subscribe(event, listener);
        dispatcher.publish(event);
        dispatcher.unsubscribe(event, listener);
    }


    @Test
    void checkMultipleSubscribersReaction() {
        EventDispatcher<HelloWorldEvent> dispatcher =
                new EventDispatcher<>();
        HelloWorldEvent event = new HelloWorldEvent();
        dispatcher.subscribe(event, new ConsoleWriterListener("listener 1"));
        dispatcher.subscribe(event, new ConsoleWriterListener("listener 2"));
        dispatcher.subscribe(event, new ConsoleWriterListener("listener 3"));
        dispatcher.publish(event);
    }


    private class HelloWorldEvent extends Event {

        @Override
        public int hashCode() {
            return "Hello, World!".hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !o.getClass().equals(HelloWorldEvent.class)) return false;
            HelloWorldEvent event = (HelloWorldEvent) o;
            return this.hashCode() == event.hashCode();
        }
    }

    private class HelloWorldListener implements IEventListener<HelloWorldEvent> {

        @Override
        public void act(HelloWorldEvent event) {
            System.out.println("Hello, World!");
        }

    }

    private class ConsoleWriterListener implements IEventListener<HelloWorldEvent> {

        private final String text;

        public ConsoleWriterListener(final String text) {
            this.text = text;
        }

        @Override
        public void act(HelloWorldEvent event) {
            System.out.println(this.text);
        }

    }

}
