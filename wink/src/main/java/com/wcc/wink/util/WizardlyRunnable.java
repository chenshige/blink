package com.wcc.wink.util;

/**
 * Created by wenbiao.xie on 2016/6/22.
 */
public abstract class WizardlyRunnable<E> implements Runnable {

    E paramE;

    public WizardlyRunnable(E e) {
        this.paramE = e;
    }

    @Override
    public void run() {
        doRun(this.paramE);
    }

    protected abstract void doRun(E e);

}
