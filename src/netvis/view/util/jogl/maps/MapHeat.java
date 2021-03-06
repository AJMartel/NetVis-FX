package netvis.view.util.jogl.maps;

import netvis.model.Packet;
import netvis.view.util.jogl.comets.FlipNode;
import netvis.view.util.jogl.comets.GraphNode;
import netvis.view.util.jogl.comets.HeatNode;
import netvis.view.util.jogl.gameengine.Node;
import netvis.view.util.jogl.gameengine.Painter;
import netvis.view.util.jogl.gameengine.Position;

import javax.media.opengl.GL2;
import java.util.*;
import java.util.concurrent.*;

public class MapHeat extends Map {
    // IPs mapped to nodesByName
    private java.util.Map<String, NodeWithPosition> nodesByName;
    private HashMap<Position, NodeWithPosition> nodesByPosition;
    private List<NodeWithPosition> nodesl;
    private MapPainter painter;
    private Random rand;
    private int gridsize;

//    class NamedThreadFactory implements ThreadFactory {
//        int i = 0;
//
//        public Thread newThread(Runnable r) {
//            return new Thread(r, "Node animating thread #" + (i++));
//        }
//    }
//
//    // Animation of the nodesByName can be parallelised
//    ExecutorService exe = new ThreadPoolExecutor(4, 8, 5000, TimeUnit.MILLISECONDS,
//            new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory());

    public MapHeat(int w, int h) {
        width = w;
        height = h;

        rand = new Random();

        nodesByName = Collections.synchronizedMap(new HashMap<>());
        nodesByPosition = new HashMap<>();
        nodesl = new ArrayList<>();

        painter = new MapPainter();

        gridsize = 160;
        Painter.generateGrid("secondgrid", gridsize);
    }

    public void drawEverything(GL2 gl) {
        synchronized (nodesByName) {
            Painter.drawGrid(base, gridsize, "secondgrid", gl);
            for (NodeWithPosition i : nodesByName.values()) {
                int x = i.pos.x;
                int y = i.pos.y;

                gl.glPushMatrix();
                // Transpose it to the right spot
                gl.glTranslated(x, y, 0.0);

                // Draw it
                i.node.draw(base, painter, gl);
                gl.glPopMatrix();
            }
        }
    }

    public void stepAnimation(final long time) throws InterruptedException, ExecutionException {
        synchronized (nodesByName) {
            for (final NodeWithPosition i : nodesByName.values()) {
                i.node.updateAnimation(time);
            }
        }

//        ArrayList<Future<?>> list = new ArrayList<Future<?>>();
//        for (final NodeWithPosition i : nodesByName.values()) {
//            list.add(exe.submit(new Callable() {
//                @Override
//                public Object call() throws Exception {
//                    i.node.updateAnimation(time);
//                    return null;
//                }
//            }));
//        }
//
//        for (Future<?> i : list) {
//            i.get();
//        }
    }

    public void setSize(int w, int h, GL2 gl) {
        width = w;
        height = h;
    }

    public void suggestNode (String sip, String dip, List<Packet> packets) {
        // Suggests the existence of the node in the network to be displayed

        // Look whether the node already exists
        NodeWithPosition find = nodesByName.get(dip);

        if (find == null) {
            find = addNode(sip, dip, "basic");
        }

        for (Packet pp : packets) {
            find.node.updateWithData(pp);
        }
    }

    public void sortNodes() {
		/*
		 * TODO This code is unreachable -- Is it still here so it can be used
		 * later? Yes, it most certainly is
		 *
		 * Collections.sort(nodesl, new Comparator<NodeWithPosition>() {
		 *
		 * @Override public int compare(NodeWithPosition n1, NodeWithPosition
		 * n2) { return n2.node.Priority() - n1.node.Priority(); }
		 *
		 * });
		 *
		 * // Now display them in the order for (int i = 0; i < nodesl.size();
		 * i++) { NodeWithPosition n = nodesl.get(i);
		 *
		 * Position posit = FindPosition(i); Position coord =
		 * CoordinateByPosition(posit);
		 *
		 * n.pos = posit; n.coo = coord; }
		 */
    }

    private Position findPosition(int num) {
        double x = 0.0;
        double y = 0.0;

        int s = num + 1;
        int innerring = (int) Math.floor(Math.sqrt((s - 1 / 4.0) / 3.0) - 0.5);
        int outerring = innerring + 1;
        int k = innerring;
        int shift = s - 3 * (k * k + k) - 1;

        // Move to the desired ring
        if (outerring % 2 == 1)
            x += Math.sqrt(3) * base / 2.0;
        y += base * outerring * 1.5;

        // Move the shift times
        double angle = 0;

        for (int i = 0 - (outerring / 2); i < shift - (outerring / 2); i++) {
            if (i % outerring == 0) {
                angle -= Math.PI / 3;
            }
            x += Math.sqrt(3) * base * Math.cos(angle);
            y += Math.sqrt(3) * base * Math.sin(angle);
        }

        // System.out.println("Node #" + num + " is on the ring : " + outerring
        // + " with shift " + shift);

        return new Position(x, y);
    }

    private Position positionByCoordinate(Position coor) {
        int coorx = coor.x;
        int coory = coor.y;

        double r3 = Math.sqrt(3.0);
        Position p = new Position(r3 * coorx * base, 1.5 * coory * base);

        // Odd rows are shifted
        if (coory % 2 != 0) {
            p.x += r3 * base / 2.0;
        }

        return p;
    }

    private Position coordinateByPosition(Position pos) {
        int posx = pos.x;
        int posy = pos.y;

        double r3 = Math.sqrt(3.0);
        Position p = new Position(0, 0);

        p.y = (int) Math.round(posy / (base * 1.5));
        if (p.y % 2 != 0) {
            p.x = (int) Math.round((posx - base * r3 / 2.0) / (base * r3));
        } else {
            p.x = (int) Math.round(posx / (base * r3));
        }

        return p;
    }

    private NodeWithPosition addNode(String near, String name, String textureName) {
        HeatNode front = new HeatNode  (textureName, name);
        GraphNode back = new GraphNode (name);

        // Make it into the flip node - the node that has two sides
        FlipNode lemur = new FlipNode (front, back);

        Position posit;
        Position coord;
        NodeWithPosition nearnode = nodesByName.get(near);

        // Look for the position around the specified coordinate
        int current = 0;
        while (true) {
            posit = findPosition(current);
            coord = coordinateByPosition(posit);
            if (nearnode != null) {
                coord.x += nearnode.coo.x;
                coord.y += nearnode.coo.y;
            }
            posit = positionByCoordinate(coord);

            if (nodesByPosition.get(coord) != null) {
                current++;
            } else {
                break;
            }
        }

        coord = coordinateByPosition(posit);
        NodeWithPosition k = new NodeWithPosition(lemur, coord, posit);

        // System.out.println("Node " + name + " placed in coords : " + coord.x + ", " + coord.y);
        synchronized (nodesByName) {
            nodesByName.put(name, k);
        }
        nodesl.add(k);
        nodesByPosition.put(coord, k);

        return k;
    }

    public Node findClickedNode(int x, int y) {
        // Optimised version
        Position p = new Position(x, y);
        Position c = coordinateByPosition(p);

        NodeWithPosition node = nodesByPosition.get(c);
        return node.node;
    }

    public Position findClickedNodePos(int x, int y) {
        // Optimised version
        Position p = new Position(x, y);
        Position c = coordinateByPosition(p);

        NodeWithPosition node = nodesByPosition.get(c);
        return node.pos;
    }

    public class NodeWithPosition {
        public NodeWithPosition(Node a, Position c, Position p) {
            node = a;
            coo = c;
            pos = p;
        }

        public Node node;
        public Position pos;
        public Position coo;
    }
}
