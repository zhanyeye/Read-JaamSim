/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.events;

/**
 * 一个实现事件优先队列的红黑树节点, 同时又维护了一个事件链表的表头和表尾
 * 它是一个红黑树和链表的结合，当被插入事件发生的时间相同时，则插入对应的链表中
 */
class EventNode {

	interface Runner {
		void runOnNode(EventNode node);
	}

	/**
	 * The tick at which this event will execute
	 * 事件发生的时刻
	 */
	long schedTick;

	/**
	 * The schedule priority of this event
	 * 调度事件的优先级
	 */
	int priority;

	/**
	 * 链表表头
	 */
	Event head;

	/**
	 * 链表表尾
	 */
	Event tail;

	/**
	 * 该节点的颜色
	 */
	boolean red;

	/**
	 * 左子节点
	 */
	EventNode left;

	/**
	 * 右子节点
	 */
	EventNode right;

	EventNode(long tick, int prio) {
		schedTick = tick;
		priority = prio;
		left = nilNode;
		right = nilNode;
	}

	/**
	 * 向该节点的事件链表种添加一个元素
	 * @param e 被添加的事件
	 * @param fifo 元素插入的方式
	 */
	final void addEvent(Event e, boolean fifo) {
		if (head == null) {
			// 若链表为空
			head = e;
			tail = e;
			e.next = null;
			return;
		}

		if (fifo) {
			// 尾插法
			tail.next = e;
			tail = tail.next;
			e.next = null;
		} else {
			// 头插发
			e.next = head;
			head = e;
		}
	}

	/**
	 * 从节点链表中移除事件
	 * @param evt 被移除的事件
	 */
	final void removeEvent(Event evt) {
		// quick case where we are the head event
		if (this.head == evt) {
			this.head = evt.next;
			if (evt.next == null) {
				this.tail = null;
			}
		}
		else {
			Event prev = this.head;
			while (prev.next != evt) {
				prev = prev.next;
			}

			prev.next = evt.next;
			if (evt.next == null) {
				this.tail = prev;
			}
		}
	}

	/**
	 * 和其他的EventNode 比较大小
	 * @param other
	 * @return
	 */
	final int compareToNode(EventNode other) {
		return compare(other.schedTick, other.priority);
	}

	/**
	 * 根据事件发生的时间刻度和事件优先级比较大小
	 * @param schedTick 时间发生的时间刻度
	 * @param priority
	 * @return -1 表示小于； 1 表示大于； 0表示等于
	 */
	final int compare(long schedTick, int priority) {
		if (this.schedTick < schedTick) return -1;
		if (this.schedTick > schedTick) return  1;

		if (this.priority < priority) return -1;
		if (this.priority > priority) return  1;

		return 0;
	}

	/**
	 * 红黑树右旋
	 * @param parent
	 */
	final void rotateRight(EventNode parent) {
		if (parent != null) {
			if (parent.left == this) {
				parent.left = left;
			} else {
				parent.right = left;
			}
		}

		EventNode oldMid = left.right;
		left.right = this;

		this.left = oldMid;
	}


	/**
	 * 红黑树左旋
	 * @param parent
	 */
	final void rotateLeft(EventNode parent) {
		if (parent != null) {
			if (parent.left == this) {
				parent.left = right;
			} else {
				parent.right = right;
			}
		}

		EventNode oldMid = right.left;
		right.left = this;

		this.right = oldMid;
	}


	/**
	 * 克隆一个指定节点
	 * @param source 被克隆的节点
	 */
	final void cloneFrom(EventNode source) {
		this.head = source.head;
		this.tail = source.tail;
		this.schedTick = source.schedTick;
		this.priority = source.priority;
		Event next = this.head;
		while (next != null) {
			next.node = this;
			next = next.next;
		}
	}

	/**
	 * 红黑树的空节点
	 */
	static final EventNode nilNode;

	static {
		nilNode = new EventNode(0, 0);
		nilNode.left = null;
		nilNode.right = null;
		nilNode.red = false;
	}
}
