package vanilla.java.bg;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public enum Background {
    ;
    private static final Thread thread = new Thread(new Runnable() {
        public void run() {
            Background.run();
        }
    }, "background");
    private static final List<BackgroundTask> TASKS = new CopyOnWriteArrayList<BackgroundTask>();
    private static final Logger LOGGER = Logger.getLogger(Background.class.getName());

    static {
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private static void run() {
        int sleep = 1;
        //noinspection InfiniteLoopStatement
        while (true) {
            boolean active = false;
            for (int i = TASKS.size() - 1; i >= 0; i--) {
                BackgroundTask task;
                try {
                    task = TASKS.get(i);
                } catch (IndexOutOfBoundsException ignored) {
                    continue;
                }
                try {
                    active |= task.perform();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Removing failing task " + task, e);
                    TASKS.remove(task);
                }
                if (active && sleep > 1)
                    sleep >>>= 1;
                else if (!active && sleep < 128)
                    sleep <<= 1;
                try {
                    synchronized (thread) {
                        thread.wait(sleep);
                    }
                } catch (InterruptedException e) {
                    LOGGER.log(Level.WARNING, "Background thread interrupted, ignoring");
                }
            }
        }
    }

    public static void wake() {
        synchronized (thread) {
            thread.notifyAll();
        }
    }

    public static void register(BackgroundTask bgTask) {
        TASKS.add(bgTask);
    }

    public static void unregister(BackgroundTask bgTask) {
        TASKS.remove(bgTask);
    }
}
