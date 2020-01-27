/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to &lt;http://unlicense.org/
 */
package net.javagaming.java4k.launcher;

import net.javagaming.java4k.launcher.progress.ProgressController;
import net.javagaming.java4k.launcher.progress.ProgressWorker;

import java.awt.Window;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Central location for managing all parts of the launcher's state.  It
 * is an instance, but there should be exactly one per launcher.
 *
 * @author Groboclown
 */
public class GameManager {
    private final List<GameStateListener> gameStateListeners =
            new ArrayList<GameStateListener>();
    private AbstractExitSecurityManager securityManager;
    private final Object gameSync = new Object();
    private final ThreadGroup gameThreadGroup = new ThreadGroup("All Games");
    private final LauncherManager launcherManager;
    private GameLifeCycleRunner activeGameRunner;
    private GameConfiguration activeGameConfig;
    private GameDetail activeGameDetail;
    private boolean gameStartPending = false;
    private volatile boolean launcherRunning = true;

    public GameManager(LauncherManager launcherManager) {
        this.launcherManager = launcherManager;
    }


    public void setupSecurity() {
        this.securityManager = Security.setSecurityManager(launcherManager);
        Thread t = new Thread(new GameWatchdog(), "GameWatchdog");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 2);
        t.start();
    }


    public GameDetail getActiveGame() {
        return activeGameDetail;
    }


    /**
     * Only used by the security manager.  This cannot be synchronized, or a
     * thread deadlock can happen.
     *
     * @param loader class loader for the game.  Must only be referenced,
     *               not stored.
     * @return the configuration if it has this class loader.
     */
    GameConfiguration getActiveGameFor(ClassLoader loader) {
        if (activeGameConfig != null &&
                activeGameConfig.isClassLoader(loader)) {
            return activeGameConfig;
        }
        return null;
    }


    /**
     * Starts and stops the game.  All stopping and starting <em>must</em>
     * pass through this method.  Even on-close handlers for frames should
     * pass the stop handler through this method.
     *
     * @param newGame the new game, or null for stopping the current game.
     */
    public void setActiveGame(final GameDetail newGame) {
        ProgressWorker<Void> worker = new ProgressWorker<Void>() {
            @Override
            public void doInBackground(Publisher<Void> controller) throws Exception {
                stopStartActiveGame(newGame, controller);
            }
        };
        String name = "Stop the current game";
        if (newGame != null) {
            name = newGame.getName();
        }
        launcherManager.getWorkerManager().startWorker(
                name, ActionSource.GAME_CREATE, false, worker);
    }


    public void addGameStateListener(GameStateListener listener) {
        if (listener != null) {
            synchronized (gameStateListeners) {
                gameStateListeners.add(listener);
            }
        }
    }

    public void removeGameStateListener(GameStateListener listener) {
        synchronized (gameStateListeners) {
            gameStateListeners.remove(listener);
        }
    }


    public void shutdownManager() {
        setActiveGame(null);

        launcherRunning = false;

        // Allow the program to exit
        securityManager.setAllowSystemExit(true);
    }


    /**
     * Runs inside a worker.
     *
     * @param newGame new game, may be null
     */
    private void stopStartActiveGame(GameDetail newGame,
            ProgressController controller) {
        synchronized (gameSync) {
            controller.advance(0,
                    newGame == null
                        ? 1
                        : activeGameRunner == null
                            ? 1 : 4, 0);
            if (activeGameRunner != null) {
                //System.out.println("destroying active game");
                controller.advance("closegame.message",
                        activeGameDetail.getName());
                ProgressController child = controller.createChild(1);
                boolean wasDestroyed;
                try {
                    wasDestroyed = activeGameRunner.destroy(child);
                } catch (Exception e) {
                    launcherManager.getErrorMessageManager().gameError(
                            activeGameDetail.getName(),
                            e, ActionSource.GAME_DESTORY);
                    // FIXME what is the right thing to do?
                    return;
                }
                child.completed();
                if (wasDestroyed) {
                    GameDetail oldDesc = activeGameDetail;
                    activeGameRunner = null;
                    activeGameConfig = null;
                    activeGameDetail = null;
                    fireGameStateStopped(oldDesc);
                } else {
                    launcherManager.getErrorMessageManager().gameError(
                            activeGameDetail.getName(), null,
                            ActionSource.
                                    GAME_DESTORY_STILL_RUNNING);
                }
            }
        }

        // stop the sync to give other tasks that sync on it a chance to run

        controller.advanceTo(1);
        synchronized (gameSync) {
            if (newGame != null) {
                GameConfiguration newConfig;
                try {
                    newConfig = newGame.createGameConfiguration(
                            gameThreadGroup);
                } catch (IOException e) {
                    launcherManager.getErrorMessageManager().gameError(
                            newGame.getName(), e,
                            ActionSource.GAME_CREATE);
                    return;
                } catch (ThreadDeath t) {
                    throw t;
                } catch (VirtualMachineError e) {
                    throw e;
                } catch (Throwable t) {
                    launcherManager.getErrorMessageManager().gameError(
                            "bad error " + newGame.getName(), t,
                            ActionSource.GAME_CREATE);
                    return;
                }
                controller.advanceBy(1);


                GameLifeCycleRunner newRunner =
                        newConfig.createGameLifeCycleRunner(launcherManager);
                controller.advanceBy(1);

                // declare the state such that it's been started, but
                // may not yet be active.
                gameStartPending = true;
                activeGameRunner = newRunner;
                activeGameDetail = newGame;
                activeGameConfig = newConfig;
                try {
                    newRunner.start(null, true);
                    fireGameStateStarted(newGame);
                } catch (InterruptedException e) {
                    launcherManager.getErrorMessageManager().gameError(
                            newGame.getName(), e,
                            ActionSource.GAME_START);
                    gameStartPending = false;
                } catch (IOException e) {
                    launcherManager.getErrorMessageManager().gameError(
                            newGame.getName(), e,
                            ActionSource.GAME_START);
                    gameStartPending = false;
                } catch (ThreadDeath t) {
                    throw t;
                } catch (VirtualMachineError e) {
                    throw e;
                } catch (HardDeathException e) {
                    // ignore
                } catch (Throwable t) {
                    launcherManager.getErrorMessageManager().gameError(
                            "bad error " + newGame.getName(), t,
                            ActionSource.GAME_CREATE);
                    gameStartPending = false;
                    return;
                }
                controller.advanceBy(1);
            }
        }
        controller.completed();
    }


    private void fireGameStateStarted(final GameDetail desc) {
        launcherManager.getWorkerManager().startEDT(
                "fire game started notice", ActionSource.GUI_UPDATE,
                new Runnable() {
                    @Override
                    public void run() {
                        synchronized (gameStateListeners) {
                            for (GameStateListener listener : gameStateListeners) {
                                listener.onGameStarted(desc);
                            }
                        }
                    }
                });
    }


    private void fireGameStateStopped(final GameDetail desc) {
        launcherManager.getWorkerManager().startEDT(
                "fire game stopped notice", ActionSource.GUI_UPDATE,
                new Runnable() {
                    @Override
                    public void run() {
                        synchronized (gameStateListeners) {
                            for (GameStateListener listener : gameStateListeners) {
                                listener.onGameStopped(desc);
                            }
                        }
                    }
                });
    }


    /**
     * Monitors the game's state, looking for signs that it has finished.
     */
    class GameWatchdog implements Runnable {

        @Override
        public void run() {
            GameLifeCycleRunner previousGame = null;
            GameConfiguration previousConfig = null;

            while (launcherRunning && ! Thread.interrupted()) {
                try {
                    synchronized (gameSync) {
                        if (gameStartPending) {
                            // a start request was made, but the game may not
                            // be active.  Do not kill or null out variables
                            // because they are not noted as active.

                            if (activeGameRunner != null) {
                                gameStartPending = false;
                                previousGame = activeGameRunner;
                                previousConfig = activeGameConfig;
                            }
                        } else if (activeGameRunner == null && previousGame != null) {
                            // Force a kill of the game
                            //System.out.println("Killing the GameRunner");

                            ThreadGroup tg = previousConfig.getThreadGroup();

                            if (tg != null) {
                                Thread[] threads = new Thread[2000];
                                int count = tg.enumerate(threads, true);
                                count = Math.min(count, threads.length);
                                for (int i = 0; i < count; ++i) {
                                    if (threads[i].isAlive()) {
                                        threads[i].stop(
                                            new HardDeathException("GameWatchdog"));
                                    }
                                }
                            }

                            for (Window w: launcherManager.getAllUnmanagedWindows()) {
                                w.dispose();
                            }

                            // FIXME force a cleanup of the previous runner


                            previousGame = null;
                            // After cleaning up the references to the object,
                            // class, and classloader, we need to run
                            // the garbage collector twice to force cleanup.
                            System.gc();
                            System.gc();
                        //} else if (activeGameRunner != null) {
                            // Monitor the game, capturing active windows
                        }
                    }

                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    // Someone forced an exit
                    return;
                }
            }
        }

    }
}
