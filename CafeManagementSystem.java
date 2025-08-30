
/*
CafeManagementSystem.java
------------------------
Full implementation aligned with the lab assignment "Build a Real-World Application with Custom Data Structures".

Features:
- Custom HashTable (menu) with chaining (O(1) avg)
- Custom AVL Tree (inventory) with insert/delete/search (O(log n))
- Linked-list Queue (normal orders) (O(1) enqueue/dequeue)
- Custom MinHeap (priority orders) (O(log n) insert/pop)
- Interval Tree (reservations) to detect overlaps (O(log n) avg)
- Integration layer with billing, inventory deduction, low-stock alerts, persistence (CSV), and demo/test cases.
- Each data structure and main algorithms include time & space complexity comments.
*/

import java.io.*;
import java.util.*;

// ---------------------------
// Menu: Custom Hash Table
// ---------------------------
class MenuItem {
    int id;
    String name;
    double price;
    MenuItem next; // chaining

    public MenuItem(int id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.next = null;
    }
}

class MenuHashTable {
    private MenuItem[] table;
    private int capacity;
    private int size;

    // Time Complexity: constructor O(capacity)
    public MenuHashTable(int capacity) {
        this.capacity = capacity;
        this.table = new MenuItem[capacity];
        this.size = 0;
    }

    private int hash(int key) {
        return Math.abs(Integer.hashCode(key)) % capacity;
    }

    // Average: O(1) insert, Worst: O(n) if many collisions
    public void insert(int id, String name, double price) {
        int idx = hash(id);
        MenuItem node = table[idx];
        while (node != null) {
            if (node.id == id) {
                node.name = name; node.price = price; return;
            }
            node = node.next;
        }
        MenuItem newItem = new MenuItem(id, name, price);
        newItem.next = table[idx];
        table[idx] = newItem;
        size++;
    }

    // Average: O(1) search
    public MenuItem search(int id) {
        int idx = hash(id);
        MenuItem node = table[idx];
        while (node != null) {
            if (node.id == id) return node;
            node = node.next;
        }
        return null;
    }

    // Average: O(1) delete
    public boolean delete(int id) {
        int idx = hash(id);
        MenuItem prev = null;
        MenuItem node = table[idx];
        while (node != null) {
            if (node.id == id) {
                if (prev == null) table[idx] = node.next;
                else prev.next = node.next;
                size--;
                return true;
            }
            prev = node; node = node.next;
        }
        return false;
    }

    // Return items sorted by id for stable display: O(n log n)
    public List<MenuItem> allItems() {
        List<MenuItem> res = new ArrayList<>();
        for (MenuItem head : table) {
            MenuItem cur = head;
            while (cur != null) {
                res.add(cur); cur = cur.next;
            }
        }
        res.sort(Comparator.comparingInt(mi -> mi.id));
        return res;
    }

    public int size() { return size; }

    // Persistence: save to CSV
    public void saveToCSV(String path) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            pw.println("id,name,price");
            for (MenuItem mi : allItems()) {
                pw.printf("%d,%s,%.2f%n", mi.id, mi.name.replace(",", " "), mi.price);
            }
        }
    }

    public void loadFromCSV(String path) throws IOException {
        File f = new File(path);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",",3);
                if (parts.length < 3) continue;
                int id = Integer.parseInt(parts[0].trim());
                String name = parts[1].trim();
                double price = Double.parseDouble(parts[2].trim());
                insert(id, name, price);
            }
        }
    }
}

// ---------------------------
// Inventory: AVL Tree
// ---------------------------
class InventoryItem {
    int id;
    String name;
    int stock;
    int reorderThreshold;

    public InventoryItem(int id, String name, int stock, int reorderThreshold) {
        this.id = id; this.name = name; this.stock = stock; this.reorderThreshold = reorderThreshold;
    }
}

class AVLNode {
    int key; // item id
    InventoryItem value;
    AVLNode left, right;
    int height;

    public AVLNode(int key, InventoryItem value) {
        this.key = key; this.value = value; this.left = this.right = null; this.height = 1;
    }
}

class AVLTree {
    private AVLNode root;

    // Utility methods
    private int height(AVLNode n) { return n == null ? 0 : n.height; }
    private int balanceFactor(AVLNode n) { return n == null ? 0 : height(n.left) - height(n.right); }
    private void updateHeight(AVLNode n) { if (n!=null) n.height = 1 + Math.max(height(n.left), height(n.right)); }

    private AVLNode rotateRight(AVLNode y) {
        AVLNode x = y.left;
        AVLNode T2 = x.right;
        x.right = y;
        y.left = T2;
        updateHeight(y); updateHeight(x);
        return x;
    }

    private AVLNode rotateLeft(AVLNode x) {
        AVLNode y = x.right;
        AVLNode T2 = y.left;
        y.left = x;
        x.right = T2;
        updateHeight(x); updateHeight(y);
        return y;
    }

    private AVLNode rebalance(AVLNode node) {
        updateHeight(node);
        int bf = balanceFactor(node);
        if (bf > 1) {
            if (balanceFactor(node.left) < 0) node.left = rotateLeft(node.left);
            return rotateRight(node);
        }
        if (bf < -1) {
            if (balanceFactor(node.right) > 0) node.right = rotateRight(node.right);
            return rotateLeft(node);
        }
        return node;
    }

    // Insert or update: O(log n)
    private AVLNode insertRec(AVLNode node, int key, InventoryItem value) {
        if (node == null) return new AVLNode(key, value);
        if (key < node.key) node.left = insertRec(node.left, key, value);
        else if (key > node.key) node.right = insertRec(node.right, key, value);
        else {
            node.value = value; // update
            return node;
        }
        return rebalance(node);
    }

    public void insert(int key, InventoryItem value) {
        root = insertRec(root, key, value);
    }

    // Search: O(log n)
    private InventoryItem searchRec(AVLNode node, int key) {
        if (node == null) return null;
        if (key == node.key) return node.value;
        if (key < node.key) return searchRec(node.left, key);
        else return searchRec(node.right, key);
    }

    public InventoryItem search(int key) { return searchRec(root, key); }

    // Delete: O(log n)
    private AVLNode minValueNode(AVLNode node) {
        AVLNode current = node;
        while (current.left != null) current = current.left;
        return current;
    }

    private AVLNode deleteRec(AVLNode node, int key) {
        if (node == null) return null;
        if (key < node.key) node.left = deleteRec(node.left, key);
        else if (key > node.key) node.right = deleteRec(node.right, key);
        else {
            if (node.left == null) return node.right;
            else if (node.right == null) return node.left;
            AVLNode temp = minValueNode(node.right);
            node.key = temp.key; node.value = temp.value;
            node.right = deleteRec(node.right, temp.key);
        }
        return rebalance(node);
    }

    public void delete(int key) { root = deleteRec(root, key); }

    // Inorder traversal: O(n)
    public List<InventoryItem> inorder() {
        List<InventoryItem> res = new ArrayList<>();
        inorderRec(root, res);
        return res;
    }

    private void inorderRec(AVLNode node, List<InventoryItem> res) {
        if (node == null) return;
        inorderRec(node.left, res);
        res.add(node.value);
        inorderRec(node.right, res);
    }

    // Low stock alert: O(n) to check all nodes
    public List<InventoryItem> lowStockItems() {
        List<InventoryItem> list = inorder();
        List<InventoryItem> low = new ArrayList<>();
        for (InventoryItem it : list) {
            if (it.stock <= it.reorderThreshold) low.add(it);
        }
        return low;
    }

    // Persistence CSV
    public void saveToCSV(String path) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            pw.println("id,name,stock,threshold");
            for (InventoryItem it : inorder()) {
                pw.printf("%d,%s,%d,%d%n", it.id, it.name.replace(",", " "), it.stock, it.reorderThreshold);
            }
        }
    }

    public void loadFromCSV(String path) throws IOException {
        File f = new File(path);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",",4);
                if (p.length < 4) continue;
                int id = Integer.parseInt(p[0].trim());
                String name = p[1].trim();
                int stock = Integer.parseInt(p[2].trim());
                int thr = Integer.parseInt(p[3].trim());
                insert(id, new InventoryItem(id, name, stock, thr));
            }
        }
    }
}

// ---------------------------
// Orders: Linked List Queue + MinHeap for priority
// ---------------------------
class Order {
    int orderId;
    String customer;
    List<int[]> items; // pair [itemId, qty]
    boolean isPriority;
    long timestamp;

    public Order(int orderId, String customer, boolean isPriority) {
        this.orderId = orderId; this.customer = customer; this.isPriority = isPriority;
        this.items = new ArrayList<>(); this.timestamp = System.currentTimeMillis();
    }
    public void addItem(int itemId, int qty) { items.add(new int[]{itemId, qty}); }
}

// Linked list queue (normal orders) - O(1) enqueue/dequeue
class OrderQueue {
    private static class Node {
        Order order; Node next;
        Node(Order order) { this.order = order; this.next = null; }
    }
    private Node head, tail;
    private int size = 0;
    public void enqueue(Order order) {
        Node n = new Node(order);
        if (tail == null) { head = tail = n; }
        else { tail.next = n; tail = n; }
        size++;
    }
    public Order dequeue() {
        if (head == null) return null;
        Order o = head.order; head = head.next;
        if (head == null) tail = null;
        size--; return o;
    }
    public boolean isEmpty() { return head == null; }
    public int size() { return size; }
}

// Min-heap priority queue (custom) - supports priority integer where lower = higher priority
class PriorityOrderHeap {
    private static class HeapNode {
        Order order; int priority; long seq;
        HeapNode(Order order, int priority, long seq) { this.order = order; this.priority = priority; this.seq = seq; }
    }
    private List<HeapNode> heap = new ArrayList<>();
    private long seqCounter = 0;

    private int parent(int i) { return (i-1)/2; }
    private int left(int i) { return 2*i+1; }
    private int right(int i) { return 2*i+2; }

    private void swap(int i, int j) { HeapNode t = heap.get(i); heap.set(i, heap.get(j)); heap.set(j, t); }

    // Comparison: by priority, then by insertion order (seq)
    private boolean less(int i, int j) {
        HeapNode a = heap.get(i), b = heap.get(j);
        if (a.priority != b.priority) return a.priority < b.priority;
        return a.seq < b.seq;
    }

    public void push(Order order, int priority) {
        heap.add(new HeapNode(order, priority, seqCounter++));
        int idx = heap.size()-1;
        while (idx > 0 && less(idx, parent(idx))) { swap(idx, parent(idx)); idx = parent(idx); }
    }

    public Order pop() {
        if (heap.isEmpty()) return null;
        Order result = heap.get(0).order;
        HeapNode last = heap.remove(heap.size()-1);
        if (!heap.isEmpty()) {
            heap.set(0, last);
            heapify(0);
        }
        return result;
    }

    private void heapify(int i) {
        int l = left(i), r = right(i), smallest = i;
        if (l < heap.size() && less(l, smallest)) smallest = l;
        if (r < heap.size() && less(r, smallest)) smallest = r;
        if (smallest != i) { swap(i, smallest); heapify(smallest); }
    }

    public boolean isEmpty() { return heap.isEmpty(); }
    public int size() { return heap.size(); }
}

// ---------------------------
// Reservations: Interval Tree (per table)
// ---------------------------
class Reservation {
    int tableId;
    int start, end; // minutes or time units
    String customer;
    public Reservation(int tableId, int start, int end, String customer) {
        this.tableId = tableId; this.start = start; this.end = end; this.customer = customer;
    }
}

class IntervalNode {
    int start, end;
    int maxEnd;
    Reservation reservation;
    IntervalNode left, right;
    public IntervalNode(Reservation res) {
        this.start = res.start; this.end = res.end; this.maxEnd = res.end; this.reservation = res;
    }
}

class IntervalTree {
    IntervalNode root = null;

    // insert: O(log n) average; update maxEnd
    private IntervalNode insertRec(IntervalNode node, Reservation res) {
        if (node == null) return new IntervalNode(res);
        if (res.start < node.start) node.left = insertRec(node.left, res);
        else node.right = insertRec(node.right, res);
        node.maxEnd = Math.max(node.maxEnd, res.end);
        return node;
    }

    // overlap search: O(k + log n) where k = number of overlapping intervals found
    private boolean overlaps(IntervalNode node, int s, int e) {
        if (node == null) return false;
        if (s < node.end && e > node.start) return true;
        if (node.left != null && node.left.maxEnd > s) return overlaps(node.left, s, e);
        return overlaps(node.right, s, e);
    }

    public boolean book(Reservation res) {
        if (overlaps(root, res.start, res.end)) return false;
        root = insertRec(root, res); return true;
    }

    // gather overlaps for a query range
    public List<Reservation> findOverlaps(int s, int e) {
        List<Reservation> res = new ArrayList<>(); findOverlapsRec(root, s, e, res); return res;
    }
    private void findOverlapsRec(IntervalNode node, int s, int e, List<Reservation> out) {
        if (node == null) return;
        if (s < node.end && e > node.start) out.add(node.reservation);
        if (node.left != null && node.left.maxEnd > s) findOverlapsRec(node.left, s, e, out);
        if (node.right != null && node.start < e) findOverlapsRec(node.right, s, e, out);
    }
}

// ---------------------------
// Integration: CafeManagementSystem class
// ---------------------------
public class CafeManagementSystem {

    private MenuHashTable menu;
    private AVLTree inventory;
    private OrderQueue normalQueue;
    private PriorityOrderHeap priorityHeap;
    private Map<Integer, IntervalTree> tableReservations; // per table
    private final double TAX_RATE = 0.08;

    // Persistence file paths
    private final String MENU_CSV = "menu.csv";
    private final String INVENTORY_CSV = "inventory.csv";

    public CafeManagementSystem() {
        menu = new MenuHashTable(127);
        inventory = new AVLTree();
        normalQueue = new OrderQueue();
        priorityHeap = new PriorityOrderHeap();
        tableReservations = new HashMap<>();
    }

    // Load sample data (or from CSV)
    public void loadSampleData() {
        // menu IDs align with inventory IDs for simplicity
        menu.insert(1, "Espresso", 350.0);
        menu.insert(2, "Cappuccino", 450.0);
        menu.insert(3, "Latte", 500.0);
        menu.insert(4, "Blueberry Muffin", 300.0);
        menu.insert(5, "Chocolate Croissant", 320.0);

        inventory.insert(1, new InventoryItem(1, "Espresso Beans", 50, 10));
        inventory.insert(2, new InventoryItem(2, "Milk", 40, 8));
        inventory.insert(3, new InventoryItem(3, "Latte Mix", 30, 5));
        inventory.insert(4, new InventoryItem(4, "Muffins", 20, 5));
        inventory.insert(5, new InventoryItem(5, "Croissants", 15, 5));
    }

    public void saveData() {
        try { menu.saveToCSV(MENU_CSV); inventory.saveToCSV(INVENTORY_CSV); System.out.println("Data saved."); }
        catch (IOException e) { System.out.println("Save failed: " + e.getMessage()); }
    }

    public void loadData() {
        try { menu.loadFromCSV(MENU_CSV); inventory.loadFromCSV(INVENTORY_CSV); System.out.println("Data loaded."); }
        catch (IOException e) { System.out.println("Load failed: " + e.getMessage()); }
    }

    // Place order: validate menu + inventory then enqueue
    public void placeOrder(int orderId, String customer, List<int[]> items, Integer priority) {
        // Validate
        for (int[] it : items) {
            int itemId = it[0], qty = it[1];
            MenuItem mi = menu.search(itemId);
            InventoryItem inv = inventory.search(itemId);
            if (mi == null) throw new IllegalArgumentException("Menu item not found: " + itemId);
            if (inv == null || inv.stock < qty) throw new IllegalArgumentException("Insufficient stock for item: " + mi.name);
        }
        Order o = new Order(orderId, customer, priority != null);
        for (int[] it : items) o.addItem(it[0], it[1]);
        if (priority == null) normalQueue.enqueue(o);
        else priorityHeap.push(o, priority);
    }

    // Calculate bill: O(k) where k = number of items in order
    private double[] calculateBill(Order o) {
        double subtotal = 0.0;
        for (int[] it : o.items) {
            int itemId = it[0], qty = it[1];
            MenuItem mi = menu.search(itemId);
            subtotal += mi.price * qty;
        }
        double tax = subtotal * TAX_RATE;
        return new double[]{round2(subtotal), round2(subtotal + tax)};
    }

    // Deduct inventory after processing: O(k log n)
    private void deductInventory(Order o) {
        for (int[] it : o.items) {
            int itemId = it[0], qty = it[1];
            InventoryItem inv = inventory.search(itemId);
            inv.stock = Math.max(0, inv.stock - qty);
            inventory.insert(itemId, inv); // update
        }
    }

    // Process next order: priority first
    public Map<String,Object> processNextOrder() {
        Order o = null;
        if (!priorityHeap.isEmpty()) o = priorityHeap.pop();
        else if (!normalQueue.isEmpty()) o = normalQueue.dequeue();
        if (o == null) return null;
        double[] tot = calculateBill(o);
        deductInventory(o);
        Map<String,Object> bill = new HashMap<>();
        bill.put("orderId", o.orderId);
        bill.put("customer", o.customer);
        List<String> itemsDesc = new ArrayList<>();
        for (int[] it : o.items) {
            MenuItem m = menu.search(it[0]);
            itemsDesc.add(String.format("%s x%d", m.name, it[1]));
        }
        bill.put("items", itemsDesc);
        bill.put("subtotal", tot[0]);
        bill.put("total", tot[1]);
        return bill;
    }

    public static double round2(double v) { return Math.round(v*100.0)/100.0; }

    public boolean bookTable(int tableId, int start, int end, String customer) {
        tableReservations.putIfAbsent(tableId, new IntervalTree());
        IntervalTree t = tableReservations.get(tableId);
        return t.book(new Reservation(tableId, start, end, customer));
    }

    public List<InventoryItem> lowStockAlerts() { return inventory.lowStockItems(); }

    // Demo run
    public static void main(String[] args) {
        CafeManagementSystem cms = new CafeManagementSystem();
        cms.loadSampleData();

        System.out.println("=== MENU ===");
        for (MenuItem mi : cms.menu.allItems()) {
            System.out.printf("%02d | %-22s Rs %.2f%n", mi.id, mi.name, mi.price);
        }

        System.out.println("\n=== RESERVATIONS ===");
        System.out.println("Book T1 9:00-10:00 -> " + (cms.bookTable(1, 9*60, 10*60, "Ayesha") ? "OK" : "Conflict"));
        System.out.println("Book T1 9:50-10:30 -> " + (cms.bookTable(1, 9*60+50, 10*60+30, "Bilal") ? "OK" : "Conflict"));
        System.out.println("Book T1 10:30-11:30 -> " + (cms.bookTable(1, 10*60+30, 11*60+30, "Danish") ? "OK" : "Conflict"));

        System.out.println("\n=== PLACE ORDERS ===");
        cms.placeOrder(101, "Fatima", Arrays.asList(new int[]{1,2}, new int[]{4,1}), null);
        cms.placeOrder(102, "Hassan", Arrays.asList(new int[]{2,1}, new int[]{3,1}), 0); // VIP priority 0
        cms.placeOrder(103, "Iram", Arrays.asList(new int[]{5,2}), 1); // lower priority
        cms.placeOrder(104, "Javed", Arrays.asList(new int[]{1,1}, new int[]{2,1}), null);

        System.out.println("\n=== PROCESSING ORDERS (Priority first) ===");
        while (true) {
            Map<String,Object> bill = cms.processNextOrder();
            if (bill == null) break;
            System.out.println("Bill for Order #" + bill.get("orderId") + " (" + bill.get("customer") + ")");
            for (String s : (List<String>)bill.get("items")) System.out.println("  - " + s);
            System.out.printf("Subtotal: Rs %.2f%n", (double)bill.get("subtotal"));
            System.out.printf("TOTAL (incl. tax): Rs %.2f%n", (double)bill.get("total"));
            System.out.println("--------------------------------");
        }

        System.out.println("\n=== LOW STOCK ALERTS ===");
        for (InventoryItem it : cms.lowStockAlerts()) {
            System.out.println("[ALERT] " + it.name + " stock=" + it.stock + " threshold=" + it.reorderThreshold);
        }

        // Save data demo
        cms.saveData();
        System.out.println("\nDemo complete.");
    }
}
