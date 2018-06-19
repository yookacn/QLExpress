package com.ql.util.express.test.lock;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * 公平自旋锁
 * @author yooka
 *
 */
public class MCSLock {
    public static class MCSNode {
        volatile MCSNode next;
        volatile boolean isLocked = true;
    }

    private static final ThreadLocal<MCSNode>                          NODE    = new ThreadLocal<MCSNode>();
    @SuppressWarnings("unused")
    private volatile MCSNode                                           queue;
    private static final AtomicReferenceFieldUpdater<MCSLock, MCSNode> UPDATER = AtomicReferenceFieldUpdater.newUpdater(MCSLock.class,
                                                                                   MCSNode.class, "queue");

    public void lock() {
        MCSNode currentNode = new MCSNode();
        NODE.set(currentNode);
        MCSNode preNode = UPDATER.getAndSet(this, currentNode);//取出上一个节点的queue同时把自己设置进去
        if (preNode != null) {//如果上一个节点不为空则把上一个节点的下一个节点设置为自己，同时在自己节点上进行自旋（与CLH锁的区别在于此处）
            preNode.next = currentNode;
            while (currentNode.isLocked) {}
        }
    }

    public void unlock() {
        MCSNode currentNode = NODE.get();
        if (currentNode.next == null) {
            if (UPDATER.compareAndSet(this, currentNode, null)) {//cas检查，当前没有竞争，同时把queue设置为null
            	return;
            } else {//cas失败，说明有后继节点，只是还没更新前驱节点的next域，等前驱节点看到后继节点后，即可安全更新后继节点的locked域
            	 while (currentNode.next == null) {}
            }
        }
        currentNode.next.isLocked = false;//给下一个节点的锁状态设置为可获取
        currentNode.next = null;//help GC
    }
}
