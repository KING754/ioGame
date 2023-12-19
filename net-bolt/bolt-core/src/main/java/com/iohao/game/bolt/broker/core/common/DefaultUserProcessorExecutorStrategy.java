/*
 * ioGame
 * Copyright (C) 2021 - 2023  渔民小镇 （262610965@qq.com、luoyizhu@gmail.com） . All Rights Reserved.
 * # iohao.com . 渔民小镇
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.iohao.game.bolt.broker.core.common;

import com.iohao.game.bolt.broker.core.aware.UserProcessorExecutorAware;
import com.iohao.game.common.kit.concurrent.DaemonThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 默认策略
 *
 * @author 渔民小镇
 * @date 2022-11-11
 */
@Slf4j
final class DefaultUserProcessorExecutorStrategy implements UserProcessorExecutorStrategy {
    final Executor commonExecutor;
    final Executor requestMessageExecutor;

    DefaultUserProcessorExecutorStrategy() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maximumPoolSize = corePoolSize << 1;
        this.commonExecutor = createExecutor("common", corePoolSize, maximumPoolSize);

        this.requestMessageExecutor = createExecutor("RequestMessage", corePoolSize, corePoolSize);
    }

    @Override
    public Executor getExecutor(UserProcessorExecutorAware userProcessorExecutorAware) {
        String userProcessorName = userProcessorExecutorAware.getClass().getSimpleName();

        if ("RequestMessageClientProcessor".equals(userProcessorName)) {
            // 单独一个池
            return this.requestMessageExecutor;
        }

        return this.commonExecutor;
    }

    Executor createExecutor(String userProcessorName, int corePoolSize, int maximumPoolSize) {

        /*
         * 目前 bolt 默认的 io 线程池的配置是
         * corePoolSize 20
         * maximumPoolSize 400
         * keepAliveTime 60
         * unit TimeUnit.SECONDS
         * workQueue ArrayBlockingQueue
         * NamedThreadFactory daemon=true
         *
         * 下面对于 UserProcessor 提供了一些默认的 Executor 配置，
         * 开发者可以根据自身业务需要来定制 UserProcessorExecutorStrategy。
         */

        String namePrefix = String.format("Processor-Executor-%s-%d"
                , userProcessorName
                , maximumPoolSize);

        DaemonThreadFactory threadFactory = new DaemonThreadFactory(namePrefix);

        var executor = new ThreadPoolExecutor(
                corePoolSize, maximumPoolSize,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory);

        // Processor-Executor
        log.debug("{} 【corePoolSize:{}】【maximumPoolSize:{}】 ",
                namePrefix,
                corePoolSize,
                maximumPoolSize
        );

        // 小预热
        for (int i = 0; i < corePoolSize; i++) {
            executor.execute(() -> {
            });
        }

        return executor;
    }
}
