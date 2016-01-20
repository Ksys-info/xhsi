package org.cutre.soft.epi.communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.cutre.soft.epi.data.DataRefRepository;
import org.cutre.soft.ExtPlaneInterface;

/**
 *
 * Copyright (C) 2015  Pau G.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Pau G.
 */
public class ExtPlaneTCPReceiver extends StoppableThread {

    private static final Logger LOGGER = Logger.getLogger(ExtPlaneTCPReceiver.class);
    private Socket socket;
    private ExecutorService pool;
    private DataRefRepository repository;
    private ExtPlaneInterface epi;

    public ExtPlaneTCPReceiver(ExtPlaneInterface epi, Socket socket, DataRefRepository repository, int poolSize) {
        this.epi = epi;
        this.socket = socket;
        this.repository = repository;
        this.keep_running = true;
        //pool = Executors.newFixedThreadPool(poolSize);
        //Thread.currentThread().setPriority(MIN_PRIORITY);
    }

    @Override
    public void run() {
        try {;
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (keep_running) {
                String line = inFromServer.readLine();
                if (line == null) {
                    sleep(1000);
                    break;
                }
                //pool.execute(new InputHandler(repository, line));
                new InputHandler(repository, line).run();
            }
        } catch (Exception e) {
            LOGGER.error("Error getting data from server.",e);
        } finally {
            //pool.shutdown();
            epi.stop();
        }
    }
}
