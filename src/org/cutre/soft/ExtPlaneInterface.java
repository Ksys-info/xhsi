// Copied from https://github.com/pau662/ExtPlaneInterface/

package org.cutre.soft;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.cutre.soft.epi.command.CommandMessage;
import org.cutre.soft.epi.command.DataRefCommand;
import org.cutre.soft.epi.command.ExtPlaneCommand;
import org.cutre.soft.epi.command.GeneralCommand;
import org.cutre.soft.epi.communication.ExtPlaneTCPReceiver;
import org.cutre.soft.epi.communication.ExtPlaneTCPSender;
import org.cutre.soft.epi.data.DataRef;
import org.cutre.soft.epi.data.DataRefRepository;
import org.cutre.soft.epi.data.DataRefRepositoryImpl;
import org.cutre.soft.epi.data.MessageRepository;
import org.cutre.soft.epi.data.MessageRepositoryImpl;
import org.cutre.soft.epi.util.Constants.DataType;
import org.cutre.soft.epi.util.ObservableAware;
import org.cutre.soft.epi.util.Observer;
import org.cutre.soft.exception.ConnectionException;

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
public class ExtPlaneInterface {

    private final static int poolSize = 2;
    private final static Logger LOGGER = Logger.getLogger(ExtPlaneInterface.class);
    private final String server;
    private final int port;

    private Socket socket;
    private DataRefRepository dataRefrepository;
    private MessageRepository messageRepository;
    private ExtPlaneTCPReceiver receive = null;
    private ExtPlaneTCPSender sender = null;
    private boolean wasRunning = true;

    public ExtPlaneInterface(String server, int port) {
        this.server = server;
        this.port   = port;
        initDataRefRepository();
        initMessageRepository();
    }

    public void excludeDataRef(String dataRefName) {
        sendMessage(new DataRefCommand(DataRefCommand.DATAREF_ACTION.UNSUBSCRIBE,dataRefName));
    }

    public DataRef getDataRef(String dataRef) {
        return dataRefrepository.getDataRef(dataRef);
    }

    public DataType getDataRefType(String dataRefName) {
        DataRef dr = dataRefrepository.getDataRef(dataRefName);
        if(dr!=null) {
            return dr.getDataType();
        }
        return null;
    }

    public String[] getDataRefValue(String dataRefName) {
        DataRef dr = dataRefrepository.getDataRef(dataRefName);
        if(dr!=null) {
            return dr.getValue();
        }
        return null;
    }

    public void includeDataRef(String dataRefName) {
        includeDataRef(dataRefName, null);
    }

    public void includeDataRef(String dataRefName, Float accuracy) {
        DataRefCommand drc = new DataRefCommand(DataRefCommand.DATAREF_ACTION.SUBSCRIBE,dataRefName);
        if(accuracy!=null) {
            drc.setAccuracy(accuracy);
        }
        sendMessage(drc);
    }

    public void setDataRefValue(String dataRefName, String... value) {
        sendMessage(new DataRefCommand(DataRefCommand.DATAREF_ACTION.SET, dataRefName, value));
    }

    public void sendMessage(CommandMessage message) {
        messageRepository.sendMessage(message);
    }

    public void setExtPlaneUpdateInterval(String interval) {
        sendMessage(new ExtPlaneCommand(ExtPlaneCommand.EXTPLANE_SETTING.UPDATE_INTERVAL, interval));
    }

    /**
     * sendCommand
     */
    public void sendCommand(String cmd) {
        sendMessage(new GeneralCommand(cmd));
    }

    public boolean restart() {
        if (socket == null) {
             return start();
        } else {
             if (!wasRunning) {
                 wasRunning = true;
                 LOGGER.info("EPI - Started");
             }
             return true;
        }
    }

    public boolean start() {
        try {
            socket = new Socket(server, port);
            startSending();
            startReceiving();
            return true;
        } catch (Exception e) {
            if (wasRunning) {
                LOGGER.error("EPI - Error connecting host " + server, e);
            }
            stop();
        }
        return false;
    }

    public void stop() {
        stopReceiving();
        stopSending();
        try {
            socket.close();
        } catch (Exception ex) {
        }
        socket = null;
        if (wasRunning) {
            wasRunning = false;
            LOGGER.info("EPI - Stopped");

        }
    }

    public void observeDataRef(String dataRefName, Observer<DataRef> observer) {
        ObservableAware.getInstance().addObserver(dataRefName, observer);
    }

    public void unObserveDataRef(String dataRefName, Observer<DataRef> observer) {
        ObservableAware.getInstance().removeObserver(dataRefName, observer);
    }

    private void initDataRefRepository() {
        dataRefrepository = new DataRefRepositoryImpl();
    }

    private void initMessageRepository() {
        messageRepository = new MessageRepositoryImpl();
    }

    private void startReceiving() {
        receive = new ExtPlaneTCPReceiver(this, socket, dataRefrepository, poolSize);
        receive.start();
    }

    private void startSending() {
        sender = new ExtPlaneTCPSender(this, socket, messageRepository);
        sender.start();
    }

    private void stopReceiving() {
        if (receive!=null) {
            receive.setKeep_running(false);
        }
        receive = null;
    }

    private void stopSending() {
        if (sender!=null) {
            sender.setKeep_running(false);
        }
        sender = null;
    }

}
