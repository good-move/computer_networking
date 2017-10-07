package ru.nsu.ccfit.boltava.view;


import ru.nsu.ccfit.boltava.model.message.TextMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;

/**
 * ConsoleView is in charge of text I/O via stdin and stdout
 */
public class ConsoleView implements Runnable, IMessageRenderer {

    private static final String OUTPUT_MESSAGE_SCHEME = "%s: %s";
    private HashSet<IMessageListener> listeners = new HashSet<>();

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (!Thread.interrupted()) {
                String message = reader.readLine();
                if (message != null) {
                    notifyListeners(message);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Finishing Console Viewer");
        }
    }

    @Override
    public void render(TextMessage message) {
        System.out.println(
                String.format(OUTPUT_MESSAGE_SCHEME, message.getSenderName(), message.getText())
        );
    }

    public synchronized void addMessageListener(IMessageListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeMessageListener(IMessageListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(String message) {
        listeners.forEach(l -> l.onTextMessageEntered(message));
    }

}
