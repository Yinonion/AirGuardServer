package com.airguard.server.model;

import com.airguard.server.entity.Plane;
import java.util.ArrayList;
import java.util.List;

public class FlightQueue {

    // הגדרת החוליה (Node) ברשימה הדו-כיוונית
    private class Node {
        Plane plane;
        Node next;
        Node prev;

        public Node(Plane plane) {
            this.plane = plane;
        }
    }

    private Node head; // תחילת התור (העדיפות הכי גבוהה - לנחיתה מיידית)
    private Node tail; // סוף התור (העדיפות הכי נמוכה)
    private int size;

    public FlightQueue() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    // --- אלגוריתם ההכנסה החכמה לפי הדרישה שלך ---
    public void insert(Plane newPlane) {
        Node newNode = new Node(newPlane);
        double newScore = newPlane.calculatePriorityScore();

        // מקרה בסיס: אם הרשימה ריקה
        if (head == null) {
            head = newNode;
            tail = newNode;
            size++;
            return;
        }

        // אם זה מצב חירום - סורקים מראש התור (Head) למטה
        if (newPlane.isEmergency()) {
            Node current = head;
            // כל עוד למטוס הנוכחי יש ציון גבוה יותר, נמשיך הלאה
            while (current != null && current.plane.calculatePriorityScore() >= newScore) {
                current = current.next;
            }
            insertNodeBefore(current, newNode);
        }
        // אם זה מטוס רגיל - סורקים מסוף התור (Tail) למעלה
        else {
            Node current = tail;
            // כל עוד למטוס הנוכחי יש ציון נמוך יותר, נלך אחורה
            while (current != null && current.plane.calculatePriorityScore() < newScore) {
                current = current.prev;
            }
            insertNodeAfter(current, newNode);
        }
        size++;
    }

    // --- פונקציות עזר להכנסת החוליה למקום הנכון מבלי לשבור את השרשרת ---

    private void insertNodeBefore(Node current, Node newNode) {
        if (current == null) { // הגענו לסוף הרשימה
            tail.next = newNode;
            newNode.prev = tail;
            tail = newNode;
        } else if (current == head) { // נכנסים לראש הרשימה
            newNode.next = head;
            head.prev = newNode;
            head = newNode;
        } else { // נכנסים באמצע הרשימה
            newNode.prev = current.prev;
            newNode.next = current;
            current.prev.next = newNode;
            current.prev = newNode;
        }
    }

    private void insertNodeAfter(Node current, Node newNode) {
        if (current == null) { // הגענו לראש הרשימה (עברנו את כולם אחורה)
            newNode.next = head;
            head.prev = newNode;
            head = newNode;
        } else if (current == tail) { // נכנסים בסוף הרשימה
            tail.next = newNode;
            newNode.prev = tail;
            tail = newNode;
        } else { // נכנסים באמצע הרשימה
            newNode.next = current.next;
            newNode.prev = current;
            current.next.prev = newNode;
            current.next = newNode;
        }
    }

    // --- שליפת המטוס עם העדיפות הכי גבוהה (עבור המגדל שינחית אותו) ---
    public Plane pollHighestPriority() {
        if (head == null) return null;
        Plane plane = head.plane;
        head = head.next;
        if (head != null) {
            head.prev = null;
        } else {
            tail = null; // הרשימה התרוקנה
        }
        size--;
        return plane;
    }

    public int getSize() {
        return size;
    }

    // פונקציה שממירה את התור לרשימה רגילה (לצורך שליחה ל-React בהמשך אם נרצה להציג את התור במסך)
    public List<Plane> toList() {
        List<Plane> list = new ArrayList<>();
        Node current = head;
        while (current != null) {
            list.add(current.plane);
            current = current.next;
        }
        return list;
    }
}
