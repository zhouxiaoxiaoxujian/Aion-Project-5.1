/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 *  Aion-Lightning is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Aion-Lightning is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details. *
 *  You should have received a copy of the GNU General Public License
 *  along with Aion-Lightning.
 *  If not, see <http://www.gnu.org/licenses/>.
 */


package com.aionemu.loginserver;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.services.CronService;
import com.aionemu.commons.utils.AEInfos;
import com.aionemu.commons.utils.ExitCode;
import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.controller.BannedIpController;
import com.aionemu.loginserver.controller.PremiumController;
import com.aionemu.loginserver.dao.BannedMacDAO;
import com.aionemu.loginserver.network.NetConnector;
import com.aionemu.loginserver.network.ncrypt.KeyGen;
import com.aionemu.loginserver.service.PlayerTransferService;
import com.aionemu.loginserver.taskmanager.TaskFromDBManager;
import com.aionemu.loginserver.utils.DeadLockDetector;
import com.aionemu.loginserver.utils.ThreadPoolManager;
import com.aionemu.loginserver.utils.cron.ThreadPoolManagerRunnableRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author -Nemesiss-
 */
public class LoginServer {

    /**
     * Logger for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(LoginServer.class);

    /**
     * @param args
     */
    public static void main(final String[] args) {
        long start = System.currentTimeMillis();

        CronService.initSingleton(ThreadPoolManagerRunnableRunner.class);

        //write a timestamp that can be used by TruncateToZipFileAppender
        log.info("\f" + new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date(System.currentTimeMillis())) + "\f");
        Config.load();
        DatabaseFactory.init();
        DAOManager.init();

        /**
         * Start deadlock detector that will restart server if deadlock happened
         */
        new DeadLockDetector(60, DeadLockDetector.RESTART).start();
        ThreadPoolManager.getInstance();

        /**
         * Initialize Key Generator
         */
        try {
            KeyGen.init();
        } catch (Exception e) {
            log.error("Failed initializing Key Generator. Reason: " + e.getMessage(), e);
            System.exit(ExitCode.CODE_ERROR);
        }

        GameServerTable.load();
        BannedIpController.start();
        DAOManager.getDAO(BannedMacDAO.class).cleanExpiredBans();

        NetConnector.getInstance().connect();
        PlayerTransferService.getInstance();
        TaskFromDBManager.getInstance();

        Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());

        AEInfos.printAllInfos();

        PremiumController.getController();
        log.info("AL Login Server started in " + (System.currentTimeMillis() - start) / 1000 + " seconds.");
    }
}
