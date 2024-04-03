package org.dromara.northstar.strategy.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class FixedSizeQueue<T> {
    private final int maxSize;
    private final ArrayDeque<T> queue = new ArrayDeque<>();

    public FixedSizeQueue(int maxSize) {
        this.maxSize = maxSize;
    }

    public synchronized void add(T item) {
        if (queue.size() >= maxSize) {
            queue.poll(); // 移除队列头部的旧数据
        }
        queue.offer(item); // 将新数据添加到队列尾部
    }

    public synchronized T poll() {
        return queue.poll(); // 移除并返回队列头部的数据
    }

    public synchronized T peek() {
        return queue.peek(); // 返回队列头部的数据但不移除
    }

    public synchronized int size() {
        return queue.size(); // 获取队列的当前大小
    }

    /**
     * 获取队列中的所有元素
     *
     * @return 队列中的所有元素
     */
    public synchronized List<T> getAllInReverseOrder() {
        List<T> list = new ArrayList<>();
        ArrayDeque<T> clone = queue.clone(); // 克隆一个队列用于遍历
        while (!clone.isEmpty()) {
            list.add(0, clone.pollLast()); // 从队列尾部开始逐个取出元素添加到 List
        }
        return list;
    }

}
