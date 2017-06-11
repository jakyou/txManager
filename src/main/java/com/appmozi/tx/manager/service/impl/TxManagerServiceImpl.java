package com.appmozi.tx.manager.service.impl;

import com.appmozi.tx.manager.service.TransactionConfirmService;
import com.appmozi.tx.manager.service.TxManagerService;
import com.appmozi.tx.mq.model.TxGroup;
import com.appmozi.tx.mq.model.TxInfo;
import com.le.core.framework.utils.ConfigurationUtils;
import com.le.core.framework.utils.KidUtils;
import com.le.core.framework.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.List;

/**
 * Created by lorne on 2017/6/7.
 */
@Service
public class TxManagerServiceImpl implements TxManagerService {


    private  static  int redis_save_max_time ;

    private static  int transaction_wait_max_time ;

    private final static String key_prefix = "tx_manager_";

    @Autowired
    private TransactionConfirmService transactionConfirmService;


    public TxManagerServiceImpl() {
        try {
            redis_save_max_time =  ConfigurationUtils.getInt("tx.properties","redis_save_max_time");
            transaction_wait_max_time =  ConfigurationUtils.getInt("tx.properties","transaction_wait_max_time");
        }catch (Exception e){
             redis_save_max_time = 30;
              transaction_wait_max_time = 5;
        }

    }

    @Override
    public TxGroup createTransactionGroup(String providerUrl, String consumerUrl) {

        String groupId = KidUtils.getKid();
        TxGroup txGroup = new TxGroup();
        txGroup.setGroupId(groupId);
        txGroup.setWaitTime(transaction_wait_max_time);
        txGroup.setConsumerUrl(consumerUrl);
        txGroup.setProviderUrl(providerUrl);

        Jedis jedis = RedisUtil.getJedis();
        String key = key_prefix+groupId;
        jedis.setex(key, redis_save_max_time, txGroup.toJsonString());
        RedisUtil.returnResource(jedis);

        return txGroup;
    }

    @Override
    public TxGroup addTransactionGroup(String groupId,String taskId, String consumerUrl) {
        Jedis jedis = RedisUtil.getJedis();
        String key = key_prefix+groupId;
        String json = jedis.get(key);
        try {
            TxGroup txGroup =  TxGroup.parser(json);
            TxInfo txInfo = new TxInfo();
            txInfo.setKid(taskId);
            txInfo.setUrl(consumerUrl);
            if(txGroup !=null){
                txGroup.addTransactionInfo(txInfo);
                jedis.setex(key, redis_save_max_time, txGroup.toJsonString());
                return txGroup;
            }
        } catch (Exception e) {
            return null;
        }finally {
            RedisUtil.returnResource(jedis);
        }
        return null;
    }

    @Override
    public boolean closeTransactionGroup(String groupId) {
        Jedis jedis = RedisUtil.getJedis();
        String key = key_prefix+groupId;
        String json = jedis.get(key);
        try {
            TxGroup txGroup =  TxGroup.parser(json);
            txGroup.hasOvered();

            jedis.del(groupId);

            transactionConfirmService.confirm(txGroup);

            return true;
        } catch (Exception e) {
            return false;
        }finally {
            RedisUtil.returnResource(jedis);
        }
    }


    @Override
    public boolean notifyTransactionInfo(String groupId, String kid, boolean state) {
        Jedis jedis = RedisUtil.getJedis();
        String key = key_prefix+groupId;
        String json = jedis.get(key);
        try {
            TxGroup txGroup =  TxGroup.parser(json);

            List<TxInfo> list =  txGroup.getList();

              for(TxInfo info:list){
                  if(info.getKid().equals(kid)){
                      info.setState(state?1:0);
                  }
              }
            jedis.setex(key, redis_save_max_time, txGroup.toJsonString());
          return true;
        } catch (Exception e) {
            return false;
        }finally {
            RedisUtil.returnResource(jedis);
        }
    }
}
