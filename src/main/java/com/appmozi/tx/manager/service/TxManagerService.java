package com.appmozi.tx.manager.service;

import com.appmozi.tx.mq.model.TxGroup;
import com.appmozi.tx.mq.model.TxInfo;

/**
 * Created by lorne on 2017/6/7.
 */

public interface TxManagerService  {


    /**
     * 创建事物组
     */
    TxGroup createTransactionGroup(String providerUrl, String consumerUrl);


    /**
     * 添加事务组子对象
     * @param consumerUrl
     * @return
     */
    TxGroup addTransactionGroup(String groupId,String taskId, String consumerUrl);



    boolean closeTransactionGroup(String groupId);


    /**
     * 通知事务组事务执行状态
     * @param groupId
     * @param kid
     * @param state
     * @return
     */
    boolean notifyTransactionInfo(String groupId, String kid, boolean state);
}
