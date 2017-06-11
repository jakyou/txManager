package com.lorne.tx.manager.service.impl;

import com.alibaba.dubbo.config.spring.ReferenceBean;
import com.lorne.Constants;
import com.lorne.tx.manager.service.TransactionConfirmService;
import com.lorne.tx.mq.model.TxGroup;
import com.lorne.tx.mq.model.TxInfo;
import com.lorne.tx.mq.service.MQTransactionService;
import com.lorne.core.framework.utils.thread.CountDownLatchHelper;
import com.lorne.core.framework.utils.thread.IExecute;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * Created by lorne on 2017/6/9.
 */
@Service
public class TransactionConfirmServiceImpl implements TransactionConfirmService {




    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void confirm(TxGroup txGroup) {
        System.out.println("end:"+txGroup.toJsonString());

        boolean checkState = true;

        //检查事务是否正常
        for(TxInfo info:txGroup.getList()){
            if(info.getState()==0){
                checkState = false;
            }
        }
        //检查网络状态是否正常
        if(checkState){
            checkState = checkRollback(txGroup.getList());
        }

        //通知事务
        transaction(txGroup.getList(),checkState);
    }



    /**
     * 检查事务是否提交
     * @param list
     */
    private boolean checkRollback(List<TxInfo> list){
        boolean isOK = true;
        CountDownLatchHelper<Boolean> countDownLatchHelper = new CountDownLatchHelper<>();
        for(final TxInfo info:list){
            final  String url = info.getUrl();
            if (StringUtils.isNotEmpty(url)) {
                countDownLatchHelper.addExecute(new IExecute<Boolean>() {
                    @Override
                    public Boolean execute() {
                        ReferenceBean<MQTransactionService> referenceBean = new ReferenceBean<MQTransactionService>();
                        try {
                            MQTransactionService transactionService =  getMQTransactionService(referenceBean,url);
                            boolean res =  transactionService.checkRollback(info.getKid());
                            return res;
                        }finally {
                            referenceBean.destroy();
                        }
                    }
                });
            }else{
                isOK = false;
                break;
            }
        }
        if(isOK){
            List<Boolean> resList =  countDownLatchHelper.execute().getData();
            for(Boolean bool:resList){
                if(bool){
                    return false;
                }
            }
            return true;
        }else {
            return false;
        }
    }


    private MQTransactionService getMQTransactionService( ReferenceBean<MQTransactionService> referenceBean,String url){
        referenceBean.setApplicationContext(applicationContext);
        referenceBean.setInterface(MQTransactionService.class);
        referenceBean.setUrl(url);
        try {
            referenceBean.afterPropertiesSet();
        } catch (Exception e) {
            e.printStackTrace();
        }
        MQTransactionService transactionService =  referenceBean.get();
        return transactionService;
    }


    /**
     * 事务提交或回归
     * @param list
     * @param checkSate
     */
    private void transaction(List<TxInfo> list,final boolean checkSate){
        for(final TxInfo info:list){
            final  String url = info.getUrl();
            if (StringUtils.isNotEmpty(url)) {
                Constants.threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        ReferenceBean<MQTransactionService> referenceBean = new ReferenceBean<MQTransactionService>();
                        try {
                            MQTransactionService transactionService =  getMQTransactionService(referenceBean,url);
                            boolean res =  transactionService.notify(info.getKid(),checkSate);
                            System.out.println(url+";"+res);
                        }finally {
                            referenceBean.destroy();
                        }

                    }
                });
            }

        }
    }


}
