package com.softwareverde.concurrent.service;

import com.softwareverde.logging.Logger;

public abstract class SleepyService {
    public enum Status {
        ACTIVE, SLEEPING, STOPPED
    }

    public interface StatusMonitor {
        Status getStatus();
    }

    private final Object _monitor = new Object();
    private final Runnable _coreRunnable;
    private final StatusMonitor _statusMonitor;

    private volatile Boolean _shouldRestart = false;
    private Thread _thread = null;

    private void _startThread() {
        _thread = new Thread(_coreRunnable);
        _thread.setName(this.getClass().getSimpleName());
        _thread.start();
    }

    protected abstract void _onStart();
    protected abstract Boolean _run();
    protected abstract void _onSleep();

    protected void _loop() {
        final Thread thread = Thread.currentThread();
        while (! thread.isInterrupted()) {
            if (_shouldRestart) {
                _shouldRestart = false;
                try {
                    _onStart();
                    while (! thread.isInterrupted()) {
                        try {
                            final Boolean shouldContinue = _run();

                            if (! shouldContinue) {
                                break;
                            }
                        }
                        catch (final Exception exception) {
                            Logger.warn(exception);
                            break;
                        }
                    }
                }
                catch (final Exception exception) {
                    Logger.warn(exception);
                }
                _onSleep();
            }

            while ( (! _shouldRestart) && (! thread.isInterrupted()) ) {
                try {
                    synchronized (_monitor) {
                        _monitor.wait();
                    }
                }
                catch (final InterruptedException exception) {
                    thread.interrupt();
                }
            }
        }
    }

    protected SleepyService() {
        _statusMonitor = new StatusMonitor() {
            @Override
            public Status getStatus() {
                final Thread thread = _thread;
                if ( (thread == null) || (thread.isInterrupted()) ) {
                    return Status.STOPPED;
                }

                if (! _shouldRestart) {
                    return Status.SLEEPING;
                }

                return Status.ACTIVE;
            }
        };

        _coreRunnable = new Runnable() {
            @Override
            public void run() {
                final Thread thread = Thread.currentThread();
                while (! thread.isInterrupted()) {
                    try {
                        _loop();
                    }
                    catch (final Exception exception) {
                        Logger.warn("Exception encountered in " + this.getClass().getSimpleName(), exception);

                        if (! thread.isInterrupted()) {
                            try {
                                synchronized (_monitor) {
                                    _monitor.wait(1000);
                                }
                            }
                            catch (final Exception ignored) {
                                thread.interrupt();
                            }
                        }
                    }
                }
            }
        };
    }

    public synchronized void start() {
        _shouldRestart = true;

        _startThread();
    }

    public synchronized void wakeUp() {
        _shouldRestart = true;

        synchronized (_monitor) {
            _monitor.notify();
        }
    }

    public synchronized void stop() {
        if (_thread != null) {
            _shouldRestart = false;

            _thread.interrupt();
            try {
                _thread.join(10000L);
            }
            catch (final InterruptedException ignored) { }

            _thread = null;
        }
    }

    public StatusMonitor getStatusMonitor() {
        return _statusMonitor;
    }
}
