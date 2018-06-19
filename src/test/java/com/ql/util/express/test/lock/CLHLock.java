package com.ql.util.express.test.lock;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * 基于链表的可扩展、高性能、公平的自旋锁
 * @author yooka
 *
 */
public class CLHLock {
    public static class CLHNode {
        private volatile boolean isLocked = true;
    }

    @SuppressWarnings("unused")
    private volatile CLHNode                                           tail;
    private static final ThreadLocal<CLHNode>                          LOCAL   = new ThreadLocal<CLHNode>();
    private static final AtomicReferenceFieldUpdater<CLHLock, CLHNode> UPDATER = AtomicReferenceFieldUpdater.newUpdater(CLHLock.class,
                                                                                   CLHNode.class, "tail");

    public void lock() {
        CLHNode node = new CLHNode();
        LOCAL.set(node);
        CLHNode preNode = UPDATER.getAndSet(this, node);//获取上一个节点，并把自己设置为tail节点
        if (preNode != null) {//上一个节点不为空并且lock为true状态则进行自旋
            while (preNode.isLocked) {}
            preNode = null;
            LOCAL.set(node);
        }
    }

    public void unlock() {
        CLHNode node = LOCAL.get();
       // node.isLocked = false;
        if (!UPDATER.compareAndSet(this, node, null)) {//tail还是自己就把tail重置为空，不是自己就把锁状态改为false
            node.isLocked = false;
        }
        node = null;
    }
    
    public static class TestLock {
    	public static void main(String[] args) {
    		final CLHLock lock = new CLHLock();
    		
    		final Thread t1 = new Thread(new Runnable() {
				public void run() {
					lock.lock();
					System.out.println("我是老大，锁被我抢到了，比我小的等2秒！");
					try {
						Thread.sleep(2000);
						lock.unlock();
					} catch (InterruptedException e) {}
				}
			});
    		Thread t2 = new Thread(new Runnable() {
				public void run() {
					lock.lock();
					System.out.println("我是老二，锁被我抢到了，比我小的等1秒！");
					try {
						Thread.sleep(1000);
						lock.unlock();
					} catch (InterruptedException e) {}
				}
			});
    		Thread t3 = new Thread(new Runnable() {
				public void run() {
					lock.lock();
					System.out.println("我是老三，锁被我抢到了，后面的不用等了，我不会给你们机会的，哈哈哈！");
					//lock.unlock();
				}
			});
    		Thread t4 = new Thread(new Runnable() {
				public void run() {
					lock.lock();
					System.out.println("我是老四，锁被我抢到了，比我小的等着！");
					lock.unlock();
				}
			});
    		t1.start();
    		try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    		t2.start();
    		t3.start();
    		t4.start();
		}
    }
}
