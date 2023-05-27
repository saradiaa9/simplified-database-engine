package database;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

public class Octree implements Serializable {
	final int MAX_CAPACITY;
	int level = 0;
	List<Node> nodes;
	List<List<Node>> nodeDup;
	Octree zero = null;
	Octree one = null;
	Octree two = null;
	Octree three = null;
	Octree four = null;
	Octree five = null;
	Octree six = null;
	Octree seven = null;
	List<Octree> children;

	Boundary boundary;

	Octree(int level, Boundary boundary) throws IOException {

		this.level = level;
		nodes = new ArrayList<Node>();
		this.boundary = boundary;
		this.nodeDup = new ArrayList<List<Node>>();
		this.children = new ArrayList<Octree>();
		String configFilePath = "src/resources/DBApp.config";
		FileInputStream propsInput = new FileInputStream(configFilePath);
		Properties prop = new Properties();
		prop.load(propsInput);
//		MAX_CAPACITY = Integer.parseInt(prop.getProperty("MaximumEntriesinOctreeNode"));
		MAX_CAPACITY = 2;
	}

	/* Traveling the Graph using Depth First Search */
	public static void dfs(Octree tree) {
		if (tree == null)
			return;

		System.out.println("Level =" + tree.level + "\n[X1=" + tree.boundary.getxMin() + " Y1="
				+ tree.boundary.getyMin() + " Z1= " + tree.boundary.getzMin() + "]" + "\n" + "[X2="
				+ tree.boundary.getxMax() + " Y2=" + tree.boundary.getyMax() + " Z2=" + tree.boundary.getzMax() + "]");

		for (Node node : tree.nodes) {
			System.out.println(
					"x=" + node.x + " y=" + node.y + " z=" + node.z + " ref = page : " + node.reference + "\n");
		}
		if (tree.nodes.size() == 0) {
			System.out.println(" \n\t  Leaf Node.");
		}
		dfs(tree.zero);
		dfs(tree.one);
		dfs(tree.two);
		dfs(tree.three);
		dfs(tree.four);
		dfs(tree.five);
		dfs(tree.six);
		dfs(tree.seven);

	}

	public Object splitHelperX(Object o) {
		switch (o.getClass().getTypeName()) {

		case "java.lang.String":
			String OffsetS = printMiddleString((String) this.boundary.getxMax(), (String) this.boundary.getxMin());

			return (Object) OffsetS;

		case "java.lang.Integer":
			int OffsetI = (int) this.boundary.getxMin()
					+ ((int) this.boundary.getxMax() - (int) this.boundary.getxMin()) / 2;
			return (Object) OffsetI;

		case "java.lang.Double":
			Double OffsetDo = (Double) this.boundary.getxMin()
					+ ((Double) this.boundary.getxMax() - (Double) this.boundary.getxMin()) / 2;
			return (Object) OffsetDo;

		case "java.util.Date":
			Long maxD = ((Date) this.boundary.getxMax()).getTime();
			Long minD = ((Date) this.boundary.getxMin()).getTime();
			Long a = minD + (maxD - minD) / 2;
			Date OffsetD = new Date();
			OffsetD.setTime(a);
			return (Object) OffsetD;

		}
		return null;
	}

	public static ArrayList<Integer> searchHelper(Octree m, Object xF, String xOp, Object yF, String yOp, Object zF,
			String zOp, ArrayList<Integer> pagenums) {

		if (m.children.isEmpty()) {
			for (int i = 0; i < m.nodes.size(); i++) {
				Node n = m.nodes.get(i);
				if (checkSatifsy(n.x, xF, xOp) && checkSatifsy(n.y, yF, yOp) && checkSatifsy(n.z, zF, zOp)) {
					pagenums.add(n.reference);
					if(!m.nodeDup.get(i).isEmpty()) {
						for ( int z =0;z<m.nodeDup.get(i).size();z++) {
							pagenums.add(m.nodeDup.get(i).get(z).reference);
						}
					}
				}

			}

			return pagenums;

		} else {
			for (int i = 0; i < m.nodes.size(); i++) {
				Node n = m.nodes.get(i);
				if (checkSatifsy(n.x, xF, xOp) && checkSatifsy(n.y, yF, yOp) && checkSatifsy(n.z, zF, zOp)) {
					pagenums.add(n.reference);
				}

			}
			for (int j = 0; j < m.children.size(); j++) {
				// String [] oprStrings= {xOp,yOp,zOp};
				// Object [] compObjects= {xF,yF,zF};
				Octree current = m.children.get(j);
				int count = 0;
				if (current != null) {
					if (xOp.equals("=")) {
						if (current.boundary.inRangeColumn(xF, current.boundary.xMin, current.boundary.xMax)) {
							count++;
						}
					} else if (xOp.equals("<")) {
						if (compareToC(xF, current.boundary.xMin) >= 0) {
							count++;
						}
					} else if (xOp.equals(">")) {
						if (compareToC(xF, current.boundary.xMax) <= 0) {
							count++;
						}
					} else if (xOp.equals(">=")) {
						if (compareToC(xF, current.boundary.xMax) < 0) {
							count++;
						}
					} else if (xOp.equals("<=")) {
						if (compareToC(xF, current.boundary.xMin) > 0) {
							count++;
						}
					} else {
						count++;
					}
					if (yOp.equals("=")) {
						if (current.boundary.inRangeColumn(yF, current.boundary.yMin, current.boundary.yMax)) {
							count++;
						}
					} else if (yOp.equals("<")) {
						if (compareToC(yF, current.boundary.yMin) >= 0) {
							count++;
						}
					} else if (yOp.equals(">")) {
						if (compareToC(yF, current.boundary.yMax) <= 0) {
							count++;
						}
					} else if (yOp.equals(">=")) {
						if (compareToC(yF, current.boundary.yMax) < 0) {
							count++;
						}
					} else if (yOp.equals("<=")) {
						if (compareToC(yF, current.boundary.yMin) > 0) {
							count++;
						}
					} else {
						count++;
					}
					if (zOp.equals("=")) {
						if (current.boundary.inRangeColumn(zF, current.boundary.zMin, current.boundary.zMax)) {
							count++;
						}
					} else if (zOp.equals("<")) {
						if (compareToC(zF, current.boundary.zMin) >= 0) {
							count++;
						}
					} else if (zOp.equals(">")) {
						if (compareToC(zF, current.boundary.zMax) <= 0) {
							count++;
						}
					} else if (zOp.equals(">=")) {
						if (compareToC(zF, current.boundary.zMax) < 0) {
							count++;
						}
					} else if (zOp.equals("<=")) {
						if (compareToC(zF, current.boundary.zMin) > 0) {
							count++;
						}
					} else {
						count++;
					}
					if (count == 3) {
						
						searchHelper(current, xF, xOp, yF, yOp, zF, zOp,pagenums);
					}
				}
			}
		}

		return pagenums;
	}

	public static boolean checkSatifsy(Object inNode, Object toCompare, String operator) {
		switch (operator) {
		case "=":
			return (compareToC(inNode, toCompare) == 0);
		case "!=":
			return (compareToC(inNode, toCompare) != 0);
		case "<":
			return (compareToC(inNode, toCompare) < 0);
		case ">":
			return (compareToC(inNode, toCompare) > 0);
		case ">=":
			return (compareToC(inNode, toCompare) >= 0);
		case "<=":
			return (compareToC(inNode, toCompare) <= 0);
		default:
			return false;

		}
	}

	public static int compareToC(Object a, Object o) {
		if (a instanceof String) {
			return ((String) a).compareTo((String) o);
		}
		if (a instanceof Integer) {
			return ((Integer) a).compareTo((Integer) o);
		}
		if (a instanceof Double) {
			return ((Double) a).compareTo((Double) o);
		}
		if (a instanceof Date) {
			return ((Date) a).compareTo((Date) o);
		}
		return 0;
	}

	public Object splitHelperY(Object o) {
		switch (o.getClass().getTypeName()) {

		case "java.lang.String":
			String OffsetS = printMiddleString((String) this.boundary.getyMax(), (String) this.boundary.getyMin());

			return (Object) OffsetS;

		case "java.lang.Integer":
			int OffsetI = (int) this.boundary.getyMin()
					+ ((int) this.boundary.getyMax() - (int) this.boundary.getyMin()) / 2;
			return (Object) OffsetI;

		case "java.lang.Double":
			Double OffsetDo = (Double) this.boundary.getyMin()
					+ ((Double) this.boundary.getyMax() - (Double) this.boundary.getyMin()) / 2;
			return (Object) OffsetDo;

		case "java.util.Date":
			Long maxD = ((Date) this.boundary.getyMax()).getTime();
			Long minD = ((Date) this.boundary.getyMin()).getTime();
			Long a = minD + (maxD - minD) / 2;
			Date OffsetD = new Date();
			OffsetD.setTime(a);
			return (Object) OffsetD;

		}
		return null;
	}

	public Object splitHelperZ(Object o) {
		switch (o.getClass().getTypeName()) {

		case "java.lang.String":
			String OffsetS = printMiddleString((String) this.boundary.getzMax(), (String) this.boundary.getzMin());

			return (Object) OffsetS;

		case "java.lang.Integer":
			int OffsetI = (int) this.boundary.getzMin()
					+ ((int) this.boundary.getzMax() - (int) this.boundary.getzMin()) / 2;
			return (Object) OffsetI;

		case "java.lang.Double":
			Double OffsetDo = (Double) this.boundary.getzMin()
					+ ((Double) this.boundary.getzMax() - (Double) this.boundary.getzMin()) / 2;
			return (Object) OffsetDo;

		case "java.util.Date":
			Long maxD = ((Date) this.boundary.getzMax()).getTime();
			Long minD = ((Date) this.boundary.getzMin()).getTime();
			Long a = minD + (maxD - minD) / 2;
			Date OffsetD = new Date();
			OffsetD.setTime(a);
			return (Object) OffsetD;

		}
		return null;
	}

	void split() throws IOException {
		Object xOffset = splitHelperX(this.boundary.getxMin());
		Object yOffset = splitHelperY(this.boundary.getyMin());
		Object zOffset = splitHelperZ(this.boundary.getzMin());

		zero = new Octree(this.level + 1, new Boundary(this.boundary.getxMin(), this.boundary.getyMin(),
				this.boundary.getzMin(), xOffset, yOffset, zOffset));
		one = new Octree(this.level + 1, new Boundary(this.boundary.getxMin(), this.boundary.getyMin(), zOffset,
				xOffset, yOffset, this.boundary.getzMax()));
		two = new Octree(this.level + 1, new Boundary(xOffset, this.boundary.getyMin(), this.boundary.getzMin(),
				this.boundary.getxMax(), yOffset, zOffset));
		three = new Octree(this.level + 1, new Boundary(xOffset, this.boundary.getyMin(), zOffset,
				this.boundary.getxMax(), yOffset, this.boundary.getzMax()));
		four = new Octree(this.level + 1, new Boundary(this.boundary.getxMin(), yOffset, this.boundary.getzMin(),
				xOffset, this.boundary.getyMax(), zOffset));
		five = new Octree(this.level + 1, new Boundary(this.boundary.getxMin(), yOffset, zOffset, xOffset,
				this.boundary.getyMax(), this.boundary.getzMax()));
		six = new Octree(this.level + 1, new Boundary(xOffset, yOffset, this.boundary.getzMin(),
				this.boundary.getxMax(), this.boundary.getyMax(), zOffset));
		seven = new Octree(this.level + 1, new Boundary(xOffset, yOffset, zOffset, this.boundary.getxMax(),
				this.boundary.getyMax(), this.boundary.getzMax()));
		this.children = Arrays.asList(zero, one, two, three, four, five, six, seven);
		
	}

	public void insert(Object x, Object y, Object z, int value) throws IOException { // handle duplicates

		if (!this.boundary.inRange(x, y, z)) {
			return;
		}

		Node node = new Node(x, y, z, value);
		if (nodes.size() < MAX_CAPACITY) {
			for (int i = 0; i < nodes.size(); i++) {
				if (this.nodes.get(i).x.equals(x) && this.nodes.get(i).y.equals(y) && this.nodes.get(i).z.equals(z)) {
					nodeDup.get(i).add(node);
					return;
				}
			}

			nodes.add(node);
			nodeDup.add(new ArrayList<Node>());

			return;
		} else {
			if (nodes.size() == MAX_CAPACITY) {
				for (int i = 0; i < nodes.size(); i++) {

					if (this.nodes.get(i).x.equals(x) && this.nodes.get(i).y.equals(y)
							&& this.nodes.get(i).z.equals(z)) {

						nodeDup.get(i).add(node);

						return;
					}
				}
			}
			// Exceeded the capacity so split it in FOUR
			if (zero == null) {
				split();

			}

			// Check coordinates belongs to which partition
			if (this.zero.boundary.inRange(x, y, z))
				this.zero.insert(x, y, z, value);
			else if (this.one.boundary.inRange(x, y, z))
				this.one.insert(x, y, z, value);
			else if (this.two.boundary.inRange(x, y, z))
				this.two.insert(x, y, z, value);
			else if (this.three.boundary.inRange(x, y, z))
				this.three.insert(x, y, z, value);
			else if (this.four.boundary.inRange(x, y, z))
				this.four.insert(x, y, z, value);
			else if (this.five.boundary.inRange(x, y, z))
				this.five.insert(x, y, z, value);
			else if (this.six.boundary.inRange(x, y, z))
				this.six.insert(x, y, z, value);
			else if (this.seven.boundary.inRange(x, y, z))
				this.seven.insert(x, y, z, value);
			else
				System.out.println("ERROR : Unhandled partition " + x + " " + y + " " + z);
			
		}
	}

//handle dups
	public static ArrayList<Integer> search(Octree m, Object xF, String xOp, Object yF, String yOp, Object zF,
			String zOp) {
		ArrayList<Integer> pagenums = new ArrayList<Integer>();
		pagenums = searchHelper(m, xF, xOp, yF, yOp, zF, zOp, pagenums);
	
		return pagenums;
	}

	static String printMiddleString(String S, String T) {
		int N;
		S = S.toLowerCase();
		T = T.toLowerCase();
		if (S.length() > T.length()) {
			N = T.length();
			for (int i = N; i < S.length(); i++) {
				T += S.indexOf(i);
			}
		} else {
			N = S.length();
			for (int i = N; i < T.length(); i++) {
				S += T.indexOf(i);
			}
		}

		// Stores the base 26 digits after addition
		int[] a1 = new int[N + 1];

		for (int i = 0; i < N; i++) {
			a1[i + 1] = (int) S.charAt(i) - 97 + (int) T.charAt(i) - 97;
		}

		// Iterate from right to left
		// and add carry to next position
		for (int i = N; i >= 1; i--) {
			a1[i - 1] += (int) a1[i] / 26;
			a1[i] %= 26;
		}

		// Reduce the number to find the middle
		// string by dividing each position by 2
		for (int i = 0; i <= N; i++) {

			// If current value is odd,
			// carry 26 to the next index value
			if ((a1[i] & 1) != 0) {

				if (i + 1 <= N) {
					a1[i + 1] += 26;
				}
			}

			a1[i] = (int) a1[i] / 2;
		}
		String r = "";
		for (int i = 1; i <= N; i++) {
			r += (char) (a1[i] + 97);
		}
		return r;
	}

	public void updateRef(int newPage, Object x, Object y, Object z) {
		for (int i = 0; i < this.nodes.size(); i++) {
			if (this.nodes.get(i).x.equals(x) && this.nodes.get(i).y.equals(y) && this.nodes.get(i).z.equals(z)) {
				this.nodes.get(i).reference = newPage;
			}
		}
	}

	public static void deleteNode(Octree m, Object xF, Object yF, Object zF) {
		if (m.boundary.inRange(xF, yF, zF)) {
			for (int i = 0; i < m.nodes.size(); i++) {
				Node ss = m.nodes.get(i);

				if (ss.x.equals(xF) && ss.y.equals(yF) && ss.z.equals(zF)) {
					m.nodeDup.get(i).clear();
					m.nodes.remove(ss);

				}
			}

			if (m.children.isEmpty()) {
				return;

			} else if (m.zero != null && m.zero.boundary.inRange(xF, yF, zF)) {

				Node ss = null;
				for (int i = 0; i < m.zero.nodes.size(); i++) {
					ss = m.zero.nodes.get(i);

					if (ss.x.equals(xF) && ss.y.equals(yF) && ss.z.equals(zF)) {
						m.nodeDup.get(i).clear();
						m.nodes.remove(ss);
					}
				}

				deleteNode(m.zero, xF, yF, zF);

			} else if (m.one != null && m.one.boundary.inRange(xF, yF, zF)) {
				Node ss = null;
				for (int i = 0; i < m.one.nodes.size(); i++) {
					ss = m.one.nodes.get(i);

					if (ss.x.equals(xF) && ss.y.equals(yF) && ss.z.equals(zF)) {
						m.nodeDup.get(i).clear();
						m.nodes.remove(ss);
					}
				}

				deleteNode(m.one, xF, yF, zF);

			} else if (m.two != null && m.two.boundary.inRange(xF, yF, zF)) {
				Node ss = null;
				for (int i = 0; i < m.two.nodes.size(); i++) {
					ss = m.two.nodes.get(i);

					if (ss.x.equals(xF) && ss.y.equals(yF) && ss.z.equals(zF)) {
						m.nodeDup.get(i).clear();
						m.nodes.remove(ss);
					}
				}

				deleteNode(m.two, xF, yF, zF);
			} else if (m.three != null && m.three.boundary.inRange(xF, yF, zF)) {
				Node ss = null;
				for (int i = 0; i < m.three.nodes.size(); i++) {
					ss = m.three.nodes.get(i);

					if (ss.x.equals(xF) && ss.y.equals(yF) && ss.z.equals(zF)) {
						m.nodeDup.get(i).clear();
						m.nodes.remove(ss);
					}
				}

				deleteNode(m.three, xF, yF, zF);
			} else if (m.four != null && m.four.boundary.inRange(xF, yF, zF)) {

				Node ss = null;
				for (int i = 0; i < m.four.nodes.size(); i++) {
					ss = m.four.nodes.get(i);

					if (ss.x.equals(xF) && ss.y.equals(yF) && ss.z.equals(zF)) {
						m.nodeDup.get(i).clear();
						m.nodes.remove(ss);
					}
				}

				deleteNode(m.four, xF, yF, zF);
			} else if (m.five != null && m.five.boundary.inRange(xF, yF, zF)) {

				Node ss = null;
				for (int i = 0; i < m.five.nodes.size(); i++) {
					ss = m.five.nodes.get(i);

					if (ss.x.equals(xF) && ss.y.equals(yF) && ss.z.equals(zF)) {
						m.nodeDup.get(i).clear();
						m.nodes.remove(ss);
					}
				}

				deleteNode(m.five, xF, yF, zF);
			} else if (m.six != null && m.six.boundary.inRange(xF, yF, zF)) {
				Node ss = null;
				for (int i = 0; i < m.six.nodes.size(); i++) {
					ss = m.six.nodes.get(i);

					if (ss.x.equals(xF) && ss.y.equals(yF) && ss.z.equals(zF)) {
						m.nodeDup.get(i).clear();
						m.nodes.remove(ss);
					}
				}

				deleteNode(m.six, xF, yF, zF);
			} else if (m.seven != null && m.seven.boundary.inRange(xF, yF, zF)) {
				Node ss = null;
				for (int i = 0; i < m.seven.nodes.size(); i++) {
					ss = m.seven.nodes.get(i);

					if (ss.x.equals(xF) && ss.y.equals(yF) && ss.z.equals(zF)) {
						m.nodeDup.get(i).clear();
						m.nodes.remove(ss);
					}
				}

				deleteNode(m.seven, xF, yF, zF);
			} else {
				return;
			}
		} else {
			return;
		}

	}

	public static void main(String args[]) throws IOException {
		Octree anySpace = new Octree(1, new Boundary(0.0, "a", 0.0, 1000.0, "z", 1000.0));

		anySpace.insert(0.3, "b", 1.1, 0);

		anySpace.insert(0.2, "s", 3.2, 1);

		anySpace.insert(0.4, "z", 3.0, 2);
		
		anySpace.insert(0.4, "z", 3.0, 4);
		System.out.println(anySpace.children.isEmpty());
		anySpace.dfs(anySpace);
		System.out.println(anySpace.search(anySpace, new Double(0.4), "=", "z","<=", new Double(3.0), "="));

//		anySpace.insert(600, 600,600, 1);
//		anySpace.insert(700, 600, 500,1);
//		
//		anySpace.insert(800, 600,500, 1);
//		anySpace.insert(900, 650,500, 1);
//		anySpace.insert(510, 610,710 ,1);
//		anySpace.insert(520, 620,720, 1);
//		anySpace.insert(530, 630,430, 1);
//		anySpace.insert(540, 640,340, 1);
//		anySpace.insert(550, 650,750, 1);
//		anySpace.insert(555, 655,755, 1);
//		anySpace.insert(560, 660,760, 1);
		// Traveling the graph

//		System.out.println(anySpace.nodes.get(0).reference);
//		deleteNode(anySpace, 0.3, "b", 1.1);
//		System.out.println("///////////////////////////////////////");
//		anySpace.dfs(anySpace);

	}
}