package database;

import java.io.Serializable;

public class Node implements Serializable{
	Object x, y, z  ;
	int reference;

	public Node(Object x, Object y, Object z, int  page) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.reference = page; 
	}

}
