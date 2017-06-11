package com.appmozi.tx.mq.service.impl;

import com.alibaba.dubbo.rpc.RpcContext;
import com.appmozi.tx.manager.service.TxManagerService;
import com.appmozi.tx.mq.model.TxGroup;
import com.appmozi.tx.mq.model.TxInfo;
import com.appmozi.tx.mq.service.MQTxManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by lorne on 2017/6/7.
 */
@Service
public class MQTxManagerServiceImpl implements MQTxManagerService {


    @Autowired
    private TxManagerService txManagerService;


    @Override
    public TxGroup createTransactionGroup(String consumerUrl) {
        String providerUrl =  RpcContext.getContext().getUrl().toString();
        return txManagerService.createTransactionGroup(providerUrl,consumerUrl);
    }

    @Override
    public TxGroup addTransactionGroup(String groupId,String taskId, String consumerUrl) {
        return txManagerService.addTransactionGroup(groupId,taskId,consumerUrl);
    }

    @Override
    public boolean closeTransactionGroup(String groupId) {
        return txManagerService.closeTransactionGroup(groupId);
    }


    @Override
    public boolean notifyTransactionInfo(String groupId, String kid, boolean state) {
        return txManagerService.notifyTransactionInfo(groupId, kid, state);
    }
}
